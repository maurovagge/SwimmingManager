package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.Swimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object YearZeroSimulator {

    /**
     * Simulates a past season for all swimmers in parallel.
     * Dramatically faster on multi-core devices.
     */
    suspend fun simulatePastSeason(swimmers: List<Swimmer>, pastYear: Int): List<RaceResult> = coroutineScope {
        // Divide swimmers into chunks to process them in parallel
        val chunks = swimmers.chunked(swimmers.size / 4 + 1)
        
        chunks.map { chunk ->
            async(Dispatchers.Default) {
                val chunkResults = mutableListOf<RaceResult>()
                val allEvents = EventDictionary.standardEvents

                for (swimmer in chunk) {
                    val stats = mapOf(
                        Stroke.FREESTYLE to swimmer.freestyle,
                        Stroke.BACKSTROKE to swimmer.backstroke,
                        Stroke.BREASTSTROKE to swimmer.breaststroke,
                        Stroke.BUTTERFLY to swimmer.butterfly
                    )

                    val topStrokes = stats.toList().sortedByDescending { it.second }.take(2).map { it.first }

                    val eligibleEvents = allEvents.filter { event ->
                        event.gender.name == swimmer.gender &&
                                event.type == EventType.INDIVIDUAL &&
                                (topStrokes.contains(event.stroke) || event.stroke == Stroke.MEDLEY)
                    }

                    for (event in eligibleEvents) {
                        // Skip long distances for lower tier swimmers to save time
                        if (event.distance > 400 && swimmer.overall < 70) continue

                        // Simulate Long Course
                        if (!event.id.contains("100_MEDLEY")) {
                            chunkResults.add(createResult(swimmer.id, event.id, false, RaceEngine.simulateRace(swimmer, event, false), pastYear, 24))
                        }

                        // Simulate Short Course
                        chunkResults.add(createResult(swimmer.id, event.id, true, RaceEngine.simulateRace(swimmer, event, true), pastYear, 52))
                    }
                }
                chunkResults
            }
        }.awaitAll().flatten()
    }

    private fun createResult(sId: Int, eId: String, sc: Boolean, time: Int, year: Int, week: Int) = RaceResult(
        swimmerId = sId,
        competitionId = -1,
        eventId = eId,
        isShortCourse = sc,
        timeMs = time,
        week = week,
        year = year,
        roundType = "TIMED_FINAL"
    )
}
