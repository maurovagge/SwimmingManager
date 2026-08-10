package com.example.swimmingmanager.engine

import android.annotation.SuppressLint
import com.example.swimmingmanager.data.Swimmer
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

// 1. Defines how much each stat weights based on the race parameters
data class RaceWeights(
    val mainStrokeWeight: Double,
    val speedWeight: Double,
    val sprintWeight: Double,
    val enduranceWeight: Double,
    val underwaterWeight: Double,
    val techniqueWeight: Double
)

data class EventLimits(
    val worldRecord: Double,
    val beginnerTime: Double
)

object RaceEngine {

    // --- CACHE MEMORY---
    private val weightsCache = ConcurrentHashMap<String, RaceWeights>()
    private val limitsCache = ConcurrentHashMap<String, EventLimits>()

    private fun getWeights(distance: Int, isShortCourse: Boolean): RaceWeights {
        val cacheKey = "${distance}_${isShortCourse}"


        weightsCache[cacheKey]?.let { return it }

        var mainW = 0.30; var speedW = 0.20; var sprintW = 0.20; var endW = 0.20; var underW = 0.10; var techW = 0.00

        when (distance) {
            50 -> { mainW = 0.18; speedW = 0.28; sprintW = 0.35; endW = 0.00; underW = 0.10; techW = 0.09 }
            100 -> { mainW = 0.30; speedW = 0.28; sprintW = 0.22; endW = 0.02; underW = 0.10; techW = 0.08 }
            200 -> { mainW = 0.35; speedW = 0.20; sprintW = 0.05; endW = 0.20; underW = 0.10; techW = 0.10 }
            400 -> { mainW = 0.40; speedW = 0.10; sprintW = 0.02; endW = 0.30; underW = 0.05; techW = 0.13 }
            800 -> { mainW = 0.40; speedW = 0.10; sprintW = 0.00; endW = 0.40; underW = 0.00 ; techW = 0.10}
            1500 -> { mainW = 0.40; speedW = 0.05; sprintW = 0.00; endW = 0.45; underW = 0.00; techW = 0.10 }
        }

        if (isShortCourse) {
            val shift = if (distance > 200) 0.05 else 0.08
            underW += shift
            if (endW >= shift) endW -= shift else if (speedW >= shift) speedW -= shift
        }

        val result = RaceWeights(mainW, speedW, sprintW, endW, underW, techW)
        weightsCache[cacheKey] = result
        return result
    }

