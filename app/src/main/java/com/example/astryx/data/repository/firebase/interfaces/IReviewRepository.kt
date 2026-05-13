package com.example.astryx.data.repository.firebase.interfaces

import com.example.astryx.data.entities.Review
import kotlinx.coroutines.flow.Flow

interface IReviewRepository {
    suspend fun addReviewAndUpdateRating(review: Review): Result<Unit>
    suspend fun syncDestinationRating(destinationId: String): Result<Unit>
    suspend fun syncAllDestinations(): Result<Unit>
    fun fetchUserReviews(uid: String): Flow<Result<List<Review>>>
    fun fetchDestinationReviews(destinationId: String): Flow<Result<List<Review>>>
    fun fetchAllReviewsOrderedByCreatedAt(): Flow<Result<List<Review>>>
    suspend fun syncUserReviews(uid: String, newUsername: String, newUserImage: String): Result<Unit>
}