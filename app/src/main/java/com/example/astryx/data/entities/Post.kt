package com.example.astryx.data.entities

import java.io.Serializable

data class Post(
    val id: String = "",
    val uid: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val title: String = "",
    val description: String = "",
    val rating: Int = 0,
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    val createdAt: Long? = System.currentTimeMillis()

) : Serializable