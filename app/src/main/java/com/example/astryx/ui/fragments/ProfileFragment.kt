package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.adapters.BadgeAdapter
import com.example.astryx.ui.adapters.TripsNewestAdapter
import com.example.astryx.ui.adapters.ReviewAdapter
import com.example.astryx.databinding.FragmentProfileBinding
import com.example.astryx.data.repository.BadgeRepository
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.viewmodels.FeedViewModel
import com.example.astryx.ui.dialogs.BadgeUnlockDialog
import com.example.astryx.ui.navigation.Screen
import kotlin.getValue

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private var lastLoadedImageUrl: String? = null
    private lateinit var newestTripsAdapter: TripsNewestAdapter
    private lateinit var badgesAdapter: BadgeAdapter
    private lateinit var badgeDialogHelper: BadgeUnlockDialog

    private lateinit var reviewAdapter: ReviewAdapter

    override fun getUIConfig() = UIConfig(
        title = "My Profile",
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        badgeDialogHelper = BadgeUnlockDialog(requireContext())

        setupBadgesRecyclerView()
        setupTripsRecyclerView()
        setupReviewsRecyclerView()
        setupObservers()
        setupClickListeners()

        userViewModel.getCurrentUid()?.let { uid ->
            userViewModel.fetchUserReviews(uid)
        }

        val isNewUser = requireActivity().intent.getBooleanExtra("IS_NEW_USER", false)
        if (isNewUser) {
            requireActivity().intent.removeExtra("IS_NEW_USER")
            uiViewModel.requestNavigation(ProfileEditFragment.newInstance(isFirstTime = true))
        }
    }

    private fun setupObservers() {
        observeUserViewModel()
        observeTripViewModel()
        observeFeedViewModel()
    }

    private fun observeUserViewModel() {
        userViewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.userName.text = it.name
                binding.userNickname.text = it.username
                binding.userBio.text = it.bio
                binding.savedCounter.text = it.favoriteDestinations.size.toString()

                val allAvailableBadges = BadgeRepository.getAllBadges()
                badgesAdapter.updateData(allAvailableBadges, it.badges)

                if (it.profileImageUrl.isNotEmpty() && it.profileImageUrl != lastLoadedImageUrl) {
                    lastLoadedImageUrl = it.profileImageUrl

                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .placeholder(binding.userPic.drawable)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .dontAnimate()
                        .circleCrop()
                        .into(binding.userPic)
                }
            }
        }

        userViewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let { showError(it) }
        }

        userViewModel.newBadgesEarned.observe(viewLifecycleOwner) { badgeIds ->
            if (!badgeIds.isNullOrEmpty()) {
                val badges = badgeIds.mapNotNull { BadgeRepository.getBadgeById(it) }
                badgeDialogHelper.showBadges(badges)
                userViewModel.onBadgesDialogShown()

            }
        }

        userViewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            val recentReviews = reviews.take(5)
            reviewAdapter.updateData(recentReviews)

            binding.recentReviewsRecyclerView.visibility =
                if (recentReviews.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeTripViewModel() {
        tripViewModel.userTrips.observe(viewLifecycleOwner) { trips ->
            val recentTrips = trips.take(5)
            newestTripsAdapter.updateData(recentTrips)
            binding.tripsCounter.text = trips.size.toString()
            binding.myNewestTripsRecyclerView.visibility =
                if (recentTrips.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun observeFeedViewModel() {
        feedViewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            binding.postsCounter.text = posts.size.toString()
        }
    }

    private fun setupReviewsRecyclerView() {
        reviewAdapter = ReviewAdapter()
        binding.recentReviewsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentReviewsRecyclerView.adapter = reviewAdapter
    }


    private fun setupTripsRecyclerView() {
        newestTripsAdapter = TripsNewestAdapter(mutableListOf()) { trip ->
            tripViewModel.setCurrentTrip(trip)
            uiViewModel.requestNavigation(TripFragment())
        }
        binding.myNewestTripsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.myNewestTripsRecyclerView.adapter = newestTripsAdapter
    }

    private fun setupBadgesRecyclerView() {
        badgesAdapter = BadgeAdapter(emptyList())
        binding.badgeRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3); isNestedScrollingEnabled =
            false; adapter = badgesAdapter
        }
    }

    private fun setupClickListeners() {
        binding.signOutBtn.setOnClickListener {
            showLoading("See you again!")
            userViewModel.startSignOutProcess()
        }
        binding.savedBtn.setOnClickListener {
            uiViewModel.requestNavigation(
                MySavedDestinationsFragment()
            )
        }
        binding.myTripsSeeAllBtn.setOnClickListener {
            uiViewModel.requestNavigation(Screen.TRIPS)
        }
        binding.reviewsSeeAllBtn.setOnClickListener {
            uiViewModel.requestNavigation(
                MyReviewsFragment()
            )
        }
        binding.editProfBtn.setOnClickListener {
            uiViewModel.requestNavigation(
                ProfileEditFragment.newInstance(
                    isFirstTime = false
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
