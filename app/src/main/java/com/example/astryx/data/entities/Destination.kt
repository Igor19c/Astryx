package com.example.astryx.data.entities

import com.google.firebase.firestore.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class Destination(
    val id: String = "",
    val parentId: String = "root",
    val title: String = "",
    val subtitle: String = "",
    val shortDescription: String = "",
    val type: String = "",
    val level: Int = 0,
    val priceTier: Int = 0,
    val imageUrl: String = "",
    val ratingAvg: Double = 0.0,
    val ratingCount: Int = 0,
    val childCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isActive: Boolean = true,
    val details: DestinationDetails? = null,
    val difficulty: Int = 0,
    @get:com.google.firebase.firestore.PropertyName("travelTimeFromEarth")
    @set:com.google.firebase.firestore.PropertyName("travelTimeFromEarth")
    var travelTimeFromEarth: Any? = 0,
    val distanceFromEarth: String = "",
    val safetyLevel: Int = 0
) : Serializable {

    /**
     * Helper property to safely get travelTime as Int, even if Firestore returns it as a Map or Long.
     */
    @com.google.firebase.firestore.Exclude
    fun getTravelTimeInt(): Int {
        return when (val value = travelTimeFromEarth) {
            is Number -> value.toInt()
            is Map<*, *> -> {
                // If it's the old Map format, try to extract 'value'
                (value["value"] as? Number)?.toInt() ?: 0
            }
            else -> 0
        }
    }
}

data class DestinationDetails(
    val longDescription: String = "",
    val bestTimeToVisit: String = "",
    val highlights: List<String> = emptyList(),
    val safety: Safety? = null,
    val environment: Environment? = null,
) : Serializable

data class Safety(
    val notes: List<String> = emptyList(),
    val hazards: List<Hazard> = emptyList()
) : Serializable

data class Hazard(
    val name: String = "",
    val level: Int = 0,
    val tip: String = ""
) : Serializable

data class Environment(
    val gravityG: Double = 0.0,
    val atmosphere: String = "",
    val temperatureC: Temperature? = null,
    val radiation: String = "",
    val dayLengthHours: Double = 0.0,
    val visibility: String = ""
) : Serializable

data class Temperature(
    val min: Int = 0,
    val max: Int = 0
) : Serializable
