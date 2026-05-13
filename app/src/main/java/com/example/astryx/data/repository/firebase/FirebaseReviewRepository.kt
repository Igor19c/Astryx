package com.example.astryx.data.repository.firebase

import com.example.astryx.data.repository.firebase.interfaces.IReviewRepository
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.Review
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseReviewRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IReviewRepository {

    override suspend fun addReviewAndUpdateRating(review: Review): Result<Unit> {
        return try {
            val destinationRef = db.collection("destinations").document(review.destinationId)
            val newReviewRef = db.collection("reviews").document()

            db.runTransaction { transaction ->
                val destinationSnapshot = transaction.get(destinationRef)
                val currentDestination = destinationSnapshot.toObject(Destination::class.java)
                    ?: throw Exception("Destination not found")

                val currentRatingTotal =
                    currentDestination.ratingAvg * currentDestination.ratingCount
                val newRatingCount = currentDestination.ratingCount + 1
                val newRatingAvg = (currentRatingTotal + review.rating) / newRatingCount

                transaction.set(newReviewRef, review.copy(id = newReviewRef.id))
                transaction.update(
                    destinationRef,
                    "ratingCount", newRatingCount,
                    "ratingAvg", newRatingAvg
                )
                null
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncDestinationRating(destinationId: String): Result<Unit> {
        return try {
            val reviewsSnapshot = db.collection("reviews")
                .whereEqualTo("destinationId", destinationId)
                .get()
                .await()

            val reviews = reviewsSnapshot.toObjects(Review::class.java)

            if (reviews.isEmpty()) {
                db.collection("destinations").document(destinationId)
                    .update("ratingCount", 0, "ratingAvg", 0.0).await()
                return Result.success(Unit)
            }

            val count = reviews.size
            val avg = reviews.sumOf { it.rating.toDouble() } / count

            db.collection("destinations").document(destinationId)
                .update(
                    "ratingCount", count,
                    "ratingAvg", avg
                ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAllDestinations(): Result<Unit> {
        return try {
            val destinationsSnapshot = db.collection("destinations").get().await()
            val destinations = destinationsSnapshot.documents

            for (doc in destinations) {
                syncDestinationRating(doc.id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchUserReviews(uid: String): Flow<Result<List<Review>>> = callbackFlow {
        val subscription = db.collection("reviews")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                trySend(Result.success(reviews))
            }
        awaitClose { subscription.remove() }
    }

    override fun fetchDestinationReviews(destinationId: String): Flow<Result<List<Review>>> =
        callbackFlow {
            val subscription = db.collection("reviews")
                .whereEqualTo("destinationId", destinationId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                    trySend(Result.success(reviews))
                }
            awaitClose { subscription.remove() }
        }

    override fun fetchAllReviewsOrderedByCreatedAt(): Flow<Result<List<Review>>> = callbackFlow {
        val subscription = db.collection("reviews")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val reviews = snapshot?.toObjects(Review::class.java) ?: emptyList()
                trySend(Result.success(reviews))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun syncUserReviews(
        uid: String,
        newUsername: String,
        newUserImage: String
    ): Result<Unit> {
        return try {
            val reviewsSnapshot = db.collection("reviews")
                .whereEqualTo("uid", uid)
                .get()
                .await()

            if (reviewsSnapshot.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            for (document in reviewsSnapshot.documents) {
                batch.update(
                    document.reference,
                    "username", newUsername,
                    "userProfileImage", newUserImage
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}