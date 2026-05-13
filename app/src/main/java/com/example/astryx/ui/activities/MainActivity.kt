package com.example.astryx.ui.activities

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.data.utils.SettingsManager
import com.example.astryx.data.utils.getColorFromAttr
import com.example.astryx.data.utils.gone
import com.example.astryx.data.utils.hideSystemBars
import com.example.astryx.data.utils.invisible
import com.example.astryx.data.utils.visible
import com.example.astryx.data.viewmodels.FeedViewModel
import com.example.astryx.data.viewmodels.TripViewModel
import com.example.astryx.databinding.ActivityMainBinding
import com.example.astryx.data.viewmodels.UIViewModel
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.ui.navigation.AppNavigator
import com.example.astryx.ui.navigation.Screen
import com.firebase.ui.auth.AuthUI

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userViewModel: UserViewModel by viewModels { ViewModelFactory(application as AstryxApplication) }
    private val tripViewModel: TripViewModel by viewModels { ViewModelFactory(application as AstryxApplication) }
    private val feedViewModel: FeedViewModel by viewModels { ViewModelFactory(application as AstryxApplication) }
    private val uiViewModel: UIViewModel by viewModels()
    private lateinit var settingsManager: SettingsManager
    private var isProgrammaticChange = false
    val navigator by lazy { AppNavigator(this, R.id.main_frame) }

    override fun onCreate(savedInstanceState: Bundle?) {
        settingsManager = (application as AstryxApplication).appContainer.settingsManager
        settingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hideSystemBars()

        setupUserObserver()
        setupUIObserver()
        setupNavigation()

        if (savedInstanceState == null) {
            val isNewUser = intent.getBooleanExtra("IS_NEW_USER", false)
            val initialScreen = if (isNewUser) Screen.PROFILE else Screen.HOME
            uiViewModel.requestNavigation(initialScreen)
            if (isNewUser) binding.btnNavigation.selectedItemId = R.id.nav_profile
        }
    }

    private fun setupUserObserver() {
        userViewModel.getCurrentUid()?.let { uid ->
            userViewModel.startListening(uid)
            tripViewModel.fetchUserTrips(uid)
            feedViewModel.fetchUserPosts(uid)
        }

        userViewModel.isLoggedOut.observe(this) { loggedOut ->
            if (loggedOut) {
                uiViewModel.requestLogout()
            }
        }

        userViewModel.triggerSignOut.observe(this) { shouldSignOut ->
            if (shouldSignOut) {
                AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener {
                        userViewModel.onSignOutCleanupComplete()
                    }
            }
        }

        tripViewModel.userTrips.observe(this) {
            userViewModel.refreshBadgeCheck()
        }
        feedViewModel.userPosts.observe(this) {
            userViewModel.refreshBadgeCheck()
        }
        userViewModel.userReviews.observe(this) {
            userViewModel.refreshBadgeCheck()
        }
    }

    private fun setupUIObserver() {
        uiViewModel.uiConfig.observe(this) { config ->
            updateHeader(config)
            updateNavigationUI(config)
        }

        uiViewModel.navigationRequest.observe(this) { request ->
            request?.let {
                navigator.handleNavigationRequest(it)
                uiViewModel.onNavigationHandled()
            }
        }
    }

    private fun setupNavigation() {
        binding.homeBtn.setOnClickListener {
            uiViewModel.requestNavigation(
                Screen.HOME,
                forceRefresh = navigator.activeFragmentClass == "HomeFragment"
            )
        }

        binding.btnNavigation.setOnItemSelectedListener { item ->
            if (isProgrammaticChange) {
                isProgrammaticChange = false
                return@setOnItemSelectedListener true
            }
            uiViewModel.requestNavigation(Screen.fromId(item.itemId))
            true
        }

        binding.btnNavigation.setOnItemReselectedListener { item ->
            uiViewModel.requestNavigation(Screen.fromId(item.itemId), forceRefresh = true)
        }
    }

    private fun updateNavigationUI(config: UIConfig) {
        if (config.isBottomNavVisible) binding.navContainer.visible() else binding.navContainer.gone()

        config.selectedTabId?.let { id ->
            val menuItem = binding.btnNavigation.menu.findItem(id)
            updateBottomNavStyles(id)

            if (binding.btnNavigation.selectedItemId != id && menuItem?.isEnabled == true) {
                isProgrammaticChange = true
                binding.btnNavigation.selectedItemId = id
            }
        }
    }

    private fun updateBottomNavStyles(selectedItemId: Int) {
        val isHome = selectedItemId == R.id.place_holder

        val colorStateList = ContextCompat.getColorStateList(this, R.color.nav_btn_tint)
        binding.btnNavigation.itemIconTintList = colorStateList
        binding.btnNavigation.itemTextColor = colorStateList

        val fabLabelColor = if (isHome) {
            getColorFromAttr(R.attr.customColorPrimary)
        } else {
            ContextCompat.getColor(this, R.color.btn_nav_unselected)
        }
        binding.homeBtnLabel.setTextColor(fabLabelColor)

    }

    private fun updateHeader(config: UIConfig) {
        binding.headerTitle.text = config.title
        if (config.isHeaderVisible) binding.mainHeader.visible() else binding.mainHeader.gone()

        binding.customHeaderContainer.removeAllViews()
        if (config.customHeaderView != null) {
            binding.defaultHeaderTitleContainer.gone()
            binding.customHeaderContainer.visible()
            (config.customHeaderView.parent as? ViewGroup)?.removeView(config.customHeaderView)
            binding.customHeaderContainer.addView(config.customHeaderView)
        } else {
            binding.defaultHeaderTitleContainer.visible()
            binding.customHeaderContainer.gone()
        }

        binding.mainHeaderLeftBtn.apply {
            if (config.isLeftBtnVisible) visible() else gone()
            setIconResource(config.leftIconRes)
            setOnClickListener {
                config.onLeftClick?.invoke() ?: uiViewModel.requestNavigation(Screen.SETTINGS)
            }
        }

        binding.mainHeaderRightBtn.apply {
            if (config.isRightBtnVisible) visible() else invisible()
            setIconResource(config.rightIconRes)
            setOnClickListener {
                config.onRightClick?.invoke() ?: uiViewModel.requestNavigation(Screen.SEARCH)
            }
        }
    }
}
