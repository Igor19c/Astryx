package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentMyReviewsBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.adapters.ReviewAdapter
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.ui.navigation.Screen

class MyReviewsFragment : BaseFragment() {

    private var _binding: FragmentMyReviewsBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    private lateinit var adapter: ReviewAdapter

    override fun getUIConfig() = UIConfig(
        title = "My Reviews",
        isRightBtnVisible = false,
        isHeaderVisible = true,
        leftIconRes = R.drawable.ic_back,
        isLeftBtnVisible = true,
        onLeftClick = { uiViewModel.requestBack() },
        selectedTabId = R.id.nav_profile
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupEmptyState()
        observeViewModel()

    }

    private fun setupEmptyState() {
        binding.emptyStateLayout.apply {
            emptyStateTitle.text = "No space logs yet!"
            emptyStateDescription.text =
                "You haven't shared your galactic adventures. Visited a planet? Tell others about it!"
            emptyStateBtn.text = "Explore Destinations"
            emptyStateBtn.setOnClickListener {
                uiViewModel.requestNavigation(Screen.EXPLORE)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ReviewAdapter(listOf())
        binding.myReviewsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.myReviewsRecyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        userViewModel.userReviews.observe(viewLifecycleOwner) { reviews ->
            val isEmpty = reviews.isNullOrEmpty()

            binding.myReviewsContainer.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.emptyStateLayout.emptyStateContainer.visibility =
                if (isEmpty) View.VISIBLE else View.GONE

            if (!isEmpty) {
                adapter.updateData(reviews)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}