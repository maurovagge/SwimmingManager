package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.RelayResult
import com.example.swimmingmanager.data.Swimmer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LiveRaceEngine {

    suspend fun simulateInteractiveRound(
        competition: Competition,
        eventId: String,
        roundType: String,
        activeSwimmers: List<Swimmer>,
        db: AppDatabase,
        currentYear: Int
    ): List<RaceResult> {
        if (eventId.startsWith("4x")) return emptyList()

        val results = mutableListOf<RaceResult>()
        val eventInfo = EventDictionary.getEventById(eventId) ?: return emptyList()

        for (swimmer in activeSwimmers) {
            val timeMs = RaceEngine.simulateRace(
                swimmer = swimmer,
                event = eventInfo,
                isShortCourse = competition.isShortCourse
            )

            results.add(
                RaceResult(
                    swimmerId = swimmer.id,
                    competitionId = competition.id,
                    eventId = eventId,
                    isShortCourse = competition.isShortCourse,
                    timeMs = timeMs,
                    week = competition.week,
                    year = currentYear,
                    roundType = roundType
                )
            )
        }

        val sortedResults = results.sortedBy { it.timeMs }
        db.raceResultDao().insertResults(sortedResults)
        return sortedResults
    }

    suspend fun simulateInteractiveRelayRound(
        competition: Competition,
        eventId: String,
        roundType: String,
        playerSwimmers: List<Swimmer>,
        playerClubId: Int,
        db: AppDatabase,
        currentYear: Int
    ): List<RelayResult> {

        val results = mutableListOf<RelayResult>()

        val isMenEvent = eventId.endsWith("_M")
        val isMedley = eventId.contains("MEDLEY")
        val distance = when {
            eventId.contains("50") -> 50
            eventId.contains("100") -> 100
            else -> 200
        }
        val targetGenderString = if (isMenEvent) "M" else "F"

        val dummyEvent = SwimmingEvent(
            id = eventId,
            distance = distance,
            stroke = if (isMedley) Stroke.MEDLEY else Stroke.FREESTYLE,
            gender = if (isMenEvent) Gender.M else Gender.F
        )

        val isInternational = competition.tier == "WORLD" || competition.tier == "INTERNATIONAL" || competition.tier == "CONTINENTAL"

        val allSwimmersAtMeet = withContext(Dispatchers.IO) {
            db.swimmerDao().getSwimmersByCompetitionId(competition.id)
                .distinctBy { it.id }
        }
        val clubsMap = withContext(Dispatchers.IO) {
            db.clubDao().getAllClubs().associateBy { it.id }
        }

        val teamsRoster = if (isInternational) {
            allSwimmersAtMeet.groupBy { it.nationality }
        } else {
            allSwimmersAtMeet.groupBy { it.clubId.toString() }
        }

        for ((teamKey, fullRoster) in teamsRoster) {
            val teamId = if (isInternational) 0 else teamKey!!.toInt()
            val teamName = if (isInternational) teamKey else clubsMap[teamId]?.name ?: "Club $teamKey"
            val isPlayerTeam = !isInternational && teamId == playerClubId

            val eligibleSwimmers = fullRoster.filter { it.gender == targetGenderString }

            val rawRelayTeam: List<Swimmer>? = if (isPlayerTeam) {
                if (playerSwimmers.size == 4) playerSwimmers else null
            } else {
                RelayTeamSelector.selectTeamForRelay(eligibleSwimmers, eventId)
            }

            if (rawRelayTeam != null && rawRelayTeam.size == 4) {
                // FIX: Applichiamo il tapering agli atleti IA per le staffette
                val taperedTeam = if (isPlayerTeam) {
                    rawRelayTeam // Gli atleti del giocatore usano la loro stanchezza attuale
                } else {
                    rawRelayTeam.map { swimmer ->
                        val aiTargetTiredness = when (competition.tier) {
                            "WORLD", "INTERNATIONAL", "CONTINENTAL" -> 0.0
                            "NATIONAL" -> 0.5
                            "REGIONAL" -> 1.0
                            else -> swimmer.tiredness
                        }
                        swimmer.copy(tiredness = aiTargetTiredness)
                    }
                }

                val splits = RaceEngine.calculateRelaySplits(taperedTeam, dummyEvent, competition.isShortCourse)
                val totalTimeMs = splits.sum()

                results.add(
                    RelayResult(
                        competitionId = competition.id,
                        eventId = eventId,
                        teamId = teamId,
                        teamName = teamName!!,
                        totalTimeMs = totalTimeMs,
                        swimmer1Id = rawRelayTeam[0].id, swimmer1SplitMs = splits[0],
                        swimmer2Id = rawRelayTeam[1].id, swimmer2SplitMs = splits[1],
                        swimmer3Id = rawRelayTeam[2].id, swimmer3SplitMs = splits[2],
                        swimmer4Id = rawRelayTeam[3].id, swimmer4SplitMs = splits[3],
                        year = currentYear,
                        isShortCourse = competition.isShortCourse,
                        roundType = roundType
                    )
                )
            }
        }

        val sortedResults = results.sortedBy { it.totalTimeMs }
        withContext(Dispatchers.IO) {
            db.raceResultDao().insertRelayResults(sortedResults)
        }
        return sortedResults
    }

    fun getQualifiersForNextRound(
        currentResults: List<RaceResult>,
        currentRound: String,
        nextRound: String
    ): List<Int> {
        val promotionLimit = TournamentManager.getPromotionLimit(currentRound, nextRound)
        if (promotionLimit <= 0) return emptyList()
        return currentResults.take(promotionLimit).map { it.swimmerId }
    }
}
