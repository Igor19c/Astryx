package com.example.astryx.data.entities

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class Trip(
    val id: String = "",
    val uid: String = "",
    val title: String = "",
    val subtitle: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val destinationIds: List<String> = emptyList(),
    val destinationImages: List<String> = emptyList(),
    val status: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long? = null,
) : Serializable {

    val isDraft: Boolean get() = status == "DRAFT" || destinationIds.isEmpty()
    val isPlanned: Boolean get() = status == "PLANNED"
    
    val isPastTrip: Boolean get() {
        val start = startDate ?: return false
        return start < System.currentTimeMillis()
    }

    val isCompleted: Boolean get() {
        return isPlanned && destinationIds.isNotEmpty() && isPastTrip
    }

    val canBePublished: Boolean get() {
        return destinationIds.isNotEmpty() && !isPastTrip
    }

    fun getFormattedStartDate(): String? {
        val date = startDate ?: return null
        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(date))
    }

    fun getFormattedEndDate(): String? {
        val date = endDate ?: return null
        return SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(date))
    }
}
