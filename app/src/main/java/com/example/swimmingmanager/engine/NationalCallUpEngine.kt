package com.example.swimmingmanager.engine

import androidx.room.withTransaction
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.Message
import com.example.swimmingmanager.data.RaceRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NationalCallupEngine {

    suspend fun processInternationalCallups(db: AppDatabase, meet: Competition, currentYear: Int, playerClubId: Int) {
        withContext(Dispatchers.Default) {

            val candidates = db.raceResultDao().getCallupCandidates(currentYear, meet.isShortCourse)
            if (candidates.isEmpty()) return@withContext

            val europeanNations = listOf("ITA", "GBR", "FRA", "GER", "HUN", "NED", "ESP", "SWE", "ROU", "LTU", "BEL", "SUI", "RUS")
            val isEuropeanMeet = meet.name.contains("European", ignoreCase = true)
            val isIntercontinentalMeet = meet.name.contains("Intercontinental", ignoreCase = true)

            val newRegistrations = mutableListOf<RaceRegistration>()
            val messagesToInsert = mutableListOf<Message>()

            val groupedByEvent = candidates.groupBy { it.eventId }

            for ((eventId, eventResults) in groupedByEvent) {
                val byNation = eventResults.groupBy { it.nationality }

                for ((nationCode, nationResults) in byNation) {
                    if (isEuropeanMeet && !europeanNations.contains(nationCode)) continue
                    if (isIntercontinentalMeet && europeanNations.contains(nationCode)) continue

                    val is50FreeRelayBase = eventId.startsWith("50_FREE") && meet.isShortCourse
                    val isLongerFreeRelayBase = eventId.startsWith("100_FREE") || eventId.startsWith("200_FREE")
                    val callupLimit = if (is50FreeRelayBase || isLongerFreeRelayBase) 4 else 2

                    val topSwimmers = nationResults
                        .filter { it.age in meet.minAge..meet.maxAge }
                        .sortedBy { it.timeMs }
                        .distinctBy { it.swimmerId }
                        .take(callupLimit)

                    topSwimmers.forEachIndexed { index, candidate ->
                        val registrationEventCode = if (index >= 2) {
                            val genderSuffix = if (eventId.endsWith("_M")) "_RELAY_M" else "_RELAY_F"
                            val distancePart = eventId.substringBeforeLast("_")
                            "4x${distancePart}${genderSuffix}"
                        } else {
                            eventId
                        }

                        newRegistrations.add(RaceRegistration(
                            swimmerId = candidate.swimmerId,
                            competitionId = meet.id,
                            eventCode = registrationEventCode,
                            week = meet.week
                        ))

                        // GENERA MESSAGGIO SE È UN ATLETA DEL GIOCATORE
                        if (candidate.clubId == playerClubId) {
                            val eventName = EventDictionary.getEventById(eventId)?.getDisplayName() ?: eventId
                            messagesToInsert.add(Message(
                                title = "Convocazione Nazionale!",
                                body = "Ottime notizie! Il tuo atleta è stato convocato in nazionale per gareggiare nei $eventName a ${meet.name}.",
                                type = "CALLUP",
                                year = currentYear,
                                week = meet.week
                            ))
                        }
                    }
                }
            }

            withContext(Dispatchers.IO) {
                db.withTransaction {
                    newRegistrations.chunked(1000).forEach { db.raceRegistrationDao().insertRegistrations(it) }
                    messagesToInsert.forEach { db.messageDao().insertMessage(it) }
                }
            }
        }
    }
}
