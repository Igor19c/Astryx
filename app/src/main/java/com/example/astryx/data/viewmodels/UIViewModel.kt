package com.example.astryx.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.astryx.data.entities.UIConfig
import com.example.astryx.ui.navigation.Screen
import androidx.fragment.app.Fragment

class UIViewModel : ViewModel() {
    private val _uiConfig = MutableLiveData<UIConfig>()
    val uiConfig: LiveData<UIConfig> = _uiConfig

    sealed class NavigationRequest {
        data class ToScreen(val screen: Screen, val forceRefresh: Boolean = false) :
            NavigationRequest()

        data class ToFragment(val fragment: Fragment) : NavigationRequest()
        object Back : NavigationRequest()
        object Logout : NavigationRequest()
    }

    private val _navigationRequest = MutableLiveData<NavigationRequest?>()
    val navigationRequest: LiveData<NavigationRequest?> = _navigationRequest

    fun updateUI(config: UIConfig) {
        _uiConfig.value = config
    }

    fun requestNavigation(screen: Screen, forceRefresh: Boolean = false) {
        _navigationRequest.value = NavigationRequest.ToScreen(screen, forceRefresh)
    }

    fun requestNavigation(fragment: Fragment) {
        _navigationRequest.value = NavigationRequest.ToFragment(fragment)
    }

    fun requestBack() {
        _navigationRequest.value = NavigationRequest.Back
    }

    fun requestLogout() {
        _navigationRequest.value = NavigationRequest.Logout
    }

    fun onNavigationHandled() {
        _navigationRequest.value = null
    }
}
