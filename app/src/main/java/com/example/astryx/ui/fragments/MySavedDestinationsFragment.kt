package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.ui.adapters.DestinationsSavedAdapter
import com.example.astryx.databinding.FragmentMySavedBinding
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.viewmodels.DestinationViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.setupSwipeAction
import com.example.astryx.data.viewmodels.DestinationUiState
import com.example.astryx.ui.navigation.Screen
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MySavedDestinationsFragment : BaseFragment() {

    private var _binding: FragmentMySavedBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private lateinit var adapter: DestinationsSavedAdapter

    override fun getUIConfig() = UIConfig(
        title = "Saved",
        isBottomNavVisible = false,
        isHeaderVisible = true,
        isLeftBtnVisible = true,
        leftIconRes = R.drawable.ic_back,
        onLeftClick = { uiViewModel.requestBack() },
        isRightBtnVisible = false,
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupEmptyState()

        userViewModel.getCurrentUid()?.let { uid ->
            destinationViewModel.fetchSavedDestinations(uid)
        }

        binding.unsaveAllBtn.setOnClickListener {
            showUnsaveAllConfirmation()
        }
    }

    private fun setupEmptyState() {
        binding.emptyStateLayout.apply {
            emptyStateIcon.setImageResource(R.drawable.ic_rocket)
            emptyStateTitle.text = "Explore new destinations 🚀"
            emptyStateDescription.text = "And save them here"
            emptyStateBtn.text = "Explore Destinations"
            emptyStateBtn.setOnClickListener {
                uiViewModel.requestNavigation(Screen.EXPLORE)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DestinationsSavedAdapter(mutableListOf())
        binding.myDestinationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@MySavedDestinationsFragment.adapter

            setupSwipeAction<DestinationsSavedAdapter.ViewHolder>(
                getCardContainer = { it.cardContainer },
                onSwiped = { position ->
                    val destination =
                        this@MySavedDestinationsFragment.adapter.getDestinationAt(position)
                    handleUnsaveDestination(
                        destination,
                        onDeleted = {
                            showCustomMessage(
                                "Destination Removed!",
                                "Destination has been removed from your saved list"
                            )
                        },
                        onCancel = {
                            this@MySavedDestinationsFragment.adapter.notifyItemChanged(
                                position
                            )
                        }
                    )
                }
            )
        }
    }

    private fun handleUnsaveDestination(
        destination: Destination, onDeleted: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Delete Destination")
            .setMessage("Are you sure you want to delete '${destination.title}'?")
            .setNegativeButton("Cancel") { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .setPositiveButton("Delete") { _, _ ->
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.unsaveDestination(uid, destination.id)
                    onDeleted?.invoke()
                }
            }.show()

    }

    private fun showUnsaveAllConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
            .setTitle("Unsave All")
            .setMessage("Are you sure you want to remove all saved destinations?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove All") { _, _ ->
                userViewModel.getCurrentUid()?.let { uid ->
                    destinationViewModel.unsaveAllDestinations(uid)
                    showCustomMessage(
                        title = "Cleared",
                        body = "All destinations removed.",
                        duration = 3000
                    )
                }
            }
            .show()
    }

    private fun observeViewModel() {
        destinationViewModel.savedDestinations.observe(viewLifecycleOwner) { destinations ->
            val isEmpty = destinations.isNullOrEmpty()

            binding.apply {
                destCountSavedFragment.text = "${destinations.size} saved"
                myDestinationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                emptyStateLayout.emptyStateContainer.visibility =
                    if (isEmpty) View.VISIBLE else View.GONE
                unsaveAllBtn.visibility = if (isEmpty) View.INVISIBLE else View.VISIBLE
            }

            if (!isEmpty) {
                adapter.updateData(destinations)
            }
        }

        destinationViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is DestinationUiState.Error) {
                showError(state.message)
                destinationViewModel.resetUiState()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
