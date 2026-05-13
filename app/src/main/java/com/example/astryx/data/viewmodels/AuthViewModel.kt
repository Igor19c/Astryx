package com.example.astryx.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astryx.data.repository.firebase.interfaces.IUserRepository
import com.example.astryx.data.utils.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: IUserRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Authenticated(val isNewUser: Boolean) : AuthState()
        data class Error(val message: String) : AuthState()
        object RequiresLogin : AuthState()
    }

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun checkAuthState(forceRelogin: Boolean) {
        if (forceRelogin || settingsManager.needsReAuthAfterPasswordReset) {
            settingsManager.needsReAuthAfterPasswordReset = false
            userRepository.signOut()
            _authState.value = AuthState.RequiresLogin
            return
        }

        val currentUserUid = userRepository.getCurrentUid()
        if (currentUserUid == null) {
            _authState.value = AuthState.RequiresLogin
        } else {
            _authState.value = AuthState.Loading
            viewModelScope.launch {
                try {
                    val result = userRepository.observeUser(currentUserUid).first()
                    result.onSuccess { user ->
                        if (user != null) {
                            _authState.value = AuthState.Authenticated(isNewUser = false)
                        } else {
                            _authState.value = AuthState.Authenticated(isNewUser = true)
                        }
                    }.onFailure {
                        _authState.value = AuthState.RequiresLogin
                    }
                } catch (e: Exception) {
                    _authState.value = AuthState.Error("Error validating session")
                }
            }
        }
    }

    fun handleSignInResult(success: Boolean, isCanceled: Boolean, isNewUser: Boolean) {
        if (success) {
            _authState.value = AuthState.Authenticated(isNewUser = isNewUser)
        } else {
            if (isCanceled) {
                _authState.value = AuthState.Idle
            } else {
                _authState.value = AuthState.Error("Sign in failed. Please try again.")
            }
        }
    }

}
