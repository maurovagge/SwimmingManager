package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.RaceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


object GhostSimulationEngine {

    // Runs a massive background simulation for all out-of-region and international swimmers
    suspend fun simulateWorld(
        db: AppDatabase,
        currentYear: Int,
        currentWeek: Int,
        playerNation: String,
        playerRegion: String
    ) {
        // Divide the season into two halves:
        // Weeks 1-26: Short Course (25m) season
        // Weeks 27-52: Long Course (50m) season
        val isShortCourseSeason = currentWeek <= 26

        coroutineScope {
            val allSwimmers = db.swimmerDao().getAllSwimmers()
            val allClubs = db.clubDao().getAllClubs().associateBy { it.id }

            // --- LOAD BALANCING LOGIC ---
            // Using the modulo of the Swimmer's ID to distribute them evenly across 5 weeks.
            val targetModulo = currentWeek % 5

            // 1. FILTERING: Who needs ghost times THIS WEEK?
            val ghostSwimmers = allSwimmers.filter { swimmer ->
                val club = allClubs[swimmer.clubId]
                val isForeigner = swimmer.nationality != playerNation
                val isOutRegionNational = swimmer.nationality == playerNation && club?.region != playerRegion

                // Only swimmers whose ID matches the current week's turn will race
                val isTheirTurnToSwim = swimmer.id % 5 == targetModulo

                // Exclude retired swimmers
                !swimmer.isRetired && (isForeigner || isOutRegionNational) && isTheirTurnToSwim
            }

            // Get a list of base events, strictly filtering OUT the relays (assuming they contain "4x")
            val availableEvents = EventDictionary.getEventsForCourse(isShortCourseSeason)
                .filter { !it.id.contains("4x", ignoreCase = true) }

            // 2. PARALLEL PROCESSING: Divide into chunks of 500 for maximum CPU efficiency
            val chunks = ghostSwimmers.chunked(500)

            val newResults = chunks.map { chunk ->
                async(Dispatchers.Default) {
                    val chunkResults = mutableListOf<RaceResult>()

                    chunk.forEach { swimmer ->
                        val taperedSwimmer = swimmer.copy(tiredness = 0.5)
                        val genderSuffix = if (swimmer.gender == "M") "_M" else "_F"

                        val strokeKeyword = when (swimmer.mainStyle) {
                            "SL" -> "FREE"
                            "DO" -> "BACK"
                            "RA" -> "BREAST"
                            "FA" -> "FLY"
                            "MX" -> "MEDLEY"
                            else -> "FREE"
                        }

                        // A. Pick up to 3 random events
                        val preferredEvents = availableEvents.filter {
                            it.id.endsWith(genderSuffix) && it.id.contains(strokeKeyword)
                        }.shuffled().take(3)

                        // B. Pick exactly 1 random event
                        val randomEvent = availableEvents.filter {
                            it.id.endsWith(genderSuffix) && !preferredEvents.contains(it)
                        }.shuffled().take(1)

                        val targetEvents = preferredEvents + randomEvent

                        targetEvents.forEach { event ->
                            val timeMs = RaceEngine.simulateRace(taperedSwimmer, event, isShortCourse = isShortCourseSeason)

                            chunkResults.add(
                                RaceResult(
                                    swimmerId = swimmer.id,
                                    competitionId = -1,
                                    eventId = event.id,
                                    roundType = "FINAL",
                                    timeMs = timeMs,
                                    week = currentWeek,
                                    year = currentYear,
                                    isShortCourse = isShortCourseSeason
                                )
                            )
                        }
                    }
                    chunkResults
                }
            }.awaitAll().flatten()

            // 3. MASSIVE DB BATCH INSERT
            if (newResults.isNotEmpty()) {
                db.raceResultDao().insertResults(newResults)
            }
        }
    }
}