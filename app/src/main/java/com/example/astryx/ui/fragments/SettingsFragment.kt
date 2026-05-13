package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentSettingsBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.dialogs.SelectAccentColorDialog
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.SettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsManager: SettingsManager

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "Settings",
            selectedTabId = null,
            isHeaderVisible = true,
            leftIconRes = R.drawable.ic_back,
            isLeftBtnVisible = true,
            onLeftClick = { uiViewModel.requestBack() },
            isRightBtnVisible = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        settingsManager =
            (requireActivity().application as AstryxApplication).appContainer.settingsManager
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        setupClickListeners()
    }

    private fun setupUI() {
        val isDarkMode = settingsManager.isDarkMode
        binding.darkModeSwitchSettingsFragment.isChecked = isDarkMode
        updateDarkModeIcon(isDarkMode)

        binding.accentColorTextTintSettingsFragment.text = settingsManager.getThemeName()
    }

    private fun setupObservers() {
        userViewModel.loadingState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) showLoading(state.message) else hideLoading()
        }

        userViewModel.passwordResetSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                showCustomMessage(
                    "Email Sent!",
                    "A reset link has been sent to your email address.",
                    5000
                )
                userViewModel.resetPasswordStatus()
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }

    private fun setupClickListeners() {
        binding.darkModeSwitchSettingsFragment.setOnCheckedChangeListener { _, isChecked ->
            showLoading("Applying theme...")
            binding.root.postDelayed({
                TransitionManager.beginDelayedTransition(binding.darkModeContainerSettingsFragment)
                updateDarkModeIcon(isChecked)
                settingsManager.isDarkMode = isChecked
            }, 400)
        }

        binding.btnAccentColorSettingsFragment.setOnClickListener {
            showColorPickerDialog()
        }

        binding.btnLogoutSettingsFragment.setOnClickListener {
            handleLogout()
        }

        binding.btnEditProfileSettingsFragment.setOnClickListener {
            uiViewModel.requestNavigation(ProfileEditFragment.newInstance(isFirstTime = false))
        }

        binding.btnChangePassSettingsFragment.setOnClickListener { showChangePasswordConfirmation() }
        binding.btnFaqSettingsFragment.setOnClickListener { uiViewModel.requestNavigation(FAQFragment()) }
        binding.btnAboutSettingsFragment.setOnClickListener { uiViewModel.requestNavigation(AboutFragment()) }
    }

    private fun showChangePasswordConfirmation() {
        val email = userViewModel.userData.value?.email ?: "your email"

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Reset Password")
            .setMessage("We will send a password reset link to:\n$email\n\nYou will remain logged in to Astryx during this process.")
            .setPositiveButton("Send Email") { _, _ ->
                settingsManager.needsReAuthAfterPasswordReset = true
                userViewModel.sendPasswordResetEmail()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDarkModeIcon(isDark: Boolean) {
        val iconRes = if (isDark) R.drawable.ic_moon else R.drawable.ic_sun
        binding.btnDarkModeSetingsFragment.setIconResource(iconRes)
        binding.darkModeSwitchSettingsFragment.thumbIconDrawable =
            ContextCompat.getDrawable(requireContext(), iconRes)
    }

    private fun showColorPickerDialog() {
        val dialog = SelectAccentColorDialog.newInstance()
        dialog.setOnColorSelectedListener { selectedTheme ->
            settingsManager.selectedThemeResId = selectedTheme
            binding.root.postDelayed({
                requireActivity().recreate()
            }, 600)
        }
        dialog.show(parentFragmentManager, SelectAccentColorDialog.TAG)
    }

    private fun handleLogout(
        onLogout: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .setOnCancelListener { onCancel?.invoke() }
            .setPositiveButton("Logout") { _, _ ->
                userViewModel.startSignOutProcess()
                onLogout?.invoke()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
