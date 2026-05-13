package com.example.astryx.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentCreatePostBinding
import com.example.astryx.data.entities.Post
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.utils.openUriInputStream
import com.example.astryx.ui.dialogs.SelectImageSourceDialog
import com.example.astryx.data.viewmodels.FeedViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory

class PostCreateFragment : BaseFragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var selectedTrip: Trip? = null
    private var editingPost: Post? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { updatePreviewImage(it) }
        }

    override fun getUIConfig() = UIConfig(
        title = if (editingPost != null) "Edit Post" else "Create Post",
        isBottomNavVisible = false,
        selectedTabId = R.id.nav_feed
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedTrip = arguments?.getSerializable("selectedTrip") as? Trip
        editingPost = arguments?.getSerializable("editingPost") as? Post

        setupInitialUI()
        setupObservers()

        if (editingPost != null) {
            binding.btnUpdatePicPostFragment.visibility = View.GONE
            binding.picPostFragment.isClickable = false
            binding.btnSavePostFragment.text = "Update"
        } else {
            binding.btnUpdatePicPostFragment.setOnClickListener {
                showImagePicker()
            }
        }

        binding.btnSavePostFragment.setOnClickListener {
            handleSaveAction()
        }

        binding.btnCancelPostFragment.setOnClickListener { uiViewModel.requestBack() }
    }

    private fun setupObservers() {
        feedViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showCustomMessage("Error", it)
                feedViewModel.resetError()
            }
        }

        feedViewModel.validationError.observe(viewLifecycleOwner) { error ->
            binding.titlePostFragment.error = error
        }

        feedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (editingPost == null) showLoading("Publishing Post")
            } else {
                hideLoading()
            }
        }

        feedViewModel.postSaved.observe(viewLifecycleOwner) { isSaved ->
            if (isSaved) {
                val title = if (editingPost == null) "Post Published!" else "Post Updated!"
                val body =
                    if (editingPost == null) "Your journey has been shared." else "Your adventure has been updated."
                showCustomMessage(title, body)

                uiViewModel.requestBack()
                feedViewModel.resetPostSavedState()
            }
        }
    }

    private fun handleSaveAction() {
        val title = binding.titlePostFragment.text.toString()
        val description = binding.descriptionPostFragment.text.toString()
        val rating = binding.ratingBarPostFragment.rating.toInt()
        val user = userViewModel.userData.value ?: return

        val selectedUri = feedViewModel.selectedImageUri.value
        val inputStream = selectedUri?.let { requireContext().openUriInputStream(it) }

        if (editingPost != null) {
            feedViewModel.updatePost(
                existingPost = editingPost!!,
                newTitle = title,
                newDescription = description,
                newRating = rating,
                imageInputStream = inputStream,
            )
        } else {
            feedViewModel.savePost(
                user = user,
                title = title,
                description = description,
                rating = rating,
                imageInputStream = inputStream,
                tripImageUrl = selectedTrip?.imageUrl ?: ""
            )
        }
    }

    private fun setupInitialUI() {
        if (editingPost != null) {
            binding.titlePostFragment.setText(editingPost!!.title)
            binding.descriptionPostFragment.setText(editingPost!!.description)
            binding.ratingBarPostFragment.rating = editingPost!!.rating.toFloat()
            if (editingPost!!.imageUrl.isNotEmpty()) {
                Glide.with(this).load(editingPost!!.imageUrl).into(binding.picPostFragment)
            }
        } else if (selectedTrip != null) {
            binding.titlePostFragment.setText(selectedTrip!!.title)
            val isSharingTrip = arguments?.getBoolean("isSharingTrip", false) ?: false
            if (isSharingTrip) {
                binding.descriptionPostFragment.setText("Check out my journey with ${selectedTrip!!.destinationIds.size} stops!")
            }

            val tripImageUrl = selectedTrip!!.imageUrl
            if (tripImageUrl.isNotEmpty()) {
                Glide.with(this).load(tripImageUrl).into(binding.picPostFragment)

                if (tripImageUrl.contains("android_asset/")) {
                    feedViewModel.setSelectedImageUri(Uri.parse(tripImageUrl))
                }
            }
        }
    }

    private fun showImagePicker() {
        val tripImages = mutableListOf<String>()
        selectedTrip?.let { trip ->
            if (trip.imageUrl.isNotEmpty()) tripImages.add(trip.imageUrl)
            tripImages.addAll(trip.destinationImages.filter { it.isNotEmpty() })
        }
        editingPost?.let { post ->
            if (post.imageUrl.isNotEmpty() && !tripImages.contains(post.imageUrl)) {
                tripImages.add(post.imageUrl)
            }
        }

        val dialog = SelectImageSourceDialog.newInstance("Select Post Image", tripImages)
        dialog.setOnImageSelectedListener { path -> updatePreviewImage(Uri.parse(path)) }
        dialog.setOnPlusClickListener { pickImageLauncher.launch("image/*") }
        dialog.show(parentFragmentManager, SelectImageSourceDialog.TAG)
    }

    private fun updatePreviewImage(uri: Uri) {
        if (uri.toString().startsWith("http")) {
            feedViewModel.setSelectedImageUri(null)
        } else {
            feedViewModel.setSelectedImageUri(uri)
        }
        Glide.with(this).load(uri).into(binding.picPostFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
