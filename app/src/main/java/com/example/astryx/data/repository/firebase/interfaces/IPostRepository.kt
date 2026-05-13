package com.example.astryx.data.repository.firebase.interfaces

import android.net.Uri
import com.example.astryx.data.entities.Post
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface IPostRepository {
    fun fetchPosts(): Flow<Result<List<Post>>>
    fun fetchUserPosts(uid: String): Flow<Result<List<Post>>>
    suspend fun savePost(post: Post): Result<String>
    suspend fun updatePost(post: Post): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun likePost(userId: String, post: Post): Result<Unit>
    suspend fun unlikePost(userId: String, postId: String): Result<Unit>
    
    suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String>
    suspend fun uploadPostImageStream(postId: String, inputStream: InputStream): Result<String>

    suspend fun syncUserPosts(uid: String, newUsername: String, newUserImage: String): Result<Unit>
}
