package org.readium.r2.testapp.epub.fragment

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.widget.SeekBar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.readium.r2.testapp.R


class OverlayViewModel : ViewModel() {

    private var totalPages: Int = 0
    val observableSetCurrentPage = MutableLiveData<Int>()
    val observableBrightness = MutableLiveData<Float>()
    val observableAnimateShowTheme = SingleLiveEvent<Unit>()

    val liveTextPageInfo = MutableLiveData<String>()
    val liveButtonLightBackground = MutableLiveData<Drawable>()
    val liveButtonSepiaBackground = MutableLiveData<Drawable>()
    val liveButtonDarkBackground = MutableLiveData<Drawable>()
    val liveButtonNightBackground = MutableLiveData<Drawable>()

    init {
    }

    private lateinit var resources: Resources

    //TODO refactor away and use resource util
    fun passResources(resources: Resources){
        this.resources = resources
        onStyleLightClicked()
    }

    fun onPageSeekChanged(seekBar: SeekBar?, progresValue: Int, fromUser: Boolean) {
        observableSetCurrentPage.postValue(progresValue)
        liveTextPageInfo.postValue("$progresValue / $totalPages")
    }

    fun onBrightnessSeekChanged(seekBar: SeekBar?, progresValue: Int, fromUser: Boolean) {
        val backLightValue = progresValue.toFloat() / 100
        observableBrightness.postValue(backLightValue)
    }

    fun setTotalPages(childCount: Int) {
        totalPages = childCount
    }

    fun onPageSelected(position: Int) {
        liveTextPageInfo.postValue("$position / $totalPages")
    }

    fun onThemeClicked(){
        observableAnimateShowTheme.call()
    }

    fun onDoneClicked(){
        observableAnimateShowTheme.call()
    }

    fun onStyleLightClicked(){
        liveButtonLightBackground.postValue(resources.getDrawable(R.drawable.button_light_selected))
        liveButtonSepiaBackground.postValue(resources.getDrawable(R.drawable.button_sepia))
        liveButtonDarkBackground.postValue(resources.getDrawable(R.drawable.button_dark))
        liveButtonNightBackground.postValue(resources.getDrawable(R.drawable.button_night))
    }

    fun onStyleSepiaClicked(){
        liveButtonLightBackground.postValue(resources.getDrawable(R.drawable.button_light))
        liveButtonSepiaBackground.postValue(resources.getDrawable(R.drawable.button_sepia_selected))
        liveButtonDarkBackground.postValue(resources.getDrawable(R.drawable.button_dark))
        liveButtonNightBackground.postValue(resources.getDrawable(R.drawable.button_night))
    }

    fun onStyleDarkClicked(){
        liveButtonLightBackground.postValue(resources.getDrawable(R.drawable.button_light))
        liveButtonSepiaBackground.postValue(resources.getDrawable(R.drawable.button_sepia))
        liveButtonDarkBackground.postValue(resources.getDrawable(R.drawable.button_dark_selected))
        liveButtonNightBackground.postValue(resources.getDrawable(R.drawable.button_night))
    }

    fun onStyleNightClicked(){
        liveButtonLightBackground.postValue(resources.getDrawable(R.drawable.button_light))
        liveButtonSepiaBackground.postValue(resources.getDrawable(R.drawable.button_sepia))
        liveButtonDarkBackground.postValue(resources.getDrawable(R.drawable.button_dark))
        liveButtonNightBackground.postValue(resources.getDrawable(R.drawable.button_night_selected))
    }
}
