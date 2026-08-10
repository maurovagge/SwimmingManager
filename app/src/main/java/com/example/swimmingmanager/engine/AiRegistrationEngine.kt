package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceRegistration
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.data.SeasonBestLight

object AiRegistrationEngine {

    // Main function called when pressing "Advance Week"
    suspend fun processAiRegistrations(
        competition: Competition,
        competitionNation: String,
        competitionRegion: String,
        playerClubId: Int,
        db: AppDatabase,
        currentYear: Int
    ) {
        // 1. SURGICAL LOADING: Fetch only AI swimmers who are eligible for this meet
        val aiSwimmers = db.swimmerDao().getEligibleSwimmersForMeet(
            minAge = competition.minAge,
            maxAge = competition.maxAge,
            nation = competitionNation,
            region = competitionRegion,
            tier = competition.tier,
            playerClubId = playerClubId
        )

        // DEBUG LOGS
        println("DEBUG_AI: Competition Tier: ${competition.tier} | Nation: $competitionNation | Region: $competitionRegion")
        println("DEBUG_AI: AI Swimmers found: ${aiSwimmers.size}")

        // If no swimmers are found, exit early
        if (aiSwimmers.isEmpty()) return

        // 2. Filter out relays
        val availableEvents = EventDictionary.getEventsForCourse(competition.isShortCourse)
            .filter { !it.id.contains("4x", ignoreCase = true) }

        // 3. Route logic by Tier
        when (competition.tier) {
            "REGIONAL" -> processRegionalRegistrations(aiSwimmers, availableEvents, competition, db)
            "NATIONAL" -> processNationalRegistrations(
                aiSwimmers,
                availableEvents,
                competition,
                competitionNation,
                db,
                currentYear
            )
        }
    }

    private suspend fun processRegionalRegistrations(
        aiSwimmers: List<Swimmer>,
        availableEvents: List<SwimmingEvent>,
        competition: Competition,
        db: AppDatabase
    ) {
        val newRegistrations = mutableListOf<RaceRegistration>()

        for (swimmer in aiSwimmers) {
            val genderStr = if (swimmer.gender == "M") "_M" else "_F"
            val eligibleEvents = availableEvents.filter { it.id.endsWith(genderStr, ignoreCase = true) }

            // 1. Find strengths
            val stats = mapOf(
                "FREE" to swimmer.freestyle,
                "BACK" to swimmer.backstroke,
                "BREAST" to swimmer.breaststroke,
                "FLY" to swimmer.butterfly
            )
            val topStrokes = stats.toList().sortedByDescending { it.second }.take(2).map { it.first }

            // 2. Selection logic
            val mainEvents = eligibleEvents.filter { event ->
                val eventIdUpper = event.id.uppercase()
                topStrokes.any { eventIdUpper.contains(it) } || eventIdUpper.contains("MEDLEY")
            }.shuffled()

            val secondaryEvents = eligibleEvents.filter { it !in mainEvents }.shuffled()
            val chosenEvents = mutableListOf<SwimmingEvent>()

            val mainRacesCount = (2..3).random()
            chosenEvents.addAll(mainEvents.take(mainRacesCount))

            if (Math.random() < 0.35 && secondaryEvents.isNotEmpty()) {
                chosenEvents.add(secondaryEvents.first())
            }

            chosenEvents.take(4).forEach { event ->
                newRegistrations.add(
                    RaceRegistration(
                        swimmerId = swimmer.id,
                        competitionId = competition.id,
                        eventCode = event.id,
                        week = competition.week
                    )
                )
            }
        }

        if (newRegistrations.isNotEmpty()) {
            db.raceRegistrationDao().insertRegistrations(newRegistrations)
        }
    }

