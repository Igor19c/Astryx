package com.example.astryx.data.viewmodels

import android.content.res.AssetManager
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astryx.data.utils.BadgeManager
import com.example.astryx.data.entities.Post
import com.example.astryx.data.entities.Review
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.entities.User
import com.example.astryx.data.repository.firebase.interfaces.IPostRepository
import com.example.astryx.data.repository.firebase.interfaces.IReviewRepository
import com.example.astryx.data.repository.firebase.interfaces.ITripRepository
import com.example.astryx.data.repository.firebase.interfaces.IUserRepository
import com.example.astryx.data.utils.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class UserViewModel(
    private val userRepository: IUserRepository,
    private val reviewRepository: IReviewRepository,
    private val postRepository: IPostRepository,
    private val tripRepository: ITripRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    data class LoadingState(val isLoading: Boolean, val message: String? = null)

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _userReviews = MutableLiveData<List<Review>>()
    val userReviews: LiveData<List<Review>> = _userReviews

    private val _avatarPaths = MutableLiveData<List<String>>()
    val avatarPaths: LiveData<List<String>> = _avatarPaths

    private val _loadingState = MutableLiveData<LoadingState>(LoadingState(false))
    val loadingState: LiveData<LoadingState> = _loadingState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _newBadgesEarned = MutableLiveData<List<String>?>()
    val newBadgesEarned: LiveData<List<String>?> = _newBadgesEarned

    private val _passwordResetSent = MutableLiveData<Boolean>()
    val passwordResetSent: LiveData<Boolean> = _passwordResetSent

    private val _isLoggedOut = MutableLiveData(false)
    val isLoggedOut: LiveData<Boolean> = _isLoggedOut

    private val _triggerSignOut = MutableLiveData(false)
    val triggerSignOut: LiveData<Boolean> = _triggerSignOut

    private val grantedBadgesIds = mutableSetOf<String>()
    private var isUpdatingBadges = false

    fun getCurrentUid(): String? = userRepository.getCurrentUid()

    fun createProfileFromFirebase() {
        val uid = getCurrentUid() ?: return
        _loadingState.value = LoadingState(true, "Creating your profile...")

        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().currentUser?.reload()?.await()

                val (name, email) = userRepository.getFirebaseUserProperties()
                val newUser = User(
                    uid = uid,
                    name = name ?: "Traveler",
                    email = email ?: "",
                    createdAt = System.currentTimeMillis()
                )

                val result = userRepository.createUserProfile(newUser)
                if (result.isSuccess) {
                    _userData.value = newUser
                } else {
                    _error.value = "Failed to create database profile"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loadingState.value = LoadingState(false)
            }
        }
    }

    fun startListening(uid: String) {
        settingsManager.switchUser(uid)
        viewModelScope.launch {
            userRepository.observeUser(uid).collectLatest { result ->
                result.onSuccess { user ->
                    if (_loadingState.value?.isLoading != true) {
                        _userData.value = user
                    }
                    user?.badges?.let { grantedBadgesIds.addAll(it) }
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun fetchUserReviews(uid: String) {
        viewModelScope.launch {
            reviewRepository.fetchUserReviews(uid).collectLatest { result ->
                result.onSuccess { reviews ->
                    _userReviews.value = reviews
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun checkForBadges(trips: List<Trip>, posts: List<Post>, reviews: List<Review>) {
        if (isUpdatingBadges) return
        val currentUser = _userData.value ?: return
        val uid = getCurrentUid() ?: return

        grantedBadgesIds.addAll(currentUser.badges)
        val newBadgeIds = BadgeManager.checkNewBadges(currentUser, trips, posts, reviews)
            .filter { it !in grantedBadgesIds }

        if (newBadgeIds.isNotEmpty()) {
            isUpdatingBadges = true
            grantedBadgesIds.addAll(newBadgeIds)
            val updatedBadgeList = (currentUser.badges + newBadgeIds).distinct()

            viewModelScope.launch {
                val result = userRepository.updateUserBadges(uid, updatedBadgeList)
                isUpdatingBadges = false
                result.onSuccess {
                    _newBadgesEarned.value = newBadgeIds
                }.onFailure { e ->
                    _error.value = "Failed to update badges: ${e.message}"
                    grantedBadgesIds.removeAll(newBadgeIds.toSet())
                }
            }
        }
    }

    fun onBadgesDialogShown() {
        _newBadgesEarned.value = null
    }

    fun refreshBadgeCheck() {
        val uid = getCurrentUid() ?: return

        viewModelScope.launch {
            val trips = tripRepository.fetchUserTrips(uid).first().getOrDefault(emptyList())
            val posts = postRepository.fetchUserPosts(uid).first().getOrDefault(emptyList())
            val reviews = reviewRepository.fetchUserReviews(uid).first().getOrDefault(emptyList())

            checkForBadges(trips, posts, reviews)
        }
    }

    fun startSignOutProcess() {
        _loadingState.value = LoadingState(true, "Signing out...")
        grantedBadgesIds.clear()
        _triggerSignOut.value = true
    }

    fun onSignOutCleanupComplete() {
        userRepository.signOut()
        settingsManager.resetToDefaults()
        _isLoggedOut.value = true
    }


    fun updateProfile(newName: String, newUsername: String, newBio: String) {
        val uid = getCurrentUid() ?: return

        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(name = newName, username = newUsername, bio = newBio)
        }

        viewModelScope.launch {
            val result = userRepository.updateProfile(uid, newName, newUsername, newBio)
            result.onSuccess {
                val currentImage = _userData.value?.profileImageUrl ?: ""
                postRepository.syncUserPosts(uid, newName, currentImage)
                reviewRepository.syncUserReviews(uid, newName, currentImage)
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun uploadProfileImage(imageUri: Uri, assetManager: AssetManager) {
        val uid = getCurrentUid() ?: return
        _loadingState.value = LoadingState(true, "Updating profile...")

        val currentUser = _userData.value
        if (currentUser != null) {
            _userData.value = currentUser.copy(profileImageUrl = imageUri.toString())
        }

        viewModelScope.launch {
            val uriString = imageUri.toString()
            val isAsset = uriString.contains("android_asset/") || !uriString.contains("://")

            val result = if (isAsset) {
                try {
                    val cleanPath = when {
                        uriString.contains("android_asset/") -> uriString.substringAfter("android_asset/")
                        uriString.startsWith("avatars/") -> uriString
                        else -> "avatars/$uriString"
                    }.trimStart('/')

                    val inputStream: InputStream = assetManager.open(cleanPath)
                    userRepository.uploadProfileImageStream(uid, inputStream)
                } catch (e: Exception) {
                    userRepository.uploadProfileImage(uid, imageUri)
                }
            } else {
                userRepository.uploadProfileImage(uid, imageUri)
            }

            result.onSuccess { imageUrl ->
                val freshName = _userData.value?.name ?: ""
                postRepository.syncUserPosts(uid, freshName, imageUrl)
                _loadingState.value = LoadingState(false)
            }.onFailure {
                _error.value = it.message
                _loadingState.value = LoadingState(false)
            }
        }
    }


    fun sendPasswordResetEmail() {
        val email = _userData.value?.email ?: return
        _loadingState.value = LoadingState(true, "Sending reset link...")
        viewModelScope.launch {
            val result = userRepository.sendPasswordResetEmail(email)
            _loadingState.value = LoadingState(false)
            result.onSuccess {
                _passwordResetSent.value = true
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun resetPasswordStatus() {
        _passwordResetSent.value = false
    }

    fun loadAvatars(assetManager: AssetManager) {
        if (_avatarPaths.value != null) return
        viewModelScope.launch {
            val avatars = mutableListOf<String>()
            scanAssets(assetManager, "avatars", avatars)
            _avatarPaths.value = avatars
        }
    }

    private fun scanAssets(
        assetManager: AssetManager,
        dirPath: String,
        result: MutableList<String>
    ) {
        try {
            val list = assetManager.list(dirPath) ?: return
            if (list.isEmpty()) {
                if (dirPath.contains(".")) {
                    result.add(dirPath.substringAfter("avatars/"))
                }
            } else {
                for (name in list) {
                    scanAssets(assetManager, "$dirPath/$name", result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
