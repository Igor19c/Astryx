package com.example.astryx.data.repository.firebase.interfaces

import android.net.Uri
import com.example.astryx.data.entities.User
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface IUserRepository {
    fun getCurrentUid(): String?
    fun getCurrentUserName(): String?

    fun getFirebaseUserProperties(): Pair<String?, String?>
    fun observeUser(uid: String): Flow<Result<User?>>
    suspend fun createUserProfile(user: User): Result<Unit>
    suspend fun updateProfile(
        uid: String,
        name: String,
        username: String,
        bio: String
    ): Result<Unit>

    suspend fun updateUserBadges(uid: String, badgeIds: List<String>): Result<Unit>
    suspend fun uploadProfileImage(uid: String, imageUri: Uri): Result<String>
    suspend fun uploadProfileImageStream(uid: String, inputStream: InputStream): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun reloadUser(): Result<Unit>
    fun signOut()
}
