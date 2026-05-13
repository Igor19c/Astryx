package com.example.astryx.data.entities

import java.io.Serializable

data class FAQ(
    val id: String,
    val question: String,
    val answer: String
) : Serializable