package com.example.astryx.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.astryx.AstryxApplication

class ViewModelFactory(private val application: AstryxApplication) :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val container = application.appContainer

        return when {
            modelClass.isAssignableFrom(TripViewModel::class.java) -> {
                TripViewModel(container.tripRepository, container.destinationRepository) as T
            }

            modelClass.isAssignableFrom(FeedViewModel::class.java) -> {
                FeedViewModel(container.postRepository) as T
            }

            modelClass.isAssignableFrom(UserViewModel::class.java) -> {
                UserViewModel(
                    container.userRepository,
                    container.reviewRepository,
                    container.postRepository,
                    container.tripRepository,
                    container.settingsManager
                ) as T
            }

            modelClass.isAssignableFrom(DestinationViewModel::class.java) -> {
                DestinationViewModel(
                    container.destinationRepository,
                    container.reviewRepository
                ) as T
            }

            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(container.userRepository, container.settingsManager) as T
            }

            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> {
                ExploreViewModel() as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
