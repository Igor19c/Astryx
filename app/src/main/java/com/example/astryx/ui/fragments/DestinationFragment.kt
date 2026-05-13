package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.DestinationDetails
import com.example.astryx.data.entities.Safety
import com.example.astryx.databinding.FragmentDestinationBinding
import com.example.astryx.databinding.ItemDestHazardBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.utils.IconHelper
import com.example.astryx.ui.adapters.ReviewAdapter
import com.example.astryx.ui.adapters.DestinationsAdapter
import com.example.astryx.data.utils.TripSelectionHelper
import com.example.astryx.data.viewmodels.DestinationViewModel
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.IconHelper.setupPriceTierIcons
import com.example.astryx.data.utils.IconHelper.setupSafetyIcons
import com.example.astryx.data.utils.addTags
import com.example.astryx.data.viewmodels.DestinationUiState
import com.example.astryx.data.viewmodels.TripUiState
import com.google.android.material.tabs.TabLayout

class DestinationFragment : BaseFragment() {

    private var _binding: FragmentDestinationBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private lateinit var childrenAdapter: DestinationsAdapter
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var pendingDestinationToAdd: Destination? = null
    private var isWaitingForSaveConfirmation = false

    override fun getUIConfig() = UIConfig(
        isHeaderVisible = false,
        isBottomNavVisible = false,
        selectedTabId = R.id.nav_explore
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper =
            TripSelectionHelper(requireContext(), tripViewModel, uiViewModel) { destination ->
                pendingDestinationToAdd = destination
            }

        val destination = arguments?.getSerializable("destination_key") as? Destination

        setupAdapters()
        setupTabs()

        destination?.let { dest ->
            setupInitialUI(dest)
            observeViewModel(dest.id)
            destinationViewModel.fetchFullDetails(dest.id)
            setupButtons(dest)
            setupReviewSection(dest)
            destinationViewModel.fetchDestinationReviews(destination.id)

            userViewModel.getCurrentUid()?.let { uid ->
                destinationViewModel.fetchSavedDestinations(uid)
                tripViewModel.fetchUserTrips(uid)
            }

            binding.destSaveDestinationBtn.setOnClickListener {
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.toggleSaveDestination(uid, dest)
                } ?: showError("Please login to save")
            }
        }


