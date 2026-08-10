package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.Competition

object TournamentManager {

    // Define the sequence of rounds for a specific event based on the competition tier
    fun getRoundSequence(eventId: String, tier: String): List<String> {
        val distance = eventId.split("_").firstOrNull()?.toIntOrNull() ?: 0
        val isMedley = eventId.contains("MEDLEY")

        return when (tier) {
            "REGIONAL" -> listOf("TIMED_FINAL")

            "NATIONAL" -> {
                // 800 and 1500 are always Timed Finals (Directly to results)
                if (distance == 800 || distance == 1500) {
                    listOf("TIMED_FINAL")
                } else {
                    listOf("HEAT", "FINAL")
                }
            }

            "INTERNATIONAL" -> {
                when {
                    // 800, 1500 and 400 Medley skip Semifinals in International meets
                    distance == 800 || distance == 1500 || (distance == 400 && isMedley) -> {
                        listOf("HEAT", "FINAL")
                    }
                    // All other events follow the full Olympic flow
                    else -> listOf("HEAT", "SEMI", "FINAL")
                }
            }

            else -> listOf("TIMED_FINAL")
        }
    }

    // Logic to determine how many swimmers advance to the next round
    fun getPromotionLimit(currentRound: String, nextRound: String): Int {
        return when (nextRound) {
            "FINAL" -> 8   // Standard 8-lane final
            "SEMI" -> 16   // Top 16 from Heats go to Semis
            else -> 0
        }
    }

    // Helper to check if an event is a "Direct Final" (no heats)
    fun isTimedFinal(eventId: String, tier: String): Boolean {
        return getRoundSequence(eventId, tier).contains("TIMED_FINAL")
    }
}