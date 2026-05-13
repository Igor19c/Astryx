package com.example.astryx.data.entities

import java.io.Serializable

data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val createdAt: Long? = null,
    val bio: String = "",
    val favoriteDestinations: List<String> = listOf(),
    val badges: List<String> = listOf()
) : Serializable
