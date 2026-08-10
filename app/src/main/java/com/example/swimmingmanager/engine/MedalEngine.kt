package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.Medal
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.RelayResult
import com.example.swimmingmanager.data.SeasonBestLight
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.data.SwimmerLight

object MedalEngine {

    private val europeanNations = listOf(
        "ITA", "GBR", "FRA", "GER", "HUN", "NED", "ESP",
        "SWE", "ROU", "LTU", "BEL", "SUI", "RUS"
    )

    suspend fun awardMedalsFromMeet(db: AppDatabase, results: List<RaceResult>, competition: Competition) {
        if (competition.tier == "REGIONAL" || competition.tier == "CUSTOM") return

        // --- NEW LOGIC FOR NATIONAL EVENTS ---
        val isChampionship = competition.week == 16 || competition.week in 24..26|| competition.week == 36 || competition.week in 45..47
        val isTrophy = competition.week == 8 || competition.week == 18 || competition.week == 32 || competition.week == 42

        // If it's a National tier but not one of our target weeks, we skip
        if (competition.tier == "NATIONAL" && !isChampionship && !isTrophy) {
            return
        }

        val newMedals = mutableListOf<Medal>()
        val colors = listOf("GOLD", "SILVER", "BRONZE")

        val finalResults = results.filter { it.roundType == "FINAL" || it.roundType == "TIMED_FINAL" }
        val resultsByEvent = finalResults.groupBy { it.eventId }

        // 1. DETERMINE CATEGORY
        val category = when {
            competition.name.contains("Ragazzi", ignoreCase = true) -> "RAGAZZI"
            competition.name.contains("Juniores", ignoreCase = true) || competition.name.contains("Junior", ignoreCase = true) -> "JUNIORES"
            competition.name.contains("Cadetti", ignoreCase = true) -> "CADETTI"
            else -> "ASSOLUTI"
        }

        // 2. DETERMINE TIER (Distinguishing Titles from Trophies)
        val medalTier = when {
            competition.name.contains("Olympic", ignoreCase = true) -> "OLYMPIC"
            competition.name.contains("World", ignoreCase = true) -> "WORLD"
            competition.name.contains("European", ignoreCase = true) || competition.name.contains("Intercontinental", ignoreCase = true) -> "CONTINENTAL"
            isTrophy -> "NATIONAL_TROPHY"
            else -> "NATIONAL"
        }

        for ((eventId, eventResults) in resultsByEvent) {
            val top3 = eventResults.sortedBy { it.timeMs }.take(3)
            top3.forEachIndexed { index, res ->
                newMedals.add(Medal(
                    swimmerId = res.swimmerId,
                    year = res.year,
                    tier = medalTier,
                    course = if (competition.isShortCourse) "SHORT" else "LONG",
                    category = category,
                    color = colors[index],
                    eventId = eventId
                ))
            }
        }

        if (newMedals.isNotEmpty()) {
            db.medalDao().insertMedals(newMedals)
        }
    }

    // --- LOGIC B: GHOST RANKING MEDAL ASSIGNMENT (TURBO VERSION) ---
    suspend fun distributeGhostMedals(
        db: AppDatabase,
        year: Int,
        playerNationCode: String,
        seasonBests: List<SeasonBestLight>,
        swimmersLight: List<SwimmerLight>
    ) {
        val isPlayerEuropean = europeanNations.contains(playerNationCode)

        val ageMap = swimmersLight.associate { it.id to it.age }
        val nationMap = swimmersLight.associate { it.id to it.nationality }

        val newMedals = mutableListOf<Medal>()

        // 2. MEGA-GROUPING: Group by Nation, Event, and Course in ONE pass
        val groupedByNationAndEvent = seasonBests.groupBy { res ->
            val nation = nationMap[res.swimmerId] ?: ""
            "${nation}|${res.eventId}|${res.isShortCourse}"
        }

        // 3. PROCESS NATIONAL MEDALS
        for ((key, results) in groupedByNationAndEvent) {
            val parts = key.split("|")
            val nation = parts[0]
            if (nation == "" || nation == playerNationCode) continue

            val eventId = parts[1]
            val isShort = parts[2].toBoolean()

            assignMedalsAcrossAllCategories(
                newMedals, results, ageMap, year, "NATIONAL",
                if (isShort) "SHORT" else "LONG", eventId
            )
        }

        // 4. PROCESS CONTINENTAL MEDALS
        val continentalGroups = seasonBests.groupBy { res ->
            val nation = nationMap[res.swimmerId] ?: ""
            val isEuro = europeanNations.contains(nation)
            "${if (isEuro) "EURO" else "WORLD"}|${res.eventId}|${res.isShortCourse}"
        }

        for ((key, results) in continentalGroups) {
            val parts = key.split("|")
            val isEuroGroup = parts[0] == "EURO"

            if (isEuroGroup != isPlayerEuropean) {
                val eventId = parts[1]
                val isShort = parts[2].toBoolean()

                assignMedalsAcrossAllCategories(
                    newMedals, results, ageMap, year, "CONTINENTAL",
                    if (isShort) "SHORT" else "LONG", eventId
                )
            }
        }

        if (newMedals.isNotEmpty()) {
            newMedals.chunked(2000).forEach { chunk ->
                db.medalDao().insertMedals(chunk)
            }
        }
    }

