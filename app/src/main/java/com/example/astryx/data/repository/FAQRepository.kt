package com.example.astryx.data.repository

import com.example.astryx.data.entities.FAQ

object FAQRepository {
    val generalFaq = listOf(
        FAQ(
            id = "G1",
            question = "What can I do with Astryx?",
            answer = "You can explore space destinations and create personalized interstellar trip plans."
        ),
        FAQ(
            id = "G2",
            question = "Are the trips real or fictional?",
            answer = "All trips are conceptual and meant for educational and imaginative exploration."
        ),
        FAQ(
            id = "G3",
            question = "Is the app free?",
            answer = "Yes — all core features are available for free."
        )
    )

    val destinationExplorationFaq = listOf(
        FAQ(
            id = "D1",
            question = "Are the destinations based on real data?",
            answer = "Most destinations are inspired by real astronomical objects combined with creative interpretation."
        ),
        FAQ(
            id = "D2",
            question = "Will more destinations be added?",
            answer = "Yes — new destinations are added regularly based on updates and user feedback."
        )
    )

    val tripPlanningFaq = listOf(
        FAQ(
            id = "P1",
            question = "How do I plan a trip?",
            answer = "Select destinations you like and add them to your trip to build a custom itinerary."
        ),
        FAQ(
            id = "P2",
            question = "Can I save destinations for later?",
            answer = "Yes — you can add any destination to your favorites list."
        ),
        FAQ(
            id = "P3",
            question = "Do you support multi-stop routes?",
            answer = "Yes — you can create trips with multiple locations in any order you choose."
        ),
        FAQ(
            id = "P4",
            question = "Can I share my trip with others?",
            answer = "Yes — you can publish your trip in the Feed or share it directly."
        )
    )

    val technicalFaq = listOf(
        FAQ(
            id = "T1",
            question = "Can I customize the look of the app?",
            answer = "Yes — you can change the primary theme color and switch between light or dark mode."
        ),
        FAQ(
            id = "T2",
            question = "Do I need internet to use the app?",
            answer = "Basic exploration works offline, but some content and updates require internet access."
        ),
        FAQ(
            id = "T3",
            question = "Is there Wi-Fi in space?",
            answer = "Yes, via the StarLink Interplanetary network, though latency increases as you move further from Earth."
        )
    )
}