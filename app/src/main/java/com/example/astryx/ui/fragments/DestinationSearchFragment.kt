package com.example.astryx.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentDestinationSearchBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.viewmodels.DestinationViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.transition.MaterialContainerTransform
import androidx.core.view.isNotEmpty
import com.example.astryx.data.entities.Destination
import com.example.astryx.ui.adapters.DestinationsSearchAdapter

enum class DestinationTypes {
    PLANET,
    LOCATION
}

class ExploreSearchFragment : BaseFragment() {

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var _binding: FragmentDestinationSearchBinding? = null
    private val binding get() = _binding!!
    private val expandedCardBinding get() = binding.filterExpandedCard

    private lateinit var resultsAdapter: DestinationsSearchAdapter

    override fun getUIConfig(): UIConfig {
        return UIConfig(
            title = "Search",
            selectedTabId = null,
            isHeaderVisible = true,
            isLeftBtnVisible = true,
            leftIconRes = R.drawable.ic_back,
            onLeftClick = { uiViewModel.requestBack() },
            isRightBtnVisible = false,
            isBottomNavVisible = false
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        destinationViewModel.fetchAllDestinationsForSearch()
        setupRecyclerView()
        setupTypeChips()
        setupListeners()
        setupSearchLogic()
        observeResults()
    }

    private fun setupRecyclerView() {
        resultsAdapter = DestinationsSearchAdapter(mutableListOf()) { destination ->
            destinationViewModel.fetchFullDetails(destination.id)
            openDestinationDetails(destination)
        }
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultsAdapter
        }
    }

    private fun openDestinationDetails(destination: Destination) {
        uiViewModel.requestNavigation(DestinationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("destination_key", destination)
            }
        })
    }

    private fun observeResults() {
        destinationViewModel.filteredDestinations.observe(viewLifecycleOwner) { list ->
            resultsAdapter.updateData(list)

            if (list.isNotEmpty()) {
                binding.searchResultsRecyclerView.post {
                    binding.searchResultsRecyclerView.smoothScrollToPosition(0)
                }
            }
        }
    }

    private fun setupTypeChips() {
        val chipGroup = expandedCardBinding.destTypeChipGroup
        chipGroup.removeAllViews()
        addFilterChip("All", true)
        DestinationTypes.entries.forEach { entry ->
            val formattedName = entry.name.lowercase().replaceFirstChar { it.uppercase() }
            addFilterChip(formattedName, false)
        }
    }

    private fun addFilterChip(label: String, isChecked: Boolean) {
        val chipGroup = expandedCardBinding.destTypeChipGroup
        val chip = LayoutInflater.from(requireContext())
            .inflate(R.layout.layout_single_filter_chip, chipGroup, false) as Chip
        chip.apply {
            this.text = label
            this.isChecked = isChecked
            this.id = View.generateViewId()
        }
        chipGroup.addView(chip)
    }

    private fun setupListeners() {
        binding.filterCollapsedTrigger.setOnClickListener { toggleFilter(true) }
        expandedCardBinding.btnCloseFilter.setOnClickListener { toggleFilter(false) }
        expandedCardBinding.btnApplyFilters.setOnClickListener {
            triggerFilter()
            toggleFilter(false)
        }
        expandedCardBinding.clearFilterBtn.setOnClickListener { clearFilters() }

    }

    private fun triggerFilter() {
        val query = binding.searchInputEditText.text.toString()

        val checkedChipId = expandedCardBinding.destTypeChipGroup.checkedChipId
        val selectedChip = expandedCardBinding.destTypeChipGroup.findViewById<Chip>(checkedChipId)
        val type = selectedChip?.text?.toString() ?: "All"

        val checkedIds = expandedCardBinding.difficultyToggleGroup.checkedButtonIds
        val difficulties = checkedIds.map { id ->
            when (id) {
                R.id.btn_easy -> 1
                R.id.btn_medium -> 2
                R.id.btn_hard -> 3
                else -> 0
            }
        }.filter { it != 0 }

        destinationViewModel.filterDestinations(query, type, difficulties)
    }

    private fun clearFilters() {
        val chipGroup = expandedCardBinding.destTypeChipGroup
        if (chipGroup.isNotEmpty()) {
            (chipGroup.getChildAt(0) as? Chip)?.isChecked = true
        }
        expandedCardBinding.difficultyToggleGroup.clearChecked()
        triggerFilter()
    }

    private fun setupSearchLogic() {
        binding.searchInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                triggerFilter()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun toggleFilter(isOpen: Boolean) {
        val transform = MaterialContainerTransform().apply {
            startView = if (isOpen) binding.filterCollapsedTrigger else expandedCardBinding.root
            endView = if (isOpen) expandedCardBinding.root else binding.filterCollapsedTrigger
            addTarget(if (isOpen) expandedCardBinding.root else binding.filterCollapsedTrigger)
            duration = 450
            scrimColor = Color.TRANSPARENT
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            interpolator = DecelerateInterpolator()
        }
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, transform)
        if (isOpen) {
            binding.searchContainer.visibility = View.GONE
            binding.filterCollapsedTrigger.visibility = View.GONE
            expandedCardBinding.root.visibility = View.VISIBLE
        } else {
            expandedCardBinding.root.visibility = View.GONE
            binding.searchContainer.visibility = View.VISIBLE
            binding.filterCollapsedTrigger.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}