package com.example.astryx.data.repository

import com.example.astryx.data.entities.Badge

object BadgeRepository {
    private val allBadges = listOf(
        Badge(
            id = "first_trip",
            name = "First Trip",
            description = "Completed your first trip!",
            assetPath = "badges/first_trip.webp"
        ),
        Badge(
            id = "explorer",
            name = "Explorer",
            description = "Visited 5 different planets",
            assetPath = "badges/explorer.webp"
        ),
        Badge(
            id = "first_review",
            name = "First Review",
            description = "Wrote 1 helpful review",
            assetPath = "badges/first_review.webp"
        ),
        Badge(
            id = "top_reviewer",
            name = "Top Reviewer",
            description = "Wrote 5 helpful reviews",
            assetPath = "badges/top_reviewer.webp"
        ),
        Badge(
            id = "first_post",
            name = "First Post",
            description = "Posted 1 trip",
            assetPath = "badges/first_post.webp"
        ),
        Badge(
            id = "top_content_creator",
            name = "Top Content Creator",
            description = "Posted 5 trip",
            assetPath = "badges/top_content_creator.webp"
        )
    )

    fun getAllBadges(): List<Badge> = allBadges

    fun getBadgeById(id: String): Badge? = allBadges.find { it.id == id }

}