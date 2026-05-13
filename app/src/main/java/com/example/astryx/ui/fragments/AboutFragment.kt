package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.astryx.R
import com.example.astryx.databinding.FragmentAboutBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.navigation.Screen
import com.example.astryx.data.utils.applyGradientText
import com.example.astryx.data.utils.applyGradientTint

class AboutFragment : BaseFragment() {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "About",
            selectedTabId = null,
            isHeaderVisible = true,
            leftIconRes = R.drawable.ic_back,
            isLeftBtnVisible = true,
            onLeftClick = { uiViewModel.requestBack() },
            isRightBtnVisible = false,
            isBottomNavVisible = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()

        binding.btnBookJourneyAboutFragment.setOnClickListener {
            uiViewModel.requestNavigation(Screen.SETTINGS)
        }

    }

    private fun setupUi() {
        binding.titleAboutFragment.applyGradientText()
        binding.tvSuccessfulMissionsCounter.applyGradientText()
        binding.tvHappyTravelersCounter.applyGradientText()
        binding.tvYearsInOperationCounter.applyGradientText()
        binding.tvPlanetsVisitedCounter.applyGradientText()
        binding.footerIconAboutFragment.applyGradientTint()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}