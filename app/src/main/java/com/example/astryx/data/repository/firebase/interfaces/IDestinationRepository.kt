package com.example.astryx.data.repository.firebase.interfaces

import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.DestinationDetails
import kotlinx.coroutines.flow.Flow

interface IDestinationRepository {
    suspend fun getDestinationDetails(destinationId: String): Result<DestinationDetails?>

    suspend fun saveDestination(userId: String, destination: Destination): Result<Unit>

    suspend fun unsaveDestination(userId: String, destinationId: String): Result<Unit>

    fun fetchSavedDestinations(userId: String): Flow<Result<List<Destination>>>

    fun fetchDestinationsByParent(parentId: String): Flow<Result<List<Destination>>>

    fun fetchDestinationsByRating(): Flow<Result<List<Destination>>>

    fun fetchAllDestinations(): Flow<Result<List<Destination>>>

    fun fetchDestinationsByIds(destinationIds: List<String>): Flow<Result<List<Destination>>>
}
