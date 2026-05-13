package com.example.astryx.data.utils

import com.example.astryx.data.entities.Post
import com.example.astryx.data.entities.Review
import com.example.astryx.data.entities.Trip
import com.example.astryx.data.entities.User

object BadgeManager {

    fun checkNewBadges(
        user: User,
        trips: List<Trip>,
        posts: List<Post>,
        reviews: List<Review>
    ): List<String> {
        val currentBadgeIds = user.badges.toSet()
        val newBadges = mutableListOf<String>()

        // 1. First Trip Badge
        if (!currentBadgeIds.contains("first_trip") && trips.any { it.isCompleted }) {
            newBadges.add("first_trip")
        }

        // 2. Explorer Badge (Visited 5 or more unique destinations)
        val allDestinationIds = trips.flatMap { it.destinationIds }.toSet()
        if (!currentBadgeIds.contains("explorer") && allDestinationIds.size >= 5) {
            newBadges.add("explorer")
        }

        // 3. First Review Badge
        if (!currentBadgeIds.contains("first_review") && reviews.isNotEmpty()) {
            newBadges.add("first_review")
        }

        // 4. Top Reviewer Badge (Wrote 5 or more reviews)
        if (!currentBadgeIds.contains("top_reviewer") && reviews.size >= 5) {
            newBadges.add("top_reviewer")
        }

        // 5. First Post Badge (Feed posts)
        if (!currentBadgeIds.contains("first_post") && posts.isNotEmpty()) {
            newBadges.add("first_post")
        }

        // 6. Top Content Creator Badge (5 posts)
        if (!currentBadgeIds.contains("top_content_creator") && posts.size >= 5) {
            newBadges.add("top_content_creator")
        }

        return newBadges
    }
}