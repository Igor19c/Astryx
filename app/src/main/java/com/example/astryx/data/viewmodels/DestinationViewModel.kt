package com.example.astryx.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.DestinationDetails
import com.example.astryx.data.entities.Review
import com.example.astryx.data.entities.User
import com.example.astryx.data.repository.firebase.interfaces.IDestinationRepository
import com.example.astryx.data.repository.firebase.interfaces.IReviewRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class DestinationUiState {
    object Idle : DestinationUiState()
    data class Loading(val message: String? = null) : DestinationUiState()
    data class Error(val message: String) : DestinationUiState()
    data class Success(val message: String? = null) : DestinationUiState()
}

data class ReviewSummary(
    val recentReviews: List<Review>,
    val averageRating: Double,
    val totalCount: Int
)

class DestinationViewModel(
    private val repository: IDestinationRepository,
    private val reviewRepository: IReviewRepository
) : ViewModel() {

//    init {
//        viewModelScope.launch {
//            reviewRepository.syncAllDestinations()
//        }
//    }

    private var allDestinationsList: List<Destination> = emptyList()

    private val _destinations = MutableLiveData<List<Destination>>()
    val destinations: LiveData<List<Destination>> = _destinations

    private val _filteredDestinations = MutableLiveData<List<Destination>>()
    val filteredDestinations: LiveData<List<Destination>> = _filteredDestinations

    private val _subDestinations = MutableLiveData<List<Destination>>()
    val subDestinations: LiveData<List<Destination>> = _subDestinations

    private val _trendingDestinations = MutableLiveData<List<Destination>>()
    val trendingDestinations: LiveData<List<Destination>> = _trendingDestinations

    private val _homeReviewSummary = MutableLiveData<ReviewSummary>()
    val homeReviewSummary: LiveData<ReviewSummary> = _homeReviewSummary

    private val _destReviewSummary = MutableLiveData<ReviewSummary>()
    val destReviewSummary: LiveData<ReviewSummary> = _destReviewSummary

    private val _savedDestinations = MutableLiveData<List<Destination>>()
    val savedDestinations: LiveData<List<Destination>> = _savedDestinations

    private val _destinationDetails = MutableLiveData<DestinationDetails?>()
    val destinationDetails: LiveData<DestinationDetails?> = _destinationDetails

    private val _uiState = MutableLiveData<DestinationUiState>(DestinationUiState.Idle)
    val uiState: LiveData<DestinationUiState> = _uiState

    private val _saveStatus = MutableLiveData<Pair<Boolean, String>?>()
    val saveStatus: LiveData<Pair<Boolean, String>?> = _saveStatus


    fun fetchDestinations(parentId: String) {
        viewModelScope.launch {
            _uiState.value = DestinationUiState.Loading("Fetching destinations...")
            repository.fetchDestinationsByParent(parentId).collectLatest { result ->
                result.onSuccess { list ->
                    _destinations.value = list
                    _uiState.value = DestinationUiState.Idle
                }.onFailure { e ->
                    _uiState.value =
                        DestinationUiState.Error(e.message ?: "Failed to fetch destinations")

                }
            }
        }
    }

    fun filterDestinations(query: String, type: String, difficulties: List<Int>) {
        val filtered = allDestinationsList.filter { dest ->
            val matchesQuery = query.isEmpty() || dest.title.contains(query, ignoreCase = true)
            val matchesType =
                type == "All" || dest.type.trim().equals(type.trim(), ignoreCase = true)
            val matchesDifficulty = difficulties.isEmpty() || difficulties.contains(dest.difficulty)
            matchesQuery && matchesType && matchesDifficulty
        }
        _filteredDestinations.value = filtered
    }

    fun fetchSubDestinations(parentId: String) {
        viewModelScope.launch {
            repository.fetchDestinationsByParent(parentId).collectLatest { result ->
                result.onSuccess { list ->
                    _subDestinations.value = list
                }.onFailure { e ->
                    _uiState.value =
                        DestinationUiState.Error(e.message ?: "Failed to fetch destinations")
                }
            }
        }
    }

    fun fetchHomeReviews() {
        viewModelScope.launch {
            reviewRepository.fetchAllReviewsOrderedByCreatedAt().collectLatest { result ->
                result.onSuccess { reviews ->
                    _homeReviewSummary.value = createReviewSummary(reviews)
                }.onFailure { error ->
                    _uiState.value =
                        DestinationUiState.Error(error.message ?: "Failed to fetch reviews")
                }
            }
        }
    }

    fun fetchDestinationReviews(destId: String) {
        viewModelScope.launch {
            reviewRepository.fetchDestinationReviews(destId).collectLatest { result ->
                result.onSuccess { reviews ->
                    _destReviewSummary.value = createReviewSummary(reviews)
                }.onFailure { error ->
                    _uiState.value =
                        DestinationUiState.Error(error.message ?: "Failed to fetch reviews")
                }
            }
        }
    }

    private fun createReviewSummary(reviews: List<Review>): ReviewSummary {
        val count = reviews.size
        val avg = if (count > 0) reviews.sumOf { it.rating.toDouble() } / count else 0.0
        return ReviewSummary(
            recentReviews = reviews.take(5),
            averageRating = avg,
            totalCount = count
        )
    }

    fun submitReview(destination: Destination, rating: Int, comment: String, user: User) {
        val review = Review(
            destinationId = destination.id,
            destinationTitle = destination.title,
            uid = user.uid,
            username = user.name,
            userProfileImage = user.profileImageUrl,
            rating = rating,
            comment = comment
        )

        viewModelScope.launch {
            _uiState.value = DestinationUiState.Loading("Sharing your feedback...")
            val result = reviewRepository.addReviewAndUpdateRating(review)
            result.onSuccess {
                _uiState.value = DestinationUiState.Success("Review shared successfully!")
            }.onFailure { e ->
                _uiState.value = DestinationUiState.Error(e.message ?: "Failed to post review")
            }
        }
    }

    fun fetchDestinationsByRating() {
        viewModelScope.launch {
            _uiState.value = DestinationUiState.Loading("Fetching destinations...")
            repository.fetchDestinationsByRating().collectLatest { result ->
                _uiState.value = DestinationUiState.Idle
                result.onSuccess { list ->
                    _trendingDestinations.value = list
                }.onFailure { e ->
                    _uiState.value =
                        DestinationUiState.Error(e.message ?: "Failed to fetch destinations")
                }
            }
        }
    }

    fun fetchAllDestinationsForSearch() {
        viewModelScope.launch {
            _uiState.value = DestinationUiState.Loading("Scanning the galaxy...")
            repository.fetchAllDestinations().collectLatest { result ->
                result.onSuccess { list ->
                    allDestinationsList = list
                    _filteredDestinations.value = list
                    _uiState.value = DestinationUiState.Idle
                }.onFailure { e ->
                    _uiState.value = DestinationUiState.Error(e.message ?: "Failed to scan")
                }
            }
        }
    }

    fun fetchFullDetails(destinationId: String) {
        viewModelScope.launch {
            _uiState.value = DestinationUiState.Loading()
            val result = repository.getDestinationDetails(destinationId)
            result.onSuccess { details ->
                _destinationDetails.value = details
                _uiState.value = DestinationUiState.Idle
            }.onFailure { e ->
                _uiState.value = DestinationUiState.Error(e.message ?: "Failed to get details")
            }
        }
    }

    fun fetchSavedDestinations(userId: String) {
        viewModelScope.launch {
            repository.fetchSavedDestinations(userId).collectLatest { result ->
                result.onSuccess { _savedDestinations.value = it }
                result.onFailure {
                    _uiState.value =
                        DestinationUiState.Error(it.message ?: "Error fetching saved destinations")
                }
            }
        }
    }

    fun toggleSaveDestination(userId: String, destination: Destination) {
        viewModelScope.launch {
            val isCurrentlySaved = _savedDestinations.value?.any { it.id == destination.id } == true
            if (isCurrentlySaved) {
                repository.unsaveDestination(userId, destination.id).onSuccess {
                    _saveStatus.postValue(false to "${destination.title} removed from favorites")
                }
            } else {
                repository.saveDestination(userId, destination).onSuccess {
                    _saveStatus.postValue(true to "${destination.title} saved to favorites")
                }
            }
        }
    }

    fun unsaveDestination(userId: String, destinationId: String) {
        viewModelScope.launch {
            repository.unsaveDestination(userId, destinationId)
        }
    }

    fun unsaveAllDestinations(userId: String) {
        viewModelScope.launch {
            _savedDestinations.value?.forEach { destination ->
                repository.unsaveDestination(userId, destination.id)
            }
        }
    }

    fun resetUiState() {
        _uiState.value = DestinationUiState.Idle
    }

    fun resetSaveStatus() {
        _saveStatus.value = null
    }

}
