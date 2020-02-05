/*
 * Module: r2-testapp-kotlin
 * Developers: Aferdita Muriqi, Mostapha Idoubihi, Paul Stoica
 *
 * Copyright (c) 2018. European Digital Reading Lab. All rights reserved.
 * Licensed to the Readium Foundation under one or more contributor license agreements.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.testapp.epub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_epub.*
import kotlinx.android.synthetic.main.filter_row.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.appcompat.v7.coroutines.onClose
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.navigator.epub.Style
import org.readium.r2.navigator.pager.R2EpubPageFragment
import org.readium.r2.navigator.pager.R2PagerAdapter
import org.readium.r2.shared.*
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.DRMManagementActivity
import org.readium.r2.testapp.R
import org.readium.r2.testapp.db.*
import org.readium.r2.testapp.epub.fragment.OverlayFragment
import org.readium.r2.testapp.library.activitiesLaunched
import org.readium.r2.testapp.outline.R2OutlineActivity
import org.readium.r2.testapp.search.MarkJSSearchEngine
import org.readium.r2.testapp.search.SearchLocator
import org.readium.r2.testapp.search.SearchLocatorAdapter
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

class EpubActivity : R2EpubActivity(), NavigatorDelegate {

    override fun locationDidChange(navigator: Navigator?, locator: Locator) {
       //save progression here
    }

    fun updateAppearance(value: Int) {
        var ref = userSettings.userProperties.getByRef<Enumerable>(APPEARANCE_REF)
        ref.index = value
        userSettings.updateEnumerable(ref)
        userSettings.updateViewCSS(APPEARANCE_REF)

        when (value) {
            0 -> {
                resourcePager.setBackgroundColor(Color.parseColor("#ffffff"))
            }
            1 -> {
                resourcePager.setBackgroundColor(Color.parseColor("#faf4e8"))
            }
            2 -> {
                resourcePager.setBackgroundColor(Color.parseColor("#000000"))
            }
        }
    }

    //UserSettings
    private lateinit var userSettings: UserSettings

    //Overlay fragment
    private lateinit var overlayFragment: OverlayFragment

    //Accessibility
    private var isExploreByTouchEnabled = false
    private var pageEnded = false

    private var menuDrm: MenuItem? = null

    override var allowToggleActionBar = true

    /**
     * Manage activity creation.
     *   - Load data from the database
     *   - Set background and text colors
     *   - Set onClickListener callbacks for the [screenReader] buttons
     *   - Initialize search.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        resourcePager.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        overlayFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_overlay) as OverlayFragment

        navigatorDelegate = this
        bookId = intent.getLongExtra("bookId", -1)

        Handler().postDelayed({
            launch {
                menuDrm?.isVisible = intent.getBooleanExtra("drm", false)
            }
        }, 100)

        resourcePager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {
                // Do nothing
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // Do nothing
            }

            override fun onPageSelected(position: Int) {
                val resource = publication.readingOrder[resourcePager.currentItem]
                //here chapter have changed
            }
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        overlayFragment.setTotalPages(resourcePager.adapter?.count ?: 0)
    }

    override fun highlightActivated(id: String) {
    }

    override fun highlightAnnotationMarkActivated(id: String) {
    }


    /**
     * Manage what happens when the focus is put back on the EpubActivity.
     *  - Synchronize the [R2ScreenReader] with the webView if the [R2ScreenReader] exists.
     *  - Create a [R2ScreenReader] instance if it was uninitialized.
     */
    override fun onResume() {
        super.onResume()

        /*
         * If TalkBack or any touch exploration service is activated
         * we force scroll mode (and override user preferences)
         */
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled


        if (publication.cssStyle != ContentLayoutStyle.cjkv.name) {
            publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
        }

        userSettings = UserSettings(preferences, this, publication.userSettingsUIPreset)
        userSettings.resourcePager = resourcePager

    }

    /**
     * Determine whether the touch exploration is enabled (i.e. that description of touched elements is orally
     * fed back to the user) and toggle the ActionBar if it is disabled and if the text to speech is invisible.
     */
    override fun toggleActionBar() {
        launch {
            overlayFragment?.onCenterClicked()
        }
    }

    /**
     * Manage activity destruction.
     */
    override fun onDestroy() {
        super.onDestroy()
        activitiesLaunched.getAndDecrement()
    }

    /**
     * Communicate with the user using a toast if touch exploration is enabled, to indicate the end of a chapter.
     */
    override fun onPageEnded(end: Boolean) {
        if (isExploreByTouchEnabled) {
            if (!pageEnded == end && end) {
                toast("End of chapter")
            }
            pageEnded = end
        }
    }

    override fun playTextChanged(text: String) {
        super.playTextChanged(text)
        findViewById<TextView>(R.id.tts_textView)?.text = text
    }
}