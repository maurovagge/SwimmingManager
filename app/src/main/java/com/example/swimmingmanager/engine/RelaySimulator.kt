package com.example.swimmingmanager.engine

import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RelayResult
import com.example.swimmingmanager.data.RelayRecord
import com.example.swimmingmanager.data.Swimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RelaySimulator {

    suspend fun simulateRelaysForCompetition(
        db: AppDatabase,
        competition: Competition,
        swimmersAtMeet: List<Swimmer>,
        isInternational: Boolean,
        currentYear: Int,
        playerClubId: Int
    ) {
        withContext(Dispatchers.Default) {
            val isRegionalFinal = competition.tier == "REGIONAL" && (competition.week == 22 || competition.week == 44)
            val isOtherTier = competition.tier != "REGIONAL"
            if (!(isRegionalFinal || isOtherTier)) return@withContext

            val generatedResults = mutableListOf<RelayResult>()
            val alreadySimulatedEvents = withContext(Dispatchers.IO) {
                db.raceResultDao().getRelayResultsForCompetition(competition.id)
                    .map { it.eventId }.toSet()
            }
            val clubsMap = withContext(Dispatchers.IO) {
                db.clubDao().getAllClubs().associateBy { it.id }
            }

            val relayEventsToSimulate = mutableListOf(
                "4x100_FREE_RELAY_M", "4x200_FREE_RELAY_M", "4x100_MEDLEY_RELAY_M",
                "4x100_FREE_RELAY_F", "4x200_FREE_RELAY_F", "4x100_MEDLEY_RELAY_F"
            )
            if (competition.isShortCourse) {
                relayEventsToSimulate.addAll(listOf("4x50_FREE_RELAY_M", "4x50_MEDLEY_RELAY_M", "4x50_FREE_RELAY_F", "4x50_MEDLEY_RELAY_F"))
            }

            val eligibleSwimmersForMeet = swimmersAtMeet.distinctBy { it.id }.filter { it.age in competition.minAge..competition.maxAge }
            val teamsRoster = if (isInternational) eligibleSwimmersForMeet.groupBy { it.nationality } else eligibleSwimmersForMeet.groupBy { it.clubId.toString() }

            for (eventId in relayEventsToSimulate) {
                if (alreadySimulatedEvents.contains(eventId)) continue
                val isMenEvent = eventId.endsWith("_M")
                val isMedley = eventId.contains("MEDLEY")
                val distance = if (eventId.contains("50")) 50 else if (eventId.contains("100")) 100 else 200
                val targetGenderString = if (isMenEvent) "M" else "F"
                val dummyEvent = SwimmingEvent(id = eventId, distance = distance, stroke = if (isMedley) Stroke.MEDLEY else Stroke.FREESTYLE, gender = if (isMenEvent) Gender.M else Gender.F)

                for ((teamKey, fullRoster) in teamsRoster) {
                    if (teamKey == null) continue
                    val genderMatchedSwimmers = fullRoster.filter { it.gender == targetGenderString }
                    val teamId = if (isInternational) 0 else teamKey.toInt()
                    val teamName = if (isInternational) teamKey else clubsMap[teamId]?.name ?: "Club $teamId"
                    val isPlayerTeam = !isInternational && teamId == playerClubId

                    val relayTeam = if (isPlayerTeam) {
                        val playerRegs = db.raceRegistrationDao().getRegistrationsForCompetition(competition.id).filter { it.eventCode == eventId }
                        if (playerRegs.size == 4) playerRegs.mapNotNull { reg -> genderMatchedSwimmers.find { it.id == reg.swimmerId } } else null
                    } else RelayTeamSelector.selectTeamForRelay(genderMatchedSwimmers, eventId)

                    if (relayTeam != null && relayTeam.size == 4) {
                        // FIX: Apply AI Tapering. 
                        // AI teams (and all teams in international competitions) should compete at their best.
                        // Player swimmers in club competitions maintain their current tiredness.
                        val taperedTeam = if (isPlayerTeam) {
                            relayTeam
                        } else {
                            relayTeam.map { swimmer ->
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
                        generatedResults.add(RelayResult(
                            competitionId = competition.id, eventId = eventId, teamId = teamId, teamName = teamName,
                            totalTimeMs = splits.sum(), swimmer1Id = relayTeam[0].id, swimmer1SplitMs = splits[0],
                            swimmer2Id = relayTeam[1].id, swimmer2SplitMs = splits[1], swimmer3Id = relayTeam[2].id,
                            swimmer3SplitMs = splits[2], swimmer4Id = relayTeam[3].id, swimmer4SplitMs = splits[3],
                            year = currentYear, isShortCourse = competition.isShortCourse
                        ))
                    }
                }
            }

            if (generatedResults.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    db.raceResultDao().insertRelayResults(generatedResults)
                    
                    val existingRecords = db.relayRecordDao().getAllRelayRecords()
                        .associateBy { "${it.eventId}_${it.isShortCourse}_${it.recordType}_${it.tag}" }
                        .toMutableMap()
                    
                    val finalRecordsToSave = mutableMapOf<String, RelayRecord>()

                    // FIX FONDAMENTALE: Ordiniamo i risultati per tempo crescente (dal più veloce al più lento)
                    // In questo modo, il primo che processiamo per un certo recordType/tag sarà SEMPRE il vincitore
                    for (res in generatedResults.sortedBy { it.totalTimeMs }) {
                        
                        // 1. Meet Record (Manifestazione)
                        val mrKey = "${res.eventId}_${res.isShortCourse}_MEET_${competition.name}"
                        if (res.totalTimeMs < (existingRecords[mrKey]?.timeMs ?: Int.MAX_VALUE)) {
                            val newRec = createRelayRecord(res, "MEET", competition.name)
                            finalRecordsToSave[mrKey] = newRec
                            existingRecords[mrKey] = newRec // Aggiorniamo la cache locale così i successivi (più lenti) falliscono il test
                        }

                        if (isInternational) {
                            // 2. World Record (Solo gare internazionali)
                            val wrKey = "${res.eventId}_${res.isShortCourse}_WORLD_WORLD"
                            if (res.totalTimeMs < (existingRecords[wrKey]?.timeMs ?: Int.MAX_VALUE)) {
                                val newRec = createRelayRecord(res, "WORLD", "WORLD")
                                finalRecordsToSave[wrKey] = newRec
                                existingRecords[wrKey] = newRec
                            }
                        } else {
                            // 3. National & Regional Records (Solo club)
                            val club = clubsMap[res.teamId]
                            if (club != null) {
                                val nrKey = "${res.eventId}_${res.isShortCourse}_NATIONAL_${club.country}"
                                if (res.totalTimeMs < (existingRecords[nrKey]?.timeMs ?: Int.MAX_VALUE)) {
                                    val newRec = createRelayRecord(res, "NATIONAL", club.country)
                                    finalRecordsToSave[nrKey] = newRec
                                    existingRecords[nrKey] = newRec
                                }

                                val rrKey = "${res.eventId}_${res.isShortCourse}_REGIONAL_${club.region}"
                                if (res.totalTimeMs < (existingRecords[rrKey]?.timeMs ?: Int.MAX_VALUE)) {
                                    val newRec = createRelayRecord(res, "REGIONAL", club.region)
                                    finalRecordsToSave[rrKey] = newRec
                                    existingRecords[rrKey] = newRec
                                }
                            }
                        }
                    }

                    if (finalRecordsToSave.isNotEmpty()) {
                        db.relayRecordDao().insertRecords(finalRecordsToSave.values.toList())
                    }
                }
            }
        }
    }

    private fun createRelayRecord(res: RelayResult, type: String, tag: String): RelayRecord {
        return RelayRecord(
            eventId = res.eventId, isShortCourse = res.isShortCourse, recordType = type, tag = tag,
            teamId = res.teamId, teamName = res.teamName, swimmer1Id = res.swimmer1Id, swimmer1SplitMs = res.swimmer1SplitMs,
            swimmer2Id = res.swimmer2Id, swimmer2SplitMs = res.swimmer2SplitMs, swimmer3Id = res.swimmer3Id,
            swimmer3SplitMs = res.swimmer3SplitMs, swimmer4Id = res.swimmer4Id, swimmer4SplitMs = res.swimmer4SplitMs,
            timeMs = res.totalTimeMs, year = res.year
        )
    }
}
