package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.SeasonBestLight
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.data.SwimmerLight
import com.example.swimmingmanager.data.TimeStandard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QualificationEngine {

    // --- 1. THE TURBO GENERATOR ---
    suspend fun generateTimeStandardsForAllNations(
        targetYear: Int,
        previousYearResults: List<SeasonBestLight>, 
        swimmers: List<SwimmerLight>
    ): List<TimeStandard> {

        return withContext(Dispatchers.Default) {
            val newStandards = mutableListOf<TimeStandard>()
            val swimmerMap = swimmers.associateBy { it.id }

            val resultsByBucket = previousYearResults.groupBy { result ->
                val nation = swimmerMap[result.swimmerId]?.nationality ?: "UNKNOWN"
                "${nation}|${result.eventId}|${if (result.isShortCourse) "SC" else "LC"}"
            }

            for ((bucketKey, results) in resultsByBucket) {
                val parts = bucketKey.split("|")
                val nationCode = parts[0]
                if (nationCode == "UNKNOWN") continue

                val eventId = parts[1]
                val isShortCourse = parts[2] == "SC"

                val sortedResults = results.mapNotNull { res ->
                    val s = swimmerMap[res.swimmerId] ?: return@mapNotNull null
                    Pair(s.age, res.timeMs)
                }.sortedBy { it.second }

                if (sortedResults.isEmpty()) continue

                val absCutoffIndex = minOf(29, (sortedResults.size * 0.7).toInt())
                val absoluteStandardMs = sortedResults[absCutoffIndex].second

                newStandards.add(TimeStandard(
                    year = targetYear, nationCode = nationCode, eventId = eventId,
                    tier = "NATIONAL_ABSOLUTE", timeMs = absoluteStandardMs, isShortCourse = isShortCourse
                ))

                val ragTime = mutableListOf<Int>()
                val junTime = mutableListOf<Int>()
                val cadTime = mutableListOf<Int>()

                for (pair in sortedResults) {
                    val age = pair.first
                    val time = pair.second
                    when (age) {
                        14, 15 -> ragTime.add(time)
                        16, 17 -> junTime.add(time)
                        18, 19 -> cadTime.add(time)
                    }
                }

                newStandards.add(createStandard(targetYear, nationCode, eventId, "NATIONAL_CADETTI", isShortCourse, cadTime, absoluteStandardMs, 20, 1.03))
                newStandards.add(createStandard(targetYear, nationCode, eventId, "NATIONAL_JUNIORES", isShortCourse, junTime, absoluteStandardMs, 30, 1.06))
                newStandards.add(createStandard(targetYear, nationCode, eventId, "NATIONAL_RAGAZZI", isShortCourse, ragTime, absoluteStandardMs, 40, 1.10))
            }

            newStandards
        }
    }

    private fun createStandard(
        year: Int, nation: String, event: String, tier: String,
        isShort: Boolean, times: List<Int>, absStd: Int,
        rankCutoff: Int, multiplier: Double
    ): TimeStandard {
        val fallback = (absStd * multiplier).toInt()
        val finalTime = if (times.size >= 5) {
            val index = minOf(rankCutoff - 1, times.size - 1)
            maxOf(times[index], fallback)
        } else {
            fallback
        }
        return TimeStandard(
            year = year,
            nationCode = nation,
            eventId = event,
            tier = tier,
            timeMs = finalTime,
            isShortCourse = isShort
        )
    }

    // --- 2. SINGLE SWIMMER CHECK (Used by UI and Player) ---
    suspend fun checkEligibility(
        db: AppDatabase,
        swimmer: Swimmer,
        event: SwimmingEvent,
        competition: Competition,
        nationCode: String,
        currentYear: Int
    ): Boolean {
        if (competition.tier == "REGIONAL" || competition.tier == "CUSTOM") return true

        if (swimmer.age < competition.minAge || swimmer.age > competition.maxAge) return false

        val standardTier = when {
            competition.tier == "NATIONAL" && competition.maxAge <= 15 -> "NATIONAL_RAGAZZI"
            competition.tier == "NATIONAL" && competition.maxAge <= 17 -> "NATIONAL_JUNIORES"
            competition.tier == "NATIONAL" && competition.maxAge <= 19 -> "NATIONAL_CADETTI"
            competition.tier == "NATIONAL" -> "NATIONAL_ABSOLUTE"
            competition.tier == "INTERNATIONAL" -> "NATIONAL_ABSOLUTE"
            else -> return true
        }

        val standardMs = db.timeStandardDao().getStandardTime(
            eventId = event.id,
            year = currentYear,
            tier = standardTier,
            nationCode = nationCode,
            isShortCourse = competition.isShortCourse
        ) ?: return true

        // 1. Check current season results
        val currentSeasonBest = db.raceResultDao().getSeasonBest(
            swimmerId = swimmer.id,
            eventId = event.id,
            minYear = currentYear,
            isShortCourse = competition.isShortCourse
        )

        // 2. Check previous season results (from our new permanent table)
        val previousSeasonBest = db.seasonBestDao().getSeasonBestsForSwimmers(listOf(swimmer.id), currentYear - 1)
            .find { it.eventId == event.id && it.isShortCourse == competition.isShortCourse }
            ?.timeMs

        // Pick the best of two
        val finalBest = when {
            currentSeasonBest != null && previousSeasonBest != null -> minOf(currentSeasonBest, previousSeasonBest)
            currentSeasonBest != null -> currentSeasonBest
            previousSeasonBest != null -> previousSeasonBest
            else -> null
        }

        return if (finalBest != null) finalBest <= standardMs else false
    }

    // --- 3. RAM CACHE SYSTEM (Used by AI Engine) ---
    class EligibilityCache(
        private val timeStandards: Map<String, Int>,
        private val seasonBests: Map<String, Int>
    ) {
        fun isEligible(
            swimmer: Swimmer,
            event: SwimmingEvent,
            competition: Competition
        ): Boolean {
            if (competition.tier == "REGIONAL" || competition.tier == "INTERNATIONAL" || competition.tier == "CUSTOM") return true

            if (swimmer.age < competition.minAge || swimmer.age > competition.maxAge) return false

            val standardTier = when {
                competition.tier == "NATIONAL" && competition.maxAge <= 15 -> "NATIONAL_RAGAZZI"
                competition.tier == "NATIONAL" && competition.maxAge <= 17 -> "NATIONAL_JUNIORES"
                competition.tier == "NATIONAL" && competition.maxAge <= 19 -> "NATIONAL_CADETTI"
                competition.tier == "NATIONAL" -> "NATIONAL_ABSOLUTE"
                else -> return true
            }

            val standardKey = "${standardTier}_${event.id}_${competition.isShortCourse}"
            val standardMs = timeStandards[standardKey] ?: return true

            val sbKey = "${swimmer.id}_${event.id}_${competition.isShortCourse}"
            val seasonBestMs = seasonBests[sbKey] ?: return false

            return seasonBestMs <= standardMs
        }
    }
}
