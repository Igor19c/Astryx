package com.example.astryx.data.repository.firebase

import android.net.Uri
import com.example.astryx.data.repository.firebase.interfaces.IPostRepository
import com.example.astryx.data.entities.Post
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.InputStream

class FirebasePostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : IPostRepository {

    override fun fetchUserPosts(uid: String): Flow<Result<List<Post>>> = callbackFlow {
        val subscription = db.collection("posts")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { subscription.remove() }
    }

    override fun fetchPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val subscription = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { subscription.remove() }
    }

    override suspend fun savePost(post: Post): Result<String> {
        return try {
            val documentReference = db.collection("posts").document()
            val postWithId = post.copy(id = documentReference.id)
            documentReference.set(postWithId).await()
            Result.success(documentReference.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: Post): Result<Unit> {
        return try {
            if (post.id.isEmpty()) throw Exception("Post ID is required for update")
            db.collection("posts").document(post.id).set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("post_images/$postId.jpg")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPostImageStream(
        postId: String,
        inputStream: InputStream
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("post_images/$postId.jpg")
            val bytes = inputStream.use { it.readBytes() }
            val metadata = StorageMetadata.Builder()
                .setContentType("image/webp")
                .build()
            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(userId: String, post: Post): Result<Unit> {
        return try {
            db.collection("posts").document(post.id)
                .update(
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likes", FieldValue.increment(1)
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(userId: String, postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId)
                .update(
                    "likedBy",
                    FieldValue.arrayRemove(userId),
                    "likes",
                    FieldValue.increment(-1)
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncUserPosts(
        uid: String,
        newUsername: String,
        newUserImage: String
    ): Result<Unit> {
        return try {
            val postsSnapshot = db.collection("posts")
                .whereEqualTo("uid", uid)
                .get()
                .await()

            if (postsSnapshot.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            for (document in postsSnapshot.documents) {
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
