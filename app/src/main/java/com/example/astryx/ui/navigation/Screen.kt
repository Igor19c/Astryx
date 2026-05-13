package com.example.astryx.ui.navigation

import android.view.View
import androidx.fragment.app.Fragment
import com.example.astryx.R
import com.example.astryx.ui.fragments.*

enum class Screen(val menuId: Int, val tag: String, val isTopLevel: Boolean = true) {
    TRIPS(R.id.nav_trips, "MyTripsFragment") { override fun create() = MyTripsFragment() },
    PROFILE(R.id.nav_profile, "ProfileFragment") { override fun create() = ProfileFragment() },
    EXPLORE(R.id.nav_explore, "ExploreFragment") { override fun create() = ExploreFragment() },
    FEED(R.id.nav_feed, "FeedFragment") { override fun create() = FeedFragment() },
    HOME(R.id.place_holder, "HomeFragment") { override fun create() = HomeFragment() },
    SETTINGS(View.NO_ID, "SettingsFragment", false) { override fun create() = SettingsFragment() },
    SEARCH(View.NO_ID, "ExploreSearchFragment", false) { override fun create() = ExploreSearchFragment() };

    abstract fun create(): Fragment

    companion object {
        fun fromId(id: Int) = entries.find { it.menuId == id } ?: HOME
    }
}
