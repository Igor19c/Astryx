package com.example.astryx.data.repository.firebase

import com.example.astryx.data.repository.firebase.interfaces.IDestinationRepository
import com.example.astryx.data.entities.Destination
import com.example.astryx.data.entities.DestinationDetails
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDestinationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IDestinationRepository {

    override suspend fun getDestinationDetails(destinationId: String): Result<DestinationDetails?> {
        return try {
            val document = db.collection("destinations")
                .document(destinationId)
                .collection("details")
                .document("main")
                .get()
                .await()

            if (document != null && document.exists()) {
                Result.success(document.toObject(DestinationDetails::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDestination(userId: String, destination: Destination): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("favoriteDestinations", FieldValue.arrayUnion(destination.id))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsaveDestination(userId: String, destinationId: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("favoriteDestinations", FieldValue.arrayRemove(destinationId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun fetchAllDestinations(): Flow<Result<List<Destination>>> = callbackFlow {
        val subscription = db.collection("destinations")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val destinations = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Destination::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(destinations))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun fetchSavedDestinations(userId: String): Flow<Result<List<Destination>>> =
        callbackFlow {
            val userDocRef = db.collection("users").document(userId)

            val subscription = userDocRef.addSnapshotListener { userSnapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (userSnapshot == null || !userSnapshot.exists()) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val destinationIds = userSnapshot.get("favoriteDestinations") as? List<String>

                if (destinationIds.isNullOrEmpty()) {
                    trySend(Result.success(emptyList()))
                } else {
                    db.collection("destinations")
                        .whereIn(FieldPath.documentId(), destinationIds)
                        .get()
                        .addOnSuccessListener { destinationSnapshot ->
                            val destinations = destinationSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Destination::class.java)?.copy(id = doc.id)
                            }
                            trySend(Result.success(destinations))
                        }
                        .addOnFailureListener { e ->
                            trySend(Result.failure(e))
                        }
                }
            }
            awaitClose { subscription.remove() }
        }

    override fun fetchDestinationsByParent(parentId: String): Flow<Result<List<Destination>>> =
        callbackFlow {
            val subscription = db.collection("destinations")
                .whereEqualTo("parentId", parentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val destinations = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Destination::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.success(destinations))
                    }
                }
            awaitClose { subscription.remove() }
        }

    override fun fetchDestinationsByRating(): Flow<Result<List<Destination>>> =
        callbackFlow {
            val subscription = db.collection("destinations")
                .orderBy("ratingAvg", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val destinations = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Destination::class.java)?.copy(id = doc.id)
                        }
                        trySend(Result.success(destinations))
                    }
                }
            awaitClose { subscription.remove() }
        }

    override fun fetchDestinationsByIds(destinationIds: List<String>): Flow<Result<List<Destination>>> =
        callbackFlow {
            if (destinationIds.isEmpty()) {
                trySend(Result.success(emptyList()))
                close()
                return@callbackFlow
            }

            val chunks = destinationIds.chunked(30)
            val allDestinations = mutableListOf<Destination>()
            var completedChunks = 0

            chunks.forEach { chunk ->
                db.collection("destinations")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val destinations = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Destination::class.java)?.copy(id = doc.id)
                        }
                        allDestinations.addAll(destinations)
                        completedChunks++

                        if (completedChunks == chunks.size) {
                            val sortedList = destinationIds.mapNotNull { id ->
                                allDestinations.find { it.id == id }
                            }
                            trySend(Result.success(sortedList))
                        }
                    }
                    .addOnFailureListener { e ->
                        trySend(Result.failure(e))
                    }
            }
            awaitClose { }
        }
}
