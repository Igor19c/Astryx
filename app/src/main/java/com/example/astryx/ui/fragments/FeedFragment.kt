package com.example.astryx.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.databinding.FragmentFeedBinding
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.adapters.PostsAdapter
import com.example.astryx.ui.dialogs.SelectTripDialog
import com.example.astryx.data.viewmodels.FeedViewModel
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.getColorFromAttr
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FeedFragment : BaseFragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val userViewModel: UserViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }
    private val tripViewModel: TripViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application as AstryxApplication)
    }

    override fun getUIConfig() = UIConfig(
        title = "Feed",
        selectedTabId = R.id.nav_feed,
        isRightBtnVisible = false
    )

    private lateinit var adapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        userViewModel.getCurrentUid()?.let { tripViewModel.fetchUserTrips(it) }

        binding.addPostFab.setOnClickListener {
            showSelectTripDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            feedViewModel.fetchPosts()
        }

        binding.swipeRefreshLayout.setColorSchemeColors(
            requireContext().getColorFromAttr(R.attr.customColorPrimary),
            requireContext().getColorFromAttr(R.attr.customColorPrimaryVariant),
            resources.getColor(R.color.success)
        )
    }

    private fun showSelectTripDialog() {
        val trips = tripViewModel.userTrips.value ?: emptyList()

        if (trips.isEmpty()) {
            showCustomMessage(
                title = "No trips found!",
                body = "You need at least one trip to create a post.",
                duration = 3000
            )
            return
        }

        val dialog = SelectTripDialog.newInstance()
        dialog.setOnTripSelectedListener { selectedTrip ->
            uiViewModel.requestNavigation(PostCreateFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("selectedTrip", selectedTrip)
                }
            })
        }
        dialog.show(parentFragmentManager, "TAG")
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            posts = mutableListOf(),
            currentUserId = userViewModel.getCurrentUid(),
            onEditClick = { post ->
                val editFragment = PostCreateFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("editingPost", post)
                    }
                }
                uiViewModel.requestNavigation(editFragment)
            },
            onDeleteClick = { post ->
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.CustomAlertDialog
                ).setTitle("Delete Post")
                    .setMessage("Are you sure you want to remove this post from the feed?")
                    .setPositiveButton("Delete") { _, _ ->
                        feedViewModel.deletePost(post)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onLikeClick = { post ->
                val uid = userViewModel.getCurrentUid()
                if (uid != null) {
                    feedViewModel.toggleLikePost(uid, post)
                }
            }
        )
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FeedFragment.adapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun observeViewModel() {
        feedViewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.updateData(posts)
            binding.postsRecyclerView.visibility = if (posts.isEmpty()) View.GONE else View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }

        feedViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (binding.swipeRefreshLayout.isRefreshing && !isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        feedViewModel.postDeleted.observe(viewLifecycleOwner) { deleted ->
            if (deleted) {
                showCustomMessage("Post Removed", "Your post has been deleted successfully.")
                feedViewModel.resetPostDeletedState()
            }
        }

        feedViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showCustomMessage(
                    title = "Error",
                    body = it,
                    duration = 3000
                )
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}