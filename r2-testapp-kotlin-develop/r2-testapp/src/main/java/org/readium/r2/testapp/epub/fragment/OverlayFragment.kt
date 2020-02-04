package org.readium.r2.testapp.epub.fragment

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentOverlayBinding
import org.readium.r2.testapp.epub.EpubActivity

class OverlayFragment : Fragment() {

    private lateinit var constraintSet2: ConstraintSet
    private lateinit var constraintSet1: ConstraintSet
    var changed = false
    private lateinit var binding: FragmentOverlayBinding

    private lateinit var viewModel: OverlayViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_overlay, container, false)
        postponeEnterTransition()

        constraintSet1 = ConstraintSet()
        constraintSet1.clone(binding.frameTop.constraintViewTop)
        constraintSet2 = ConstraintSet()
        constraintSet2.clone(requireContext(), R.layout.include_ebook_display)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.frameTop.constraintViewTop.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.frameTop.constraintViewTop.viewTreeObserver.removeOnGlobalLayoutListener(
                    this
                )
                binding.frameTop.constraintViewTop.translationY =
                    -binding.frameTop.constraintViewTop.height.toFloat()
            }
        })

        binding.frameBottom.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.frameBottom.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.frameBottom.translationY = binding.frameBottom.height.toFloat()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(OverlayViewModel::class.java)
        viewModel.passResources(resources)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        viewModel.observableSetCurrentPage.observe(viewLifecycleOwner, Observer {
            it?.let {
                (activity as EpubActivity).resourcePager.currentItem = it
            }
        })

        viewModel.observableAnimateShowTheme.observe(viewLifecycleOwner, Observer {
            TransitionManager.beginDelayedTransition(binding.frameTop.constraintViewTop)
            val constraint = if (changed) constraintSet1 else constraintSet2
            constraint.applyTo(binding.frameTop.constraintViewTop)
            changed = !changed
        })

        viewModel.observableBrightness.observe(viewLifecycleOwner, Observer {
            it?.let {
                setBrightness(it)
            }
        })

        viewModel.observableSetPageColor.observe(viewLifecycleOwner, Observer {
            it?.let {
                (activity as EpubActivity).updateAppearance(it)
            }
        })
    }

    fun onCenterClicked() {
        if (binding.frameTop.constraintViewTop.translationY == -binding.frameTop.constraintViewTop.height.toFloat()) {
            binding.frameTop.constraintViewTop.animate().translationY(0f).duration =
                ANIMATION_DURATION
            binding.frameBottom.animate().translationY(0f).duration = ANIMATION_DURATION
        } else {
            binding.frameTop.constraintViewTop.animate()
                .translationY(-binding.frameTop.constraintViewTop.height.toFloat()).duration =
                ANIMATION_DURATION
            binding.frameBottom.animate().translationY(binding.frameBottom.height.toFloat())
                .duration =
                ANIMATION_DURATION
        }
    }

    fun setTotalPages(childCount: Int) {
        binding.seekPages.max = childCount
        viewModel.setTotalPages(childCount)
    }

    fun onPageSelected(position: Int) {
        viewModel.onPageSelected(position)
    }

    private fun setBrightness(value: Float) {
        val layoutParams = (context as AppCompatActivity).window.attributes
        layoutParams.screenBrightness = value
        activity?.window?.attributes = layoutParams
    }

    companion object {
        fun newInstance() = OverlayFragment()
        const val ANIMATION_DURATION = 200L
    }
}
