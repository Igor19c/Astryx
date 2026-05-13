package com.example.astryx.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentProfileEditBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.dialogs.SelectImageSourceDialog
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory

class ProfileEditFragment : BaseFragment() {

    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var selectedImageUri: Uri? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                updatePreviewImage(uri)
            }
        }

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "Edit Profile",
            isHeaderVisible = true,
            isLeftBtnVisible = true,
            leftIconRes = R.drawable.ic_back,
            onLeftClick = { uiViewModel.requestBack() },
            isRightBtnVisible = false,
            isBottomNavVisible = false,
            selectedTabId = R.id.nav_profile
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.userData.observe(viewLifecycleOwner) { currentUser ->
            currentUser?.let {
                if (binding.editTextNameEditProfileFragment.text.toString().isEmpty()) {
                    binding.editTextNameEditProfileFragment.setText(it.name)
                }
                if (binding.editTextUsernameEditProfileFragment.text.toString().isEmpty()) {
                    binding.editTextUsernameEditProfileFragment.setText(it.username)
                }
                if (binding.editTextBioEditProfileFragment.text.toString().isEmpty()) {
                    binding.editTextBioEditProfileFragment.setText(it.bio)
                }
                if (selectedImageUri == null && !it.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(it.profileImageUrl)
                        .placeholder(binding.profilePicEditProfileFragment.drawable)
                        .circleCrop()
                        .into(binding.profilePicEditProfileFragment)
                }
            }
        }


        binding.profilePicEditProfileFragment.setOnClickListener { showAvatarPicker() }
        binding.btnUpdatePicEditProfileFragment.setOnClickListener { showAvatarPicker() }

        binding.saveBtnEditProfileFragment.setOnClickListener {
            val isFirstTime = arguments?.getBoolean(ARG_IS_FIRST_TIME, false) ?: false

            userViewModel.updateProfile(
                binding.editTextNameEditProfileFragment.text.toString(),
                binding.editTextUsernameEditProfileFragment.text.toString(),
                binding.editTextBioEditProfileFragment.text.toString()
            )
            if (!isFirstTime) {
                showCustomMessage("Profile Updated!", "Your profile has been updated.")
            }
            selectedImageUri?.let { userViewModel.uploadProfileImage(it, requireContext().assets) }
            uiViewModel.requestBack()
        }

        binding.cancelBtnEditProfileFragment.setOnClickListener { uiViewModel.requestBack() }

        if (arguments?.getBoolean(ARG_IS_FIRST_TIME, false) == true) {
            showCustomMessage(
                title = "Welcome, Space Traveler!",
                body = "Your account is ready. Let's start by personalizing your profile!",
                duration = 5000
            )
            arguments?.putBoolean(ARG_IS_FIRST_TIME, false)
        }
    }

    private fun showAvatarPicker() {
        userViewModel.loadAvatars(requireContext().assets)
        userViewModel.avatarPaths.observe(viewLifecycleOwner) { avatarFiles ->
            val fullPaths = avatarFiles.map { "file:///android_asset/avatars/$it" }

            val dialog = SelectImageSourceDialog.newInstance("Select Avatar", fullPaths)
            dialog.setOnImageSelectedListener { path -> updatePreviewImage(Uri.parse(path)) }
            dialog.setOnPlusClickListener { openGallery() }
            dialog.show(parentFragmentManager, SelectImageSourceDialog.TAG)
        }
    }

    private fun openGallery() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updatePreviewImage(uri: Uri) {
        selectedImageUri = uri
        Glide.with(this).load(uri).circleCrop().into(binding.profilePicEditProfileFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IS_FIRST_TIME = "is_first_time"

        fun newInstance(isFirstTime: Boolean = false): ProfileEditFragment {
            val fragment = ProfileEditFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_FIRST_TIME, isFirstTime)
            fragment.arguments = args
            return fragment
        }
    }
}