    private suspend fun processNationalRegistrations(
        aiSwimmers: List<Swimmer>,
        availableEvents: List<SwimmingEvent>,
        competition: Competition,
        competitionNation: String,
        db: AppDatabase,
        currentYear: Int
    ) {
        val newRegistrations = mutableListOf<RaceRegistration>()
        val targetSwimmerIds = aiSwimmers.map { it.id }

        if (targetSwimmerIds.isEmpty()) return

        // 1. Fetch Standards
        val allStandards = db.timeStandardDao().getAllStandardsForNation(currentYear, competitionNation)
        val standardsMap = allStandards.associate {
            "${it.tier}_${it.eventId}_${it.isShortCourse}" to it.timeMs
        }

        // 2. FETCH SEASON BESTS (Current Year results)
        val currentResults = db.raceResultDao().getOptimizedRecentResults(
            fromYear = currentYear,
            isShortCourse = competition.isShortCourse,
            swimmerIds = targetSwimmerIds
        )

        // 3. FETCH PREVIOUS SEASON BESTS (to handle qualification for early season meets)
        val previousResults = db.seasonBestDao().getSeasonBestsForSwimmers(
            swimmerIds = targetSwimmerIds,
            year = currentYear - 1
        )

        // 4. MERGE RESULTS (Take the best time between last year and this year)
        val bestTimesLookup = mutableMapOf<String, Int>()
        
        // Add previous year times
        for (pb in previousResults) {
            if (pb.isShortCourse == competition.isShortCourse) {
                val key = "${pb.swimmerId}_${pb.eventId}_${pb.isShortCourse}"
                bestTimesLookup[key] = pb.timeMs
            }
        }
        
        // Overwrite with current season bests if they are better
        for (res in currentResults) {
            val key = "${res.swimmerId}_${res.eventId}_${res.isShortCourse}"
            val existing = bestTimesLookup[key]
            if (existing == null || res.timeMs < existing) {
                bestTimesLookup[key] = res.timeMs
            }
        }

        // 5. Initialize eligibility cache using the composite lookup
        val eligibilityCache = QualificationEngine.EligibilityCache(standardsMap, bestTimesLookup)

        for (swimmer in aiSwimmers) {
            val genderStr = if (swimmer.gender == "M") "_M" else "_F"
            val eligibleEvents = availableEvents.filter { it.id.endsWith(genderStr, ignoreCase = true) }

            val qualifiedEvents = mutableListOf<SwimmingEvent>()

            for (event in eligibleEvents) {
                if (eligibilityCache.isEligible(swimmer, event, competition)) {
                    qualifiedEvents.add(event)
                }
            }

            if (qualifiedEvents.isNotEmpty()) {
                val stats = mapOf(
                    "FREE" to swimmer.freestyle,
                    "BACK" to swimmer.backstroke,
                    "BREAST" to swimmer.breaststroke,
                    "FLY" to swimmer.butterfly
                )

                val topStrokes = stats.toList().sortedByDescending { it.second }.take(2).map { it.first }

                // A swimmer is considered a good IM swimmer if they have 3+ strokes above 39 or 2+ strokes above 55
                val isIMSpecialist = stats.values.count { it >= 39.0 } >= 3 || stats.values.count { it >= 55.0 } >= 2

                val chosenEvents = qualifiedEvents.shuffled().sortedByDescending { event ->
                    val eventIdUpper = event.id.uppercase()
                    var priorityScore = 0

                    // Priority for top strokes
                    if (topStrokes.any { eventIdUpper.contains(it) }) priorityScore += 10

                    // Priority for Medley
                    if (eventIdUpper.contains("MEDLEY")) {
                        if (isIMSpecialist) {
                            priorityScore += 15 // High priority for specialists
                        } else {
                            priorityScore += 5  // Even non-specialists prioritize IM if qualified for Nationals
                        }
                    }

                    priorityScore
                }.take(6)

                chosenEvents.forEach { event ->
                    newRegistrations.add(
                        RaceRegistration(
                            swimmerId = swimmer.id,
                            competitionId = competition.id,
                            eventCode = event.id,
                            week = competition.week
                        )
                    )
                }
            }
        }

        if (newRegistrations.isNotEmpty()) {
            db.raceRegistrationDao().insertRegistrations(newRegistrations)
        }
    }
}