    private fun getEventLimits(eventId: String, isShortCourse: Boolean): EventLimits {
        val cacheKey = "${eventId}_${isShortCourse}"

        limitsCache[cacheKey]?.let { return it }

        val baseLimits = when (eventId) {
            "50_FREE_M" -> EventLimits(20.70, 32.00)
            "100_FREE_M" -> EventLimits(46.24, 70.00)    // 1:10
            "200_FREE_M" -> EventLimits(101.30, 145.00)  // 2:25
            "400_FREE_M" -> EventLimits(218.90, 317.00)  // 5:17
            "800_FREE_M" -> EventLimits(451.10, 667.00)  // 11:07
            "1500_FREE_M" -> EventLimits(869.50, 1300.00)// 21:40

            "50_FREE_F" -> EventLimits(23.30, 35.50)
            "100_FREE_F" -> EventLimits(51.11, 78.00)    // 1:18
            "200_FREE_F" -> EventLimits(111.85, 160.00)  // 2:40
            "400_FREE_F" -> EventLimits(233.38, 345.00)  // 5:45
            "800_FREE_F" -> EventLimits(483.79, 725.00)  // 12:05
            "1500_FREE_F" -> EventLimits(918.48, 1400.00)// 23:20

            "50_BACK_M" -> EventLimits(23.20, 36.00)
            "100_BACK_M" -> EventLimits(51.00, 78.00)    // 1:18
            "200_BACK_M" -> EventLimits(110.72, 165.00)  // 2:45
            "50_BACK_F" -> EventLimits(26.46, 40.00)
            "100_BACK_F" -> EventLimits(56.63, 86.00)    // 1:26
            "200_BACK_F" -> EventLimits(121.90, 180.00)  // 3:00

            "50_BREAST_M" -> EventLimits(25.65, 40.00)
            "100_BREAST_M" -> EventLimits(56.48, 88.00)  // 1:28
            "200_BREAST_M" -> EventLimits(124.48, 190.00)// 3:10
            "50_BREAST_F" -> EventLimits(28.86, 44.00)
            "100_BREAST_F" -> EventLimits(63.13, 98.00)  // 1:38
            "200_BREAST_F" -> EventLimits(135.95, 205.00)// 3:25

            "50_FLY_M" -> EventLimits(21.97, 34.00)
            "100_FLY_M" -> EventLimits(48.90, 78.00)     // 1:18
            "200_FLY_M" -> EventLimits(109.34, 175.00)   // 2:55
            "50_FLY_F" -> EventLimits(24.03, 37.00)
            "100_FLY_F" -> EventLimits(54.68, 88.00)     // 1:28
            "200_FLY_F" -> EventLimits(120.01, 195.00)   // 3:15

            "100_MEDLEY_M" -> EventLimits(46.50, 78.00)  // 1:18
            "200_MEDLEY_M" -> EventLimits(107.00, 170.00)// 2:50
            "400_MEDLEY_M" -> EventLimits(228.50, 360.00)// 6:00
            "100_MEDLEY_F" -> EventLimits(53.30, 88.00)  // 1:28
            "200_MEDLEY_F" -> EventLimits(119.60, 190.00)// 3:10
            "400_MEDLEY_F" -> EventLimits(251.30, 400.00)// 6:40

            else -> EventLimits(100.0, 200.0)
        }

        val calculatedLimit = if (isShortCourse && !eventId.contains("100_MEDLEY") && (eventId.contains("_M"))) {
            EventLimits(baseLimits.worldRecord * 0.96, baseLimits.beginnerTime * 0.96)
        } else if (isShortCourse && !eventId.contains("100_MEDLEY") && (eventId.contains("_F"))) {
            EventLimits(baseLimits.worldRecord * 0.97, baseLimits.beginnerTime * 0.97)
        } else {
            baseLimits
        }

        limitsCache[cacheKey] = calculatedLimit
        return calculatedLimit
    }

    // --- INDIVIDUAL RACE SIMULATOR ---
    fun simulateRace(swimmer: Swimmer, event: SwimmingEvent, isShortCourse: Boolean): Int {
        val weights = getWeights(event.distance, isShortCourse)

        val mainStrokeStat = when (event.stroke) {
            Stroke.FREESTYLE -> swimmer.freestyle
            Stroke.BACKSTROKE -> swimmer.backstroke
            Stroke.BREASTSTROKE -> swimmer.breaststroke
            Stroke.BUTTERFLY -> swimmer.butterfly
            Stroke.MEDLEY -> (swimmer.freestyle + swimmer.backstroke + swimmer.breaststroke + swimmer.butterfly) / 4.0
        }

        val weightedSkill = (mainStrokeStat * weights.mainStrokeWeight) +
                (swimmer.speed * weights.speedWeight) +
                (swimmer.sprintPower * weights.sprintWeight) +
                (swimmer.endurance * weights.enduranceWeight) +
                (swimmer.underwater * weights.underwaterWeight) +
                (swimmer.technique * weights.techniqueWeight)

        val limits = getEventLimits(event.id, isShortCourse)
        val timeRange = limits.beginnerTime - limits.worldRecord
        var finalTimeSeconds = limits.beginnerTime - ((weightedSkill / 100.0) * timeRange)

        val distanceMultiplier = event.distance / 100.0
        val tirednessMalus = (swimmer.tiredness * 0.4) * distanceMultiplier
        val moraleBonus = (swimmer.moral - 2.5) * 0.1 * distanceMultiplier

        finalTimeSeconds = finalTimeSeconds + tirednessMalus - moraleBonus

        val rngRange = 0.40 * distanceMultiplier
        val rngFactor = Random.nextDouble(-rngRange, rngRange)
        finalTimeSeconds += rngFactor

        return (finalTimeSeconds * 1000).toInt()
    }

