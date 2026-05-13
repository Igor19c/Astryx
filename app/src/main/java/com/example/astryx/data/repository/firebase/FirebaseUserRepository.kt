package com.example.astryx.data.repository.firebase

import android.net.Uri
import com.example.astryx.data.repository.firebase.interfaces.IUserRepository
import com.example.astryx.data.entities.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class FirebaseUserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : IUserRepository {

    override fun getCurrentUid(): String? = auth.currentUser?.uid
    override fun getCurrentUserName(): String? = auth.currentUser?.displayName

    override fun getFirebaseUserProperties(): Pair<String?, String?> {
        val user = auth.currentUser
        return Pair(user?.displayName, user?.email)
    }

    override fun observeUser(uid: String): Flow<Result<User?>> = callbackFlow {
        val subscription = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(Result.success(user))
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        uid: String,
        name: String,
        username: String,
        bio: String
    ): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "username" to username,
                "bio" to bio
            )
            db.collection("users").document(uid).update(updates).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUserBadges(uid: String, badgeIds: List<String>): Result<Unit> {
        return try {
            db.collection("users").document(uid).update("badges", badgeIds).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_images/$uid.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.collection("users").document(uid).update("profileImageUrl", downloadUrl).await()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadProfileImageStream(
        uid: String,
        inputStream: InputStream
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_images/$uid.jpg")
            val bytes = inputStream.use { it.readBytes() }
            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .build()
            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            db.collection("users").document(uid).update("profileImageUrl", downloadUrl).await()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Result<Unit> {
        return try {
            auth.currentUser?.reload()?.await()
            if (auth.currentUser == null) {
                Result.failure(Exception("User no longer exists"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}
