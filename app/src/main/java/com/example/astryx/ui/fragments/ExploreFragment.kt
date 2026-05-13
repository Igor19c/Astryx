package com.example.astryx.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentExploreBinding
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.adapters.CarouselAdapter
import com.example.astryx.data.utils.TripSelectionHelper
import com.example.astryx.data.viewmodels.DestinationViewModel
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.IconHelper.setupDifficultyIcons
import com.example.astryx.data.utils.IconHelper.setupPriceTierIcons
import com.example.astryx.data.utils.IconHelper.setupSafetyIcons
import com.example.astryx.data.viewmodels.DestinationUiState
import com.example.astryx.data.viewmodels.ExploreViewModel
import com.example.astryx.data.viewmodels.TripUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator

class ExploreFragment : BaseFragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val exploreViewModel: ExploreViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var isWaitingForSaveConfirmation = false

    private lateinit var carouselAdapter: CarouselAdapter
    private var mediator: TabLayoutMediator? = null
    private var isLevelTransition = false
    private lateinit var tripSelectionHelper: TripSelectionHelper
    private var pendingDestinationToAdd: Destination? = null

    // Header View References
    private var headerProgressBar: ProgressBar? = null
    private var headerDotPlanet: ImageView? = null
    private var headerDotLocation: ImageView? = null
    private var headerLabelPlanet: TextView? = null
    private var headerLabelLocation: TextView? = null

    override fun getUIConfig(): UIConfig {
        val context = context ?: return UIConfig(
            selectedTabId = R.id.nav_explore,
            isHeaderVisible = true
        )

        val headerView = LayoutInflater.from(context).inflate(R.layout.layout_explore_header, null)
        headerProgressBar = headerView.findViewById(R.id.trip_progress_bar_header)
        headerDotPlanet = headerView.findViewById(R.id.dot_planet_header)
        headerDotLocation = headerView.findViewById(R.id.dot_location_header)
        headerLabelPlanet = headerView.findViewById(R.id.dot_label_planet_header)
        headerLabelLocation = headerView.findViewById(R.id.dot_label_location_header)

        val navState = exploreViewModel.navigationUiState.value
        if (navState != null) {
            updateProgressUI(navState.planetActive, navState.locationActive)
        }

        val isRoot: Boolean = exploreViewModel.currentExploreParentId == "root"

        return UIConfig(
            selectedTabId = R.id.nav_explore,
            isHeaderVisible = true,
            isLeftBtnVisible = true,
            leftIconRes = if (isRoot) R.drawable.ic_settings else R.drawable.ic_back,
            onLeftClick = if (isRoot) null else {
                { handleBackNavigation() }
            },
            isRightBtnVisible = true,
            customHeaderView = headerView
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (exploreViewModel.currentExploreParentId == "root" && savedInstanceState == null) {
            arguments?.getSerializable("target_destination")?.let {
                val dest = it as Destination
                exploreViewModel.currentExploreParentId = dest.id
                exploreViewModel.currentExploreParentType = dest.type
                exploreViewModel.currentExploreParentTitle = dest.title
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripSelectionHelper =
            TripSelectionHelper(requireContext(), tripViewModel, uiViewModel) { destination ->
                pendingDestinationToAdd = destination
                isWaitingForSaveConfirmation = true
            }

        setupViewPager()
        observeViewModel()

        binding.exploreHelpBtn.setOnClickListener {
            showExplanationDialog()
        }

        userViewModel.getCurrentUid()?.let { tripViewModel.fetchUserTrips(it) }

        loadDestinationsWithAnimation(animateOut = false)
    }

    private fun setupViewPager() {
        carouselAdapter = CarouselAdapter(emptyList())

        binding.carouselViewPager.apply {
            adapter = carouselAdapter
            offscreenPageLimit = 3
            getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            setPageTransformer(MarginPageTransformer(40))

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (carouselAdapter.itemCount > 0) {
                        updateDestinationDetails(carouselAdapter.getItem(position))
                    }
                }
            })
        }
    }

    private fun updateDestinationDetails(destination: Destination) {
        if (isLevelTransition) {
            applyDestinationData(destination)
            return
        }

        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 200 }
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 300 }

        binding.destTitleCarouselItem.startAnimation(fadeOut)
        binding.destSubtitleCarouselItem.startAnimation(fadeOut)
        binding.destTravelTimeContainerCarouselItem.startAnimation(fadeOut)
        binding.destRatingChipCarouselItem.startAnimation(fadeOut)

        fadeOut.setAnimationListener(object :
            android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(p0: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(p0: android.view.animation.Animation?) {}
            override fun onAnimationEnd(p0: android.view.animation.Animation?) {
                applyDestinationData(destination)
                binding.destTitleCarouselItem.startAnimation(fadeIn)
                binding.destSubtitleCarouselItem.startAnimation(fadeIn)
                binding.destTravelTimeContainerCarouselItem.startAnimation(fadeIn)
                binding.destRatingChipCarouselItem.startAnimation(fadeIn)
            }
        })
    }

    private fun applyDestinationData(destination: Destination) {
        binding.destRatingTextCarouselItem.text = String.format("%.1f", destination.ratingAvg)
        binding.destTitleCarouselItem.text = destination.title
        binding.destSubtitleCarouselItem.text = destination.subtitle
        binding.destTravelTimeTextCarouselItem.text = "${destination.getTravelTimeInt()} days"

        setupPriceTierIcons(
            binding.destPriceTierContainerCarouselItem,
            destination.priceTier
        )

        setupSafetyIcons(
            binding.destSafetyLvlContainerCarouselItem,
            destination.safetyLevel
        )

        if (destination.childCount == 0) {
            binding.destDifficultyContainerCarouselItem.visibility = View.VISIBLE
            setupDifficultyIcons(
                binding.destDifficultyContainerCarouselItem,
                destination.difficulty
            )
        } else
            binding.destDifficultyContainerCarouselItem.visibility = View.GONE

        binding.detailsBtn.setOnClickListener { openDestinationDetails(destination) }
        binding.selectBtn.text = if (destination.childCount == 0) "Add to Trip" else "Explore"
        binding.selectBtn.setOnClickListener {
            if (destination.childCount > 0) {
                navigateToChildren(destination)
            } else {
                isWaitingForSaveConfirmation = true
                tripSelectionHelper.handleAddToTrip(destination)
            }
        }
    }

    private fun handleDestinationsUpdate(list: List<Destination>) {
        carouselAdapter.updateData(list)

        mediator?.detach()
        mediator = TabLayoutMediator(
            binding.carouselIndicator,
            binding.carouselViewPager
        ) { tab, _ ->
            tab.setIcon(R.drawable.tab_selector)
        }
        mediator?.attach()

        val indexToSelect = exploreViewModel.calculateInitialIndex(list)

        binding.carouselViewPager.post {
            if (_binding == null) return@post
            binding.carouselViewPager.setCurrentItem(indexToSelect, false)
            binding.carouselIndicator.selectTab(binding.carouselIndicator.getTabAt(indexToSelect))

            applyDestinationData(list[indexToSelect])

            binding.exploreContentLayout.animate()
                .alpha(1f)
                .setDuration(400)
                .withEndAction {
                    exploreViewModel.explorePendingSelectedIndex = null
                    isLevelTransition = false
                }
                .start()

            binding.carouselViewPager.requestLayout()
        }
    }

    private fun observeViewModel() {
        destinationViewModel.destinations.observe(viewLifecycleOwner) { list ->
            if (list.isNotEmpty()) handleDestinationsUpdate(list)
        }

        exploreViewModel.navigationUiState.observe(viewLifecycleOwner) { state ->
            updateProgressUI(state.planetActive, state.locationActive)
        }

        destinationViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is DestinationUiState.Error) {
                showError(state.message)
                destinationViewModel.resetUiState()
            }
        }
        destinationViewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state is DestinationUiState.Error) {
                showError(state.message)
                binding.exploreContentLayout.alpha = 1f
                isLevelTransition = false
                destinationViewModel.resetUiState()
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

        tripViewModel.currentTrip.observe(viewLifecycleOwner)
        { trip ->
            if (trip != null && trip.id.isNotEmpty() && pendingDestinationToAdd != null) {
                tripViewModel.addDestination(pendingDestinationToAdd!!)
                pendingDestinationToAdd = null
            }
        }
    }

    private fun loadDestinationsWithAnimation(animateOut: Boolean = true) {
        isLevelTransition = true

        if (animateOut) {
            binding.exploreContentLayout.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    performDataFetch()
                }
                .start()
        } else {
            binding.exploreContentLayout.alpha = 0f
            performDataFetch()
        }
    }

    private fun performDataFetch() {
        uiViewModel.updateUI(getUIConfig())
        destinationViewModel.fetchDestinations(
            exploreViewModel.currentExploreParentId
        )
    }

    private fun navigateToChildren(destination: Destination) {
        val currentIndex = binding.carouselViewPager.currentItem
        exploreViewModel.exploreTo(destination, currentIndex)
        loadDestinationsWithAnimation()
    }

    private fun openDestinationDetails(destination: Destination) {
        exploreViewModel.explorePendingSelectedIndex = binding.carouselViewPager.currentItem

        uiViewModel.requestNavigation(DestinationFragment().apply {
            arguments = Bundle().apply {
                putSerializable("destination_key", destination)
            }
        })
    }

    private fun updateProgressUI(bodyActive: Boolean, locationActive: Boolean) {
        val currentParentId = exploreViewModel.currentExploreParentId
        val progress = if (currentParentId == "root") 3 else 100

        headerProgressBar?.progress = progress
        headerDotPlanet?.setImageResource(if (bodyActive || currentParentId == "root") R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        headerDotLocation?.setImageResource(if (locationActive) R.drawable.ic_dot_active else R.drawable.ic_dot_inactive)
        headerDotLocation?.backgroundTintList =
            (if (locationActive) null else ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.explore_header_dot_inactive
                )
            ))

        val currentContext = context ?: return
        val activeColor = ContextCompat.getColor(currentContext, R.color.subtitle)
        val inactiveColor = ContextCompat.getColor(currentContext, R.color.body)

        headerLabelPlanet?.setTextColor(if (bodyActive || currentParentId == "root") activeColor else inactiveColor)
        headerLabelLocation?.setTextColor(if (locationActive) activeColor else inactiveColor)
    }

    private fun handleBackNavigation() {
        if (exploreViewModel.exploreBack()) {
            loadDestinationsWithAnimation()
        }
    }

    private fun showExplanationDialog() {
        val currentContext = context ?: return
        val dialogView =
            LayoutInflater.from(currentContext).inflate(R.layout.dialog_explore_explanation, null)
        val dialog = MaterialAlertDialogBuilder(currentContext, R.style.CustomAlertDialog)
            .setView(dialogView)
            .show()

        dialogView.findViewById<View>(R.id.lets_go_btn_explore_dialog).setOnClickListener {
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator?.detach()
        mediator = null
        _binding = null

        headerProgressBar = null
        headerDotPlanet = null
        headerDotLocation = null
        headerLabelPlanet = null
        headerLabelLocation = null
    }
}