        binding.destBackBtn.setOnClickListener { uiViewModel.requestBack() }
    }

    private fun setupTabs() {
        binding.destHighlightsContent.visibility = View.VISIBLE
        binding.destEnvironmentContent.visibility = View.GONE
        binding.destSafetyContent.visibility = View.GONE

        binding.destTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.destHighlightsContent.visibility = View.GONE
                binding.destEnvironmentContent.visibility = View.GONE
                binding.destSafetyContent.visibility = View.GONE

                when (tab?.position) {
                    0 -> binding.destHighlightsContent.visibility = View.VISIBLE
                    1 -> binding.destEnvironmentContent.visibility = View.VISIBLE
                    2 -> binding.destSafetyContent.visibility = View.VISIBLE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        childrenAdapter = DestinationsAdapter(mutableListOf()) { destination ->
            val fragment = DestinationFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("destination_key", destination)
                }
            }
            uiViewModel.requestNavigation(fragment)
        }

        binding.destChildrenRecycleView.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = childrenAdapter
        }

        reviewAdapter = ReviewAdapter(listOf())
        binding.reviewsRecyclerViewDestFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.reviewsRecyclerViewDestFragment.adapter = reviewAdapter
    }

    private fun setupInitialUI(destination: Destination) {
        binding.destTitleText.text = destination.title
        binding.destSubtitleText.text = destination.subtitle
        updateRatingUI(destination.ratingAvg, destination.ratingCount)

        if (destination.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(destination.imageUrl)
                .placeholder(R.drawable.ic_home_earth)
                .into(binding.destImage)
        }

        binding.destTagsChipGroup.removeAllViews()
        binding.destTagsChipGroup.addTags(destination.tags)

        setupPriceTierIcons(binding.destPriceTierContainer, destination.priceTier)

        binding.destShortDescriptionText.text = destination.shortDescription


        if (destination.childCount == 0) {
            binding.destChildrenSection.visibility = View.GONE
        } else {
            binding.destChildrenSection.visibility = View.VISIBLE
            binding.destChildrenTitleText.text = "${destination.title} destinations"
            destinationViewModel.fetchSubDestinations(destination.id)
        }
    }

    private fun updateRatingUI(avg: Double, count: Int) {
        binding.destRatingText.text = String.format("%.1f", avg)
    }

    private fun observeViewModel(destinationId: String) {
        destinationViewModel.destinationDetails.observe(viewLifecycleOwner) { details ->
            details?.let { updateDetailsUI(it) }
        }

        destinationViewModel.subDestinations.observe(viewLifecycleOwner) { list ->
            childrenAdapter.updateData(list)
        }

        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            val isSaved = user?.favoriteDestinations?.contains(destinationId) == true
            updateSaveButtonIcon(isSaved)
        }

        destinationViewModel.saveStatus.observe(viewLifecycleOwner) { statusPair ->
            statusPair?.let { (isSaved, message) ->
                val title = if (isSaved) "Added to Favorites" else "Removed from Favorites"
                showCustomMessage(title, message)
                destinationViewModel.resetSaveStatus()
            }
        }

        destinationViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is DestinationUiState.Success) {
                showCustomMessage("Review shared!", "Thank you for your feedback.")
                destinationViewModel.resetUiState()
                destinationViewModel.fetchFullDetails(destinationId)
            }
        }

        tripViewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip != null && trip.id.isNotEmpty() && pendingDestinationToAdd != null) {
                tripViewModel.addDestination(pendingDestinationToAdd!!)
                pendingDestinationToAdd = null
            }
        }

        destinationViewModel.destReviewSummary.observe(viewLifecycleOwner) { summary ->
            if (summary != null) {
                binding.destReviewCountDestFragment.text = "${summary.totalCount} reviews"
                binding.destAvgRatingDestFragment.text =
                    String.format("%.1f", summary.averageRating)

                IconHelper.updateStarsInContainer(
                    binding.destRatingStarContainerDestFragment,
                    summary.averageRating
                )

                reviewAdapter.updateData(summary.recentReviews)
            }
        }

        tripViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripUiState.Loading -> showLoading(state.message)
                is TripUiState.Error -> {
                    hideLoading()
                    showError(state.message)
                    tripViewModel.resetUiState()
                }

                is TripUiState.Success -> {
                    hideLoading()
                    state.message?.let { showCustomMessage("Success", it) }
                    tripViewModel.resetUiState()
                }

                is TripUiState.Idle -> hideLoading()
            }
        }

    }

    private fun setupReviewSection(destination: Destination) {
        binding.destSubmitReviewBtn.setOnClickListener {
            val user = userViewModel.userData.value
            if (user == null) {
                showError("Please login to leave a review")
                return@setOnClickListener
            }

            val rating = binding.destReviewRatingBar.rating.toInt()
            if (rating == 0) {
                showError("Please select a rating")
                return@setOnClickListener
            }

            destinationViewModel.submitReview(
                destination,
                rating,
                binding.destEditReviewContent.text.toString(),
                user
            )
        }

        binding.destClearReviewBtn.setOnClickListener {
            clearReviewFields()
        }
    }

    private fun clearReviewFields() {
        binding.destReviewRatingBar.rating = 0f
        binding.destEditReviewContent.setText("")
    }

    private fun updateSaveButtonIcon(isSaved: Boolean) {
        val iconRes = if (isSaved) R.drawable.ic_star_filled else R.drawable.ic_star_outlined
        binding.destSaveDestinationBtn.icon = resources.getDrawable(iconRes, null)
    }

    private fun setupButtons(destination: Destination) {
        val canExplore = destination.childCount > 0
        binding.destAddBtn.text =
            if (canExplore) "Explore ${destination.title}" else "Add to Trip"

        binding.destAddBtn.setOnClickListener {
            if (canExplore) {
                val nextFragment = ExploreFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("target_destination", destination)
                    }
                }
                uiViewModel.requestNavigation(nextFragment)
            } else {
                isWaitingForSaveConfirmation = true
                tripSelectionHelper.handleAddToTrip(destination)
            }
        }
    }

    private fun updateDetailsUI(details: DestinationDetails) {
        if (details.longDescription.isNotEmpty()) {
            binding.destLongDescriptionText.text = details.longDescription
        }

        if (details.bestTimeToVisit.isNotEmpty()) {
            binding.destBestTimeToVisitText.text = details.bestTimeToVisit
        }

        binding.destHighlightsContainer.removeAllViews()
        if (details.highlights.isNotEmpty()) {
            details.highlights.forEach { highlight ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_bullet,
                    binding.destHighlightsContainer,
                    false
                )
                itemView.findViewById<TextView>(R.id.bullet_text).text = highlight
                binding.destHighlightsContainer.addView(itemView)
            }
        }

        val env = details.environment ?: return
        binding.envAtmosphereValue.text = env.atmosphere
        binding.envGravityValue.text = "${env.gravityG} G"
        binding.envTempValue.text = if (env.temperatureC != null)
            "${env.temperatureC.min}°C to ${env.temperatureC.max}°C" else "N/A"
        binding.envRadiationValue.text = env.radiation
        binding.envDayLengthValue.text = "${env.dayLengthHours}h"
        binding.envVisibilityValue.text = env.visibility

        details.safety?.let { updateSafetyUI(it) }

        binding.destSafetyNotesContainer.removeAllViews()
        if (details.safety!!.notes.isNotEmpty()) {
            details.safety.notes.forEach { highlight ->
                val itemView = layoutInflater.inflate(
                    R.layout.item_bullet,
                    binding.destSafetyNotesContainer,
                    false
                )
                itemView.findViewById<TextView>(R.id.bullet_text).text = highlight
                binding.destSafetyNotesContainer.addView(itemView)
            }
        }
    }

    private fun updateSafetyUI(safety: Safety) {
        binding.destHazardsContainer.removeAllViews()
        safety.hazards.forEach { hazard ->
            val itemBinding =
                ItemDestHazardBinding.inflate(
                    layoutInflater,
                    binding.destHazardsContainer,
                    false
                )
            itemBinding.hazardName.text = hazard.name
            itemBinding.hazardTipText.text = hazard.tip

            itemBinding.hazardTipHeader.setOnClickListener {
                val isVisible = itemBinding.hazardTipText.visibility == View.VISIBLE
                if (isVisible) {
                    itemBinding.hazardTipText.visibility = View.GONE
                    itemBinding.hazardTipArrow.animate().rotation(0f).setDuration(200).start()
                } else {
                    itemBinding.hazardTipText.visibility = View.VISIBLE
                    itemBinding.hazardTipArrow.animate().rotation(90f).setDuration(200).start()
                }
            }

            setupSafetyIcons(itemBinding.hazardStarsContainer, hazard.level)
            binding.destHazardsContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}