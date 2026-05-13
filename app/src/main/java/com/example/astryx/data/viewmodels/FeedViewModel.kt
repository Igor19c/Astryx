package com.example.astryx.data.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.astryx.data.entities.Post
import com.example.astryx.data.entities.User
import com.example.astryx.data.repository.firebase.interfaces.IPostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID

sealed class FeedUiState {
    object Idle : FeedUiState()
    data class Loading(val message: String? = null) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
    data class Success(val message: String? = null) : FeedUiState()
}

class FeedViewModel(
    private val repository: IPostRepository
) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _validationError = MutableLiveData<String?>(null)
    val validationError: LiveData<String?> = _validationError

    private val _postSaved = MutableLiveData<Boolean>(false)
    val postSaved: LiveData<Boolean> = _postSaved

    private val _postDeleted = MutableLiveData<Boolean>(false)
    val postDeleted: LiveData<Boolean> = _postDeleted

//    private val _uiState = MutableLiveData<FeedUiState>(FeedUiState.Idle)
//    val uiState: LiveData<FeedUiState> = _uiState

    private val _selectedImageUri = MutableLiveData<Uri?>(null)
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private var postsJob: Job? = null

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        postsJob?.cancel()
        postsJob = viewModelScope.launch {
            _isLoading.value = true
            repository.fetchPosts().collect { result ->
                _isLoading.value = false
                result.onSuccess { _posts.value = it }
                result.onFailure { _error.value = it.message }
            }
        }
    }

    fun fetchUserPosts(uid: String) {
        viewModelScope.launch {
            repository.fetchUserPosts(uid).collectLatest { result ->
                result.onSuccess { posts ->
                    _userPosts.value = posts
                }.onFailure { e ->
                    _error.value = e.message
                }
            }
        }
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    private fun validatePost(title: String, rating: Int): Boolean {
        var isValid = true

        if (title.isBlank()) {
            _validationError.value = "Title is required"
            isValid = false
        } else {
            _validationError.value = null
        }

        if (rating == 0) {
            _validationError.value = "Please provide a rating"
            isValid = false
        } else {
            _validationError.value = null
        }

        return isValid
    }

    private suspend fun handleImageUpload(
        postId: String,
        stream: InputStream?,
        uri: Uri?
    ): Result<String>? {
        return when {
            stream != null -> repository.uploadPostImageStream(postId, stream)
            uri != null -> repository.uploadPostImage(postId, uri)
            else -> null
        }
    }

    fun savePost(
        user: User,
        title: String,
        description: String,
        rating: Int,
        imageInputStream: InputStream? = null,
        localImageUri: Uri? = null,
        tripImageUrl: String = ""
    ) {
        if (!validatePost(title, rating)) return
        viewModelScope.launch {
            _error.value = null
            _postSaved.value = false
            _isLoading.value = true

            var finalImageUrl = tripImageUrl
            val uploadResult =
                handleImageUpload(UUID.randomUUID().toString(), imageInputStream, localImageUri)

            uploadResult?.onSuccess { url ->
                finalImageUrl = url
            }?.onFailure {
                _error.value = "Failed to upload image: ${it.message}"
                _isLoading.value = false
                return@launch
            }

            val newPost = Post(
                uid = user.uid,
                username = user.name,
                userProfileImage = user.profileImageUrl,
                title = title,
                description = description,
                rating = rating,
                imageUrl = finalImageUrl,
                createdAt = System.currentTimeMillis()
            )

            val result = repository.savePost(newPost)
            _isLoading.value = false

            result.onSuccess {
                _postSaved.value = true
                _selectedImageUri.value = null
            }.onFailure {
                _error.value = it.message
            }

        }
    }

    fun updatePost(
        existingPost: Post,
        newTitle: String,
        newDescription: String,
        newRating: Int,
        imageInputStream: InputStream? = null, localImageUri: Uri? = null
    ) {
        if (!validatePost(newTitle, newRating)) return

        viewModelScope.launch {
            _error.value = null
            _postSaved.value = false
            _isLoading.value = true

            var finalImageUrl = existingPost.imageUrl

            val uploadResult = when {
                imageInputStream != null -> {
                    repository.uploadPostImageStream(existingPost.id, imageInputStream)
                }

                localImageUri != null -> {
                    repository.uploadPostImage(existingPost.id, localImageUri)
                }

                else -> null
            }

            uploadResult?.onSuccess { url ->
                finalImageUrl = url
            }?.onFailure {
                _error.value = "Failed to upload image: ${it.message}"
                _isLoading.value = false
                return@launch
            }

            val updatedPost = existingPost.copy(
                title = newTitle,
                description = newDescription,
                rating = newRating,
                imageUrl = finalImageUrl
            )

            val result = repository.updatePost(updatedPost)
            _isLoading.value = false

            result.onSuccess {
                _postSaved.value = true
                _selectedImageUri.value = null
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun toggleLikePost(userId: String, post: Post) {
        viewModelScope.launch {
            val isCurrentlyLiked = post.likedBy.contains(userId)
            if (isCurrentlyLiked) {
                repository.unlikePost(userId, post.id)
            } else {
                repository.likePost(userId, post)
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deletePost(post.id)
            _isLoading.value = true
            result.onSuccess {
                _postDeleted.value = true
            }.onFailure {
                _error.value = it.message
            }
        }
    }

    fun resetPostSavedState() {
        _postSaved.value = false
    }

    fun resetPostDeletedState() {
        _postDeleted.value = false
    }

    fun resetError() {
        _error.value = null
        _validationError.value = null
    }
}
