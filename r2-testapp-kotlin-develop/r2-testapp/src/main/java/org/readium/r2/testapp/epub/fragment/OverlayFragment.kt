package org.readium.r2.testapp.epub.fragment

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.databinding.DataBindingUtil
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentOverlayBinding
import org.readium.r2.testapp.epub.EpubActivity

class OverlayFragment : Fragment() {

    private lateinit var binding: FragmentOverlayBinding

    private lateinit var viewModel: OverlayViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_overlay, container, false)
        postponeEnterTransition()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.frameTop.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.frameTop.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.frameTop.translationY = -binding.frameTop.height.toFloat()
            }
        })

        binding.frameBottom.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.frameBottom.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.frameBottom.translationY = binding.frameBottom.height.toFloat()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(OverlayViewModel::class.java)
    }

    fun onCenterClicked(){
        if (binding.frameTop.translationY == -binding.frameTop.height.toFloat()) {
            binding.frameTop.animate().translationY(0f).duration = ANIMATION_DURATION
            binding.frameBottom.animate().translationY(0f).duration = ANIMATION_DURATION
        } else {
            binding.frameTop.animate().translationY(-binding.frameTop.height.toFloat()).duration =
                ANIMATION_DURATION
            binding.frameBottom.animate().translationY(binding.frameBottom.height.toFloat()).duration =
                ANIMATION_DURATION
        }
    }

    companion object {
        fun newInstance() = OverlayFragment()
        const val ANIMATION_DURATION = 200L
    }
}
