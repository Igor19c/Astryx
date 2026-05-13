package com.example.astryx.ui.navigation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.astryx.R
import com.example.astryx.data.viewmodels.UIViewModel
import com.example.astryx.ui.activities.AuthActivity

class AppNavigator(
    private val activity: AppCompatActivity,
    private val containerId: Int,
) {

    enum class NavigationAnimation {
        SLIDE_UP, FADE, NONE
    }

    private var activeFragment: Fragment? = null

    val activeFragmentClass: String?
        get() = activeFragment?.javaClass?.simpleName


    fun handleNavigationRequest(request: UIViewModel.NavigationRequest) {
        when (request) {
            is UIViewModel.NavigationRequest.ToScreen -> {
                navigateTo(request.screen, request.forceRefresh)
            }

            is UIViewModel.NavigationRequest.ToFragment -> {
                navigateToFragment(request.fragment)
            }

            is UIViewModel.NavigationRequest.Back -> {
                activity.supportFragmentManager.popBackStack()
            }

            is UIViewModel.NavigationRequest.Logout -> {
                navigateToAuth()
            }
        }
    }
    fun navigateTo(screen: Screen, forceRefresh: Boolean = false) {
        val fragmentManager = activity.supportFragmentManager

        if (screen.isTopLevel && fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        if (forceRefresh) {
            fragmentManager.findFragmentByTag(screen.tag)?.let {
                fragmentManager.beginTransaction().remove(it).commitNow()
            }
        }

        val target = fragmentManager.findFragmentByTag(screen.tag) ?: screen.create()
        executeTransaction(target, screen.tag, addToBackStack = !screen.isTopLevel)
    }

    fun navigateToFragment(
        fragment: Fragment,
        animation: NavigationAnimation = NavigationAnimation.SLIDE_UP,
        addToBackStack: Boolean = true
    ) {
        if (fragment is DialogFragment) {
            fragment.show(activity.supportFragmentManager, fragment.javaClass.simpleName)
            return
        }

        val tag = fragment.javaClass.simpleName
        executeTransaction(fragment, tag, addToBackStack, animation)
    }

    fun navigateToAuth() {
        val intent = Intent(activity, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }

    private fun executeTransaction(
        fragment: Fragment,
        tag: String,
        addToBackStack: Boolean,
        animation: NavigationAnimation = NavigationAnimation.FADE
    ) {
        if (fragment === activeFragment && !addToBackStack) return

        activity.supportFragmentManager.beginTransaction().apply {
            when (animation) {
                NavigationAnimation.SLIDE_UP -> setCustomAnimations(
                    R.anim.slide_in_bottom_to_top,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.slide_out_top_to_bottom
                )

                NavigationAnimation.FADE -> setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )

                NavigationAnimation.NONE -> {}
            }

            activity.supportFragmentManager.fragments.forEach {
                if (it != fragment && it.isAdded && !it.isHidden) hide(it)
            }

            if (!fragment.isAdded) {
                add(containerId, fragment, tag)
            } else {
                show(fragment)
            }

            if (addToBackStack) addToBackStack(tag)

            commit()
        }

        activeFragment = fragment
    }
}
