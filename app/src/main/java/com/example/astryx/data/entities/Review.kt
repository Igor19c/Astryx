package com.example.astryx.data.entities

import java.io.Serializable

data class Review(
    val id: String = "",
    val destinationId: String = "",
    val destinationTitle: String = "", // השדה החדש
    val uid: String = "",
    val username: String = "",
    val userProfileImage: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: Long? = System.currentTimeMillis()
) : Serializable