package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.ui.adapters.TripDestinationsAdapter
import com.example.astryx.databinding.FragmentTripBinding
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.dialogs.EditTripDialog
import com.example.astryx.data.utils.TripSelectionHelper
import com.example.astryx.data.utils.setupSwipeAction
import com.example.astryx.data.utils.showAsFullScreen
import com.example.astryx.data.viewmodels.TripUiState
import com.example.astryx.ui.navigation.Screen


class TripFragment : BaseFragment() {

    private var _binding: FragmentTripBinding? = null
    private val binding get() = _binding!!

    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private lateinit var tripAdapter: TripDestinationsAdapter
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var isSubmitting = false

    override fun getUIConfig() = UIConfig(
        isBottomNavVisible = false,
        isHeaderVisible = true,
        isLeftBtnVisible = true,
        leftIconRes = R.drawable.ic_back,
        onLeftClick = {
            tripViewModel.setEditMode(false)
            uiViewModel.requestBack()
        },
        isRightBtnVisible = true,
        selectedTabId = R.id.nav_trips
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(requireContext(), tripViewModel, uiViewModel)

        val currentTrip = tripViewModel.currentTrip.value
        if ((currentTrip == null || currentTrip.status.isEmpty()) && !isSubmitting) {
            EditTripDialog.newInstance().show(parentFragmentManager, "ADD_TRIP_DIALOG")
        }

        if (savedInstanceState == null && tripViewModel.isEditMode.value != true) {
            val startInEditMode = arguments?.getBoolean("start_edit") ?: false
            if (startInEditMode) tripViewModel.setEditMode(true)
        }

        setupEmptyState()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupEmptyState() {
        binding.emptyStateLayout.apply {
            emptyStateIcon.setImageResource(R.drawable.ic_rocket)
            emptyStateTitle.text = "No stops planned yet"
            emptyStateDescription.text =
                "Your itinerary is looking a bit empty. \nReady to add some star systems to your route?"
            emptyStateBtn.text = "Plan My First Trip"
        }
    }

    private fun setupClickListeners() {
        val navigateToExplore = {
            tripViewModel.setEditMode(true)
            uiViewModel.requestNavigation(Screen.EXPLORE)
        }

        binding.editBtn.setOnClickListener {
            EditTripDialog.newInstance(isEdit = true)
                .show(parentFragmentManager, "EDIT_TRIP_DIALOG")
            tripViewModel.setEditMode(true)
        }

        binding.finalizeBtnTripItem.setOnClickListener {
            tripViewModel.currentTrip.value?.let { trip ->
                tripSelectionHelper.handleFinalizeTrip(trip)
            }
        }

        binding.addDestinationBtn.setOnClickListener { navigateToExplore() }
        binding.emptyStateLayout.emptyStateBtn.setOnClickListener { navigateToExplore() }
    }

    private fun observeViewModel() {
        tripViewModel.selectedDestinations.observe(viewLifecycleOwner) { destinations ->
            val isEmpty = destinations.isNullOrEmpty()

            binding.itineraryRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateLayout.emptyStateContainer.visibility =
                if (isEmpty) View.VISIBLE else View.GONE

            tripAdapter.updateData(destinations ?: emptyList())

            refreshUiComponents()
        }

        tripViewModel.currentTrip.observe(viewLifecycleOwner) { trip ->
            if (trip == null) return@observe
            if (trip.status.isNotEmpty()) isSubmitting = false

            uiViewModel.updateUI(getUIConfig().copy(title = trip.title.ifEmpty { "New Journey" }))

            refreshUiComponents()
        }

        tripViewModel.isEditMode.observe(viewLifecycleOwner) {
            refreshUiComponents()
        }

        tripViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TripUiState.Error -> {
                    isSubmitting = false
                    showError(state.message)
                    tripViewModel.resetUiState()
                }

                is TripUiState.Success -> {
                    state.message?.let { showCustomMessage("Success", it) }
                    tripViewModel.resetUiState()
                }

                else -> {}
            }
        }
    }

    private fun refreshUiComponents() {
        val trip = tripViewModel.currentTrip.value ?: return
        val isEditing = tripViewModel.isEditMode.value ?: false
        val hasDestinations = !tripViewModel.selectedDestinations.value.isNullOrEmpty()

        if (trip.status.isEmpty() || isSubmitting) {
            binding.root.visibility = View.INVISIBLE
            return
        }

        binding.root.visibility = View.VISIBLE

        val isForcedCompleted = arguments?.getBoolean("is_completed") ?: false
        val isCompleted = isForcedCompleted || trip.isCompleted

        val startFormatted = trip.getFormattedStartDate()
        val endFormatted = trip.getFormattedEndDate()

        if (startFormatted != null && endFormatted != null) {
            binding.tripDepartureDateText.text = startFormatted
            binding.tripReturnDateText.text = endFormatted
            binding.departureDateContainer.visibility = View.VISIBLE
            binding.returnDateContainer.visibility = View.VISIBLE
        } else {
            binding.departureDateContainer.visibility = View.GONE
            binding.returnDateContainer.visibility = View.GONE
        }

        binding.editBtn.visibility =
            if (!isCompleted && hasDestinations) View.VISIBLE else View.GONE
        binding.addDestinationBtn.visibility =
            if (!isCompleted && hasDestinations) View.VISIBLE else View.GONE

        val canFinalize = !isCompleted && hasDestinations && (trip.isDraft || isEditing)
        binding.finalizeBtnTripItem.visibility = if (canFinalize) View.VISIBLE else View.GONE

        tripAdapter.setEditMode(!isCompleted && (trip.isDraft || isEditing))

    }

    private fun setupRecyclerView() {
        tripAdapter = TripDestinationsAdapter(mutableListOf()) { source, url ->
            source.showAsFullScreen(url)
        }
        binding.itineraryRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = tripAdapter

            setupSwipeAction<TripDestinationsAdapter.ViewHolder>(
                getCardContainer = { it.cardContainer },
                canSwipe = {
                    val trip = tripViewModel.currentTrip.value
                    val isEditing = tripViewModel.isEditMode.value ?: false
                    trip?.isDraft == true || (trip?.isPlanned == true && isEditing)
                },
                onSwiped = { position ->
                    val trip = tripViewModel.currentTrip.value
                    val isEditing = tripViewModel.isEditMode.value ?: false
                    if (trip?.isDraft == true || (trip?.isPlanned == true && isEditing)) {
                        val destination = tripAdapter.getDestinationAt(position)
                        tripSelectionHelper.handleDeleteTripDestination(
                            destination,
                            onDeleted = { showCustomMessage("Success", "Destination removed") },
                            onCancel = { tripAdapter.notifyItemChanged(position) }
                        )
                    } else {
                        tripAdapter.notifyItemChanged(position)
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
