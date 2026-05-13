package com.example.astryx.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.repository.firebase.interfaces.IDestinationRepository
import com.example.astryx.data.repository.firebase.interfaces.ITripRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class TripUiState {
    object Idle : TripUiState()
    data class Loading(val message: String? = null) : TripUiState()
    data class Error(val message: String) : TripUiState()
    data class Success(val message: String? = null) : TripUiState()
}

class TripViewModel(
    private val repository: ITripRepository,
    private val destinationRepository: IDestinationRepository
) : ViewModel() {

    private val _currentTrip = MutableLiveData(Trip())
    val currentTrip: LiveData<Trip> = _currentTrip

    private val _selectedDestinations = MutableLiveData<MutableList<Destination>>(mutableListOf())
    val selectedDestinations: LiveData<MutableList<Destination>> = _selectedDestinations

    private val _userTrips = MutableLiveData<List<Trip>>()
    val userTrips: LiveData<List<Trip>> = _userTrips

    private val _isEditMode = MutableLiveData(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    private val _uiState = MutableLiveData<TripUiState>(TripUiState.Idle)
    val uiState: LiveData<TripUiState> = _uiState

    private var fetchJob: Job? = null

    fun resetUiState() {
        _uiState.value = TripUiState.Idle
    }


    fun setEditMode(isEditing: Boolean) {
        _isEditMode.value = isEditing

        if (isEditing) {
            val trip = _currentTrip.value
            if (trip?.status == "PLANNED") {
                _currentTrip.value = trip.copy(status = "DRAFT")
                _uiState.value =
                    TripUiState.Success("Trip status updated to Draft")
            }
        }
    }

    fun setCurrentTrip(trip: Trip) {
        if (_currentTrip.value?.id != trip.id) {
            _selectedDestinations.value = trip.destinationIds.map { id ->
                Destination(id = id)
            }.toMutableList()
        }

        if (trip.status != "COMPLETED")
            _currentTrip.value = trip

        fetchTripDestinations(trip)
    }

    fun createNewTrip() {
        _currentTrip.value = Trip()
        _selectedDestinations.value = mutableListOf()
        setEditMode(true)
    }

    fun addDestinationToTrip(trip: Trip, destination: Destination) {
        if (trip.destinationIds.contains(destination.id)) {
            _uiState.value = TripUiState.Error("${destination.title} is already in ${trip.title}!")
            return
        }
        if (_currentTrip.value?.id != trip.id) setCurrentTrip(trip)
        addDestination(destination)
    }

    fun addDestination(destination: Destination) {
        fetchJob?.cancel()
        val currentList = _selectedDestinations.value ?: mutableListOf()

        if (currentList.none { it.id == destination.id }) {
            currentList.add(destination)
            _selectedDestinations.value = currentList
            updateTripObject(currentList)
            syncTripWithFirebase()
            _uiState.value = TripUiState.Success("${destination.title} added")
        }
    }

    fun removeDestination(destinationId: String) {
        fetchJob?.cancel()
        val currentList = _selectedDestinations.value ?: mutableListOf()
        val destinationName = currentList.find { it.id == destinationId }?.title
        currentList.removeIf { it.id == destinationId }
        _selectedDestinations.value = currentList
        updateTripObject(currentList)
        syncTripWithFirebase()
        _uiState.value = TripUiState.Success("$destinationName removed")
    }

    fun updateTripDetails(
        title: String,
        startDate: Long?,
        endDate: Long?,
        imageUrl: String? = null
    ) {
        val trip = _currentTrip.value ?: return
        val newStatus = if (trip.isPlanned) "DRAFT" else trip.status
        val updatedTrip = trip.copy(
            title = title,
            startDate = startDate,
            endDate = endDate,
            status = newStatus,
            imageUrl = imageUrl ?: trip.imageUrl
        )

        setCurrentTrip(updatedTrip)
    }

    private fun updateTripObject(destinations: List<Destination>) {
        val trip = _currentTrip.value ?: Trip()
        val oldFirstId = if (trip.destinationIds.isNotEmpty()) trip.destinationIds[0] else null
        val newFirstId = if (destinations.isNotEmpty()) destinations[0].id else null
        val updatedDestinationImages = destinations.mapNotNull { dest ->
            dest.imageUrl.ifEmpty {
                val existingIndex = trip.destinationIds.indexOf(dest.id)
                if (existingIndex != -1 && existingIndex < trip.destinationImages.size) {
                    trip.destinationImages[existingIndex]
                } else {
                    null
                }
            }
        }

        val updatedTrip = trip.copy(
            destinationIds = destinations.map { it.id },
            destinationImages = updatedDestinationImages
        )
        when {
            newFirstId == null -> _currentTrip.value = updatedTrip.copy(imageUrl = "")

            newFirstId != oldFirstId || updatedTrip.imageUrl.startsWith("avatars/") || updatedTrip.imageUrl.isEmpty() -> {
                val firstDest = destinations[0]
                val tripWithInitialImage = updatedTrip.copy(imageUrl = firstDest.imageUrl)
                _currentTrip.value = tripWithInitialImage
                if (firstDest.parentId != "root")
                    tryRefineTripCover(firstDest.parentId, newFirstId)
            }

            else -> _currentTrip.value = updatedTrip

        }
    }

    private fun tryRefineTripCover(parentId: String, expectedFirstId: String) {
        viewModelScope.launch {
            try {
                destinationRepository.fetchDestinationsByIds(listOf(parentId)).first()
                    .onSuccess { parents ->
                        if (parents.isNotEmpty()) {
                            val current = _currentTrip.value ?: return@onSuccess

                            if (parents.isNotEmpty() && current.destinationIds.firstOrNull() == expectedFirstId) {
                                _currentTrip.value =
                                    current.copy(imageUrl = parents[0].imageUrl)
                                syncTripWithFirebase()
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = TripUiState.Error(e.message ?: " ")
            }
        }
    }

    fun finalizeTrip() {
        _currentTrip.value = _currentTrip.value?.copy(status = "PLANNED")
        setEditMode(false)
        syncTripWithFirebase()
    }

    fun saveTrip(
        uid: String,
        title: String,
        subtitle: String,
        startDate: Long?,
        endDate: Long?
    ) {
        val trip = _currentTrip.value?.copy(
            uid = uid,
            title = title,
            subtitle = subtitle,
            startDate = startDate,
            endDate = endDate,
            status = "DRAFT",
            createdAt = System.currentTimeMillis()
        ) ?: return

        _currentTrip.value = trip

        viewModelScope.launch {
            _uiState.value = TripUiState.Loading("Creating your journey...")
            val result = repository.saveTrip(trip)

            result.onSuccess { newId ->
                _currentTrip.value = trip.copy(id = newId)
                _uiState.value = TripUiState.Success("Trip '$title' created!")
            }.onFailure { e ->
                _uiState.value = TripUiState.Error(e.message ?: "Failed to create trip")
            }
        }
    }

    fun syncTripWithFirebase() {
        val trip = _currentTrip.value ?: return
        if (trip.id.isEmpty()) return

        viewModelScope.launch {
            repository.updateTrip(trip)
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _uiState.value = TripUiState.Loading("Deleting trip...")
            repository.deleteTrip(tripId).onSuccess {
                if (_currentTrip.value?.id == tripId) {
                    createNewTrip()
                    _isEditMode.value = false
                }
                _uiState.value = TripUiState.Success("Trip deleted successfully")
            }.onFailure { e ->
                _uiState.value = TripUiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun fetchUserTrips(uid: String) {
        viewModelScope.launch {
            repository.fetchUserTrips(uid).collectLatest { result ->
                result.onSuccess { trips ->
                    _userTrips.value = trips
                }.onFailure { e ->
                    _uiState.value = TripUiState.Error(e.message ?: "Failed to load trips")
                }
            }
        }
    }

    fun fetchTripDestinations(trip: Trip) {
        fetchJob?.cancel()
        if (trip.destinationIds.isEmpty()) {
            _selectedDestinations.value = mutableListOf()
            return
        }

        fetchJob = viewModelScope.launch {
            destinationRepository.fetchDestinationsByIds(trip.destinationIds)
                .collectLatest { result ->
                    result.onSuccess { destinations ->
                        _selectedDestinations.value = destinations.toMutableList()
                    }.onFailure { e ->
                        _uiState.value =
                            TripUiState.Error(e.message ?: "Failed to load destinations")
                        val basicList = trip.destinationIds.mapIndexed { index, destId ->
                            Destination(
                                id = destId
                            )
                        }.toMutableList()
                        _selectedDestinations.value = basicList
                    }
                }
        }
    }
}
