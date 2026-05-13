package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentHomeBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.utils.IconHelper
import com.example.astryx.data.utils.setupFadeOnScroll
import com.example.astryx.ui.adapters.ReviewAdapter
import com.example.astryx.ui.adapters.DestinationsAdapter
import com.example.astryx.data.viewmodels.DestinationViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.ui.navigation.Screen

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var reviewAdapter: ReviewAdapter

    private lateinit var trendingDestinationsAdapter: DestinationsAdapter

    private val destinationViewModel: DestinationViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    override fun getUIConfig() = UIConfig(
        title = "ASTRYX",
        selectedTabId = R.id.place_holder
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTrendingDestinationsRecyclerView()
        setupRecyclerView()
        observeViewModel()

        destinationViewModel.fetchDestinationsByRating()
        destinationViewModel.fetchHomeReviews()

        binding.btnHeaderExploreHomeFragment.setOnClickListener{
            uiViewModel.requestNavigation(Screen.EXPLORE)
        }
        val scrollView = binding.nestedScrollViewHomeFragment
        val fadeViews = listOf(
            binding.imageHeaderHomeFragment,
            binding.titleHomeFragment,
            binding.subtitleHomeFragment,
            binding.btnHeaderExploreHomeFragment,
            binding.highlightsContainerHomeFragment
        )

        scrollView.setupFadeOnScroll(fadeViews)

    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter(listOf())
        binding.reviewsRecyclerViewHomeFragment.layoutManager =
            LinearLayoutManager(requireContext())
        binding.reviewsRecyclerViewHomeFragment.adapter = reviewAdapter
    }

    private fun observeViewModel() {
        destinationViewModel.trendingDestinations.observe(viewLifecycleOwner) { destinations ->
            trendingDestinationsAdapter.updateData(destinations)
        }

        destinationViewModel.homeReviewSummary.observe(viewLifecycleOwner) { summary ->
            reviewAdapter.updateData(summary.recentReviews)

            binding.reviewCountHomeFragment.text = "${summary.totalCount} reviews"
            binding.globalAvgRatingHomeFragment.text = String.format("%.1f", summary.averageRating)

            IconHelper.updateStarsInContainer(
                binding.ratingStarContainerHomeFragment,
                summary.averageRating
            )
        }
    }

    private fun setupTrendingDestinationsRecyclerView() {
        trendingDestinationsAdapter = DestinationsAdapter(mutableListOf()) { destination ->
            uiViewModel.requestNavigation(DestinationFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("destination_key", destination)
                }
            })
        }
        binding.trendingRecyclerViewHomeFragment.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL, false
        )
        binding.trendingRecyclerViewHomeFragment.adapter = trendingDestinationsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
