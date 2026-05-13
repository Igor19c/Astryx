package com.example.astryx.data.entities

import java.io.Serializable

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val assetPath: String
) : Serializable