    // --- RELAY RACE SIMULATOR ---
    fun simulateRelay(team: List<Swimmer>, relayEvent: SwimmingEvent, isShortCourse: Boolean): Int {
        if (team.size != 4) throw IllegalArgumentException("A relay must have exactly 4 swimmers")

        var totalTimeMs = 0

        for (i in 0..3) {
            val legStroke = if (relayEvent.stroke == Stroke.MEDLEY) {
                when (i) {
                    0 -> Stroke.BACKSTROKE
                    1 -> Stroke.BREASTSTROKE
                    2 -> Stroke.BUTTERFLY
                    else -> Stroke.FREESTYLE
                }
            } else {
                relayEvent.stroke
            }

            val legEvent = SwimmingEvent(
                id = "LEG_${relayEvent.distance}_${legStroke.name}",
                distance = relayEvent.distance,
                stroke = legStroke,
                gender = relayEvent.gender
            )

            var legTimeMs = simulateRace(team[i], legEvent, isShortCourse)

            if (i > 0) {
                val flyingStartAdvantageMs = Random.nextInt(400, 800)
                legTimeMs -= flyingStartAdvantageMs
            }

            totalTimeMs += legTimeMs
        }

        return totalTimeMs
    }

    // --- UI FORMATTER ---
    @SuppressLint("DefaultLocale")
    fun formatTime(timeMs: Int): String {
        val minutes = timeMs / 60000
        val remainingMs = timeMs % 60000
        val seconds = remainingMs / 1000
        val hundredths = (remainingMs % 1000) / 10

        return if (minutes > 0) {
            String.format("%d:%02d.%02d", minutes, seconds, hundredths)
        } else {
            String.format("%d.%02d", seconds, hundredths)
        }
    }

    /**
     * Calculates the individual split times for a 4-person relay team.
     * Returns a List of 4 integers (the times in milliseconds).
     */

    fun calculateRelaySplits(team: List<Swimmer>, relayEvent: SwimmingEvent, isShortCourse: Boolean): List<Int> {
        val splits = mutableListOf<Int>()
        val isMedley = relayEvent.stroke == Stroke.MEDLEY

        // Determine gender string safely
        val genderStr = if (relayEvent.id.endsWith("_M")) "M" else "F"

        for (i in 0..3) {
            val swimmer = team[i]

            // 1. Determine the stroke for this specific leg
            val strokeForThisLeg = if (isMedley) {
                when (i) {
                    0 -> Stroke.BACKSTROKE
                    1 -> Stroke.BREASTSTROKE
                    2 -> Stroke.BUTTERFLY
                    else -> Stroke.FREESTYLE
                }
            } else {
                Stroke.FREESTYLE
            }

            // 2. Reconstruct the REAL individual event ID (e.g., "100_FREE_M")
            val strokeStr = when (strokeForThisLeg) {
                Stroke.FREESTYLE -> "FREE"
                Stroke.BACKSTROKE -> "BACK"
                Stroke.BREASTSTROKE -> "BREAST"
                Stroke.BUTTERFLY -> "FLY"
                else -> "FREE"
            }

            val realIndividualEventId = "${relayEvent.distance}_${strokeStr}_${genderStr}"

            // 3. Fetch the real event from the Dictionary so the engine knows the base times!
            val individualLegEvent = EventDictionary.getEventById(realIndividualEventId)
                ?: SwimmingEvent(
                    id = realIndividualEventId,
                    distance = relayEvent.distance,
                    stroke = strokeForThisLeg,
                    // Adjust this based on how your Gender is defined (String vs Enum)
                    gender = if (genderStr == "M") Gender.M else Gender.F
                )

            // 4. Simulate the base time
            var splitTime = simulateRace(swimmer, individualLegEvent, isShortCourse)

            // 5. Apply the Flying Start Bonus (Swimmers 2, 3, and 4)
            if (i > 0) {
                // A typical flying start saves between 0.50s (500ms) and 0.70s (700ms)
                val flyingStartBonus = (500..700).random()
                splitTime -= flyingStartBonus
            }

            splits.add(splitTime)
        }

        return splits
    }
}