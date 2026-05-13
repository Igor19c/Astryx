package com.example.astryx.data.repository.firebase.interfaces

import com.example.astryx.data.entities.Trip
import kotlinx.coroutines.flow.Flow

interface ITripRepository {
    /**
     * Fetches user trips in real-time.
     */
    fun fetchUserTrips(uid: String): Flow<Result<List<Trip>>>

    /**
     * Saves a new trip and returns its ID.
     */
    suspend fun saveTrip(trip: Trip): Result<String>

    /**
     * Updates an existing trip.
     */
    suspend fun updateTrip(trip: Trip): Result<Unit>

    /**
     * Deletes a trip by ID.
     */
    suspend fun deleteTrip(tripId: String): Result<Unit>
}
