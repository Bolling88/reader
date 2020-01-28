package org.readium.r2.testapp.epub.fragment

import android.util.Log
import android.widget.SeekBar
import androidx.lifecycle.ViewModel


class OverlayViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    fun onPageSeekChanged(seekBar: SeekBar?, progresValue: Int, fromUser: Boolean) {
        Log.d("seek", "value $progresValue")
    }
}
