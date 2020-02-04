package org.readium.r2.testapp.epub.fragment

import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class OverlayViewModel : ViewModel() {

    private var totalPages: Int = 0
    val observableSetCurrentPage = MutableLiveData<Int>()
    val observableBrightness = MutableLiveData<Float>()
    val observableAnimateShowTheme = SingleLiveEvent<Unit>()

    val liveTextPageInfo = MutableLiveData<String>()
    val liveTextDoneVisibility = MutableLiveData<Int>()

    init {
        liveTextDoneVisibility.postValue(View.GONE)
    }

    fun onPageSeekChanged(seekBar: SeekBar?, progresValue: Int, fromUser: Boolean) {
        Log.d("seek", "value $progresValue")
        observableSetCurrentPage.postValue(progresValue)
        liveTextPageInfo.postValue("$progresValue / $totalPages")
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

    fun onBrightnessChanged(value: Int) {
        val backLightValue = value.toFloat() / 100
        Log.d("Backlight", "$backLightValue")
        observableBrightness.postValue(backLightValue)
    }
}
