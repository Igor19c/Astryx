package com.example.astryx.data.repository.firebase

import com.example.astryx.data.repository.firebase.interfaces.ITripRepository
import com.example.astryx.data.entities.Trip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseTripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ITripRepository {

    override fun fetchUserTrips(uid: String): Flow<Result<List<Trip>>> = callbackFlow {
        val subscription = db.collection("trips")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val trips = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Trip::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(trips))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun saveTrip(trip: Trip): Result<String> {
        return try {
            val documentReference = db.collection("trips").add(trip).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrip(trip: Trip): Result<Unit> {
        return try {
            if (trip.id.isEmpty()) throw Exception("Trip ID is required for update")
            db.collection("trips").document(trip.id).set(trip).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            db.collection("trips").document(tripId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
