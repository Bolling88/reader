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

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.epub.R2EpubActivity
import org.readium.r2.shared.*
import org.readium.r2.testapp.R
import org.readium.r2.testapp.epub.fragment.OverlayFragment
import org.readium.r2.testapp.library.activitiesLaunched

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