    private fun assignMedalsAcrossAllCategories(
        list: MutableList<Medal>,
        res: List<SeasonBestLight>,
        ageMap: Map<Int, Int>,
        year: Int,
        tier: String,
        course: String,
        eventId: String
    ) {
        val sortedResults = res.sortedBy { it.timeMs }
        val colors = listOf("GOLD", "SILVER", "BRONZE")
        val categories = listOf("ASSOLUTI", "CADETTI", "JUNIORES", "RAGAZZI")

        for (cat in categories) {
            val catResults = when(cat) {
                "ASSOLUTI" -> sortedResults
                "CADETTI"  -> sortedResults.filter { (ageMap[it.swimmerId] ?: 0) in 18..19 }
                "JUNIORES" -> sortedResults.filter { (ageMap[it.swimmerId] ?: 0) in 16..17 }
                else       -> sortedResults.filter { (ageMap[it.swimmerId] ?: 0) <= 15 }
            }

            catResults.take(3).forEachIndexed { index, result ->
                list.add(Medal(
                    swimmerId = result.swimmerId, year = year, tier = tier,
                    course = course, category = cat, color = colors[index], eventId = eventId
                ))
            }
        }
    }

    // --- NEW: LOGIC FOR RELAY MEDAL ASSIGNMENT ---
    suspend fun awardRelayMedalsFromMeet(
        db: AppDatabase,
        relayResults: List<RelayResult>,
        competition: Competition
    ) {
        // 1. Skip regional or custom meets as per individual logic
        if (competition.tier == "REGIONAL" || competition.tier == "CUSTOM") return

        // 2. Identify relevant national weeks (Championships and Trophies)
        val isChampionship = competition.week == 16 || competition.week in 24..26 ||
                competition.week == 36 || competition.week in 45..47
        val isTrophy = competition.week == 8 || competition.week == 18 ||
                competition.week == 32 || competition.week == 42

        if (competition.tier == "NATIONAL" && !isChampionship && !isTrophy) {
            return
        }

        val newMedals = mutableListOf<Medal>()
        val colors = listOf("GOLD", "SILVER", "BRONZE")

        // 3. Determine Category (Ragazzi, Juniores, etc.)
        val category = when {
            competition.name.contains("Ragazzi", ignoreCase = true) -> "RAGAZZI"
            competition.name.contains("Juniores", ignoreCase = true) ||
                    competition.name.contains("Junior", ignoreCase = true) -> "JUNIORES"
            competition.name.contains("Cadetti", ignoreCase = true) -> "CADETTI"
            else -> "ASSOLUTI"
        }

        // 4. Determine Medal Tier
        val medalTier = when {
            competition.name.contains("Olympic", ignoreCase = true) -> "OLYMPIC"
            competition.name.contains("World", ignoreCase = true) -> "WORLD"
            competition.name.contains("European", ignoreCase = true) ||
                    competition.name.contains("Intercontinental", ignoreCase = true) -> "CONTINENTAL"
            isTrophy -> "NATIONAL_TROPHY"
            else -> "NATIONAL"
        }

        // 5. Group relay results by event
        val resultsByEvent = relayResults.groupBy { it.eventId }

        for ((eventId, eventResults) in resultsByEvent) {
            val top3 = eventResults.sortedBy { it.totalTimeMs }.take(3)

            top3.forEachIndexed { index, teamRes ->
                val medalColor = colors[index]

                // Create a medal entry for EACH of the 4 swimmers
                val swimmerIds = listOf(
                    teamRes.swimmer1Id, teamRes.swimmer2Id,
                    teamRes.swimmer3Id, teamRes.swimmer4Id
                )

                swimmerIds.forEach { sId ->
                    newMedals.add(Medal(
                        swimmerId = sId,
                        year = teamRes.year,
                        tier = medalTier,
                        course = if (competition.isShortCourse) "SHORT" else "LONG",
                        category = category,
                        color = medalColor,
                        eventId = eventId
                    ))
                }
            }
        }

        // 6. Save ALL the medals (4 per team) to the database
        if (newMedals.isNotEmpty()) {
            db.medalDao().insertMedals(newMedals)
        }
    }
}