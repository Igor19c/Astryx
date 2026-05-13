package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.ui.adapters.TripsAdapter
import com.example.astryx.databinding.FragmentMyTripsBinding
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.dialogs.EditTripDialog
import com.example.astryx.data.utils.TripSelectionHelper
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.setupSwipeAction
import com.example.astryx.data.viewmodels.TripUiState
import com.example.astryx.ui.navigation.Screen
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MyTripsFragment : BaseFragment() {

    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private lateinit var draftTripsAdapter: TripsAdapter

    private lateinit var plannedTripsAdapter: TripsAdapter

    private lateinit var completedTripsAdapter: TripsAdapter

    private lateinit var tripSelectionHelper: TripSelectionHelper

    override fun getUIConfig() = UIConfig(
        title = "My Trips",
        selectedTabId = R.id.nav_trips
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper = TripSelectionHelper(requireContext(), tripViewModel, uiViewModel)

        setupRecyclerView()
        setupEmptyState()
        setupClickListeners()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { uid ->
            tripViewModel.fetchUserTrips(uid)
        }
    }

    private fun setupEmptyState() {
        binding.emptyStateLayout.apply {
            emptyStateIcon.setImageResource(R.drawable.ic_rocket)
            emptyStateTitle.text = "Ready for your first adventure?"
            emptyStateDescription.text = "Start planning your dream trip across the galaxy today!"
            emptyStateBtn.text = "Explore Destinations"
        }
    }

    private fun setupClickListeners() {
        val createTripAction = {
            tripViewModel.createNewTrip()
            val dialog = EditTripDialog.newInstance()
            dialog.setOnSaveSuccessListener {
                navigateToTripEditor(tripViewModel.currentTrip.value ?: Trip())
            }
            dialog.show(parentFragmentManager, "CREATE_TRIP_DIALOG")
        }

        binding.myTripsHelpBtn.setOnClickListener {
            showExplanationDialog()
        }
        binding.createTripBtn.setOnClickListener { createTripAction() }

        binding.emptyStateLayout.emptyStateBtn.setOnClickListener {
            uiViewModel.requestNavigation(Screen.EXPLORE)
        }
    }

    private fun setupRecyclerView() {
        draftTripsAdapter = TripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.draftTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.draftTripsRecyclerView.adapter = draftTripsAdapter

        plannedTripsAdapter = TripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )
        binding.plannedTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.plannedTripsRecyclerView.adapter = plannedTripsAdapter

        completedTripsAdapter = TripsAdapter(
            trips = mutableListOf(),
            onOpenClick = { trip ->
                tripViewModel.setCurrentTrip(trip)
                navigateToTripEditor(trip)
            },
            onConfirmClick = { trip -> tripSelectionHelper.handleFinalizeTrip(trip) },
            onShareClick = { trip -> navigateToCreatePost(trip) }
        )

        binding.completedTripsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.completedTripsRecyclerView.adapter = completedTripsAdapter


        binding.draftTripsRecyclerView.setupSwipeAction<TripsAdapter.ViewHolder>(
            getCardContainer = { it.cardContainer },
            onSwiped = { pos -> handleDeleteTripAction(draftTripsAdapter, pos) }
        )

        binding.plannedTripsRecyclerView.setupSwipeAction<TripsAdapter.ViewHolder>(
            getCardContainer = { it.cardContainer },
            onSwiped = { pos -> handleDeleteTripAction(plannedTripsAdapter, pos) }
        )

        binding.completedTripsRecyclerView.setupSwipeAction<TripsAdapter.ViewHolder>(
            getCardContainer = { it.cardContainer },
            onSwiped = { pos -> handleDeleteTripAction(completedTripsAdapter, pos) }
        )
    }

    private fun handleDeleteTripAction(adapter: TripsAdapter, position: Int) {
        val trip = adapter.getTripAt(position)
        tripSelectionHelper.handleDeleteTrip(
            trip,
            onDeleted = { },
            onCancel = { adapter.notifyItemChanged(position) }
        )
    }

    private fun showExplanationDialog() {
        val currentContext = context ?: return
        val dialogView =
            LayoutInflater.from(currentContext).inflate(R.layout.dialog_my_trips_explanation, null)

        val dialog = MaterialAlertDialogBuilder(currentContext, R.style.CustomAlertDialog)
            .setView(dialogView)
            .show()

        dialogView.findViewById<View>(R.id.lets_go_btn_my_trips_dialog).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun observeViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val isEmpty = trips.isNullOrEmpty()

            binding.tripsListContainer.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateLayout.emptyStateContainer.visibility =
                if (isEmpty) View.VISIBLE else View.GONE
            binding.createTripBtn.visibility = if (isEmpty) View.GONE else View.VISIBLE

            if (!isEmpty) {
                val drafts = trips.filter { it.isDraft }
                val planned = trips.filter { it.status == "PLANNED" && !it.isCompleted }
                val completed = trips.filter { it.isCompleted }

                draftTripsAdapter.updateData(drafts)
                plannedTripsAdapter.updateData(planned)
                completedTripsAdapter.updateData(completed)

                binding.draftTripsCountMyTripsFragment.text = "${drafts.size} trips"
                binding.plannedTripsCountMyTripsFragment.text = "${planned.size} trips"
                binding.completedTripsCountMyTripsFragment.text = "${completed.size} trips"

                binding.draftTripsContainerMyTripsFragment.visibility =
                    if (drafts.isEmpty()) View.GONE else View.VISIBLE
                binding.draftTripsRecyclerView.visibility =
                    if (drafts.isEmpty()) View.GONE else View.VISIBLE

                binding.plannedTripsContainerMyTripsFragment.visibility =
                    if (planned.isEmpty()) View.GONE else View.VISIBLE
                binding.plannedTripsRecyclerView.visibility =
                    if (planned.isEmpty()) View.GONE else View.VISIBLE

                binding.completedTripsContainerMyTripsFragment.visibility =
                    if (completed.isEmpty()) View.GONE else View.VISIBLE
                binding.completedTripsRecyclerView.visibility =
                    if (completed.isEmpty()) View.GONE else View.VISIBLE

            }
        }

        tripViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is TripUiState.Error) {
                showError(state.message)
                tripViewModel.resetUiState()
            }
        }

    }

    private fun navigateToCreatePost(trip: Trip) {
        uiViewModel.requestNavigation(PostCreateFragment().apply {
            arguments = Bundle().apply {
                putSerializable("selectedTrip", trip)
                putBoolean("isSharingTrip", true)
            }
        })
    }

    private fun navigateToTripEditor(trip: Trip) {
        uiViewModel.requestNavigation(TripFragment().apply {
            arguments = Bundle().apply {
                putBoolean("start_edit", false)
                putBoolean("is_completed", trip.isCompleted)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
