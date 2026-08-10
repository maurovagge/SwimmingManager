package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceRegistration
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.Swimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

import kotlinx.coroutines.*

object WeekendSimulationEngine {

    suspend fun simulateAiOnlyEvents(
        competition: Competition,
        allRegistrations: List<RaceRegistration>,
        playerClubId: Int,
        db: AppDatabase,
        currentYear: Int
    ) {
        if (allRegistrations.isEmpty()) return

        // 1. FAST LOADING: Get only the swimmers involved in ONE query
        val swimmerMap = db.swimmerDao().getSwimmersByCompetitionId(competition.id)
            .associateBy { it.id }

        val registrationsByEvent = allRegistrations.groupBy { it.eventCode }

        // 2. PARALLEL EXECUTION: We process each EVENT in a separate coroutine
        val allNewResults = withContext(Dispatchers.Default) {
            registrationsByEvent.entries.map { (eventId, eventRegistrations) ->
                async {
                    // --- IL FIX FONDAMENTALE ---
                    // Se l'evento è una staffetta, SALTA. Verrà gestita dal RelaySimulator a fine weekend.
                    if (eventId.startsWith("4x")) {
                        return@async emptyList<RaceResult>()
                    }

                    val eventResults = mutableListOf<RaceResult>()

                    // Skip if player is in this specific event
                    val hasPlayer = eventRegistrations.any { swimmerMap[it.swimmerId]?.clubId == playerClubId }
                    if (hasPlayer) return@async emptyList<RaceResult>()

                    val eventInfo = EventDictionary.getEventById(eventId) ?: return@async emptyList<RaceResult>()
                    val roundSequence = TournamentManager.getRoundSequence(eventId, competition.tier)
                    var activeSwimmerIds = eventRegistrations.map { it.swimmerId }

                    // Process Rounds (Heats -> Semis -> Finals)
                    for (i in roundSequence.indices) {
                        val currentRound = roundSequence[i]
                        val nextRound = roundSequence.getOrNull(i + 1)
                        val isFinal = nextRound == null // Se non c'è un round successivo, è la finale

                        val roundResults = mutableListOf<RaceResult>()

                        for (swimmerId in activeSwimmerIds) {
                            val swimmer = swimmerMap[swimmerId] ?: continue

                            val aiTiredness = if (competition.tier == "INTERNATIONAL") 0.0 else 0.5
                            val taperedSwimmer = swimmer.copy(tiredness = aiTiredness)

                            val timeMs = RaceEngine.simulateRace(
                                taperedSwimmer, eventInfo, competition.isShortCourse
                            )

                            val result = RaceResult(
                                swimmerId = swimmer.id,
                                competitionId = competition.id,
                                eventId = eventId,
                                isShortCourse = competition.isShortCourse,
                                timeMs = timeMs,
                                week = competition.week,
                                year = currentYear,
                                roundType = currentRound
                            )

                            roundResults.add(result)

                            // --- THE SPEED BOOSTER ---
                            // Save ONLY if it's the final round.
                            // This reduces 90% of DB writes for AI-only events.
                            if (isFinal) {
                                eventResults.add(result)
                            }
                        }

                        // Promotion logic (still needed to know who goes to finals)
                        if (nextRound != null) {
                            val limit = TournamentManager.getPromotionLimit(currentRound, nextRound)
                            activeSwimmerIds = roundResults
                                .sortedBy { it.timeMs }
                                .take(limit)
                                .map { it.swimmerId }
                        }
                    }
                    eventResults
                }
            }.awaitAll().flatten() // Combine all results from all events
        }

        // 3. ATOMIC CHUNKED INSERTION
        if (allNewResults.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                // Large chunks to reduce transaction overhead
                allNewResults.chunked(5000).forEach { chunk ->
                    db.raceResultDao().insertResults(chunk)
                }
            }
        }
    }
}