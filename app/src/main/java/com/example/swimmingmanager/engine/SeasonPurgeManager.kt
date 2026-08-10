package com.example.swimmingmanager.engine

import androidx.room.withTransaction
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.PersonalBest
import com.example.swimmingmanager.data.RaceRecord
import com.example.swimmingmanager.data.SeasonBestLight
import com.example.swimmingmanager.data.MeetBestResult
import com.example.swimmingmanager.data.SeasonBest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SeasonPurgeManager {

    /**
     * Esegue la pulizia del database di fine stagione.
     * Salva Personal Best e Record mondiali/nazionali/manifestazione in modo permanente
     * prima di eliminare i risultati grezzi delle singole gare.
     * Ottimizzato per l'esecuzione in pochi secondi tramite caching in RAM.
     */
    suspend fun executeEndSeasonCleanup(db: AppDatabase, completedYear: Int) {
        withContext(Dispatchers.IO) {
            
            // 1. CARICAMENTO DATI IN RAM (Turbo Mode)
            // Recuperiamo i migliori tempi stagionali e della manifestazione
            val seasonBestsLight: List<SeasonBestLight> = db.raceResultDao().getOptimizedSeasonBests(completedYear)
            val meetBests: List<MeetBestResult> = db.raceResultDao().getBestResultsByMeet(completedYear)
            
            // Carichiamo dizionari e record attuali per confronti istantanei
            val allSwimmersMap = db.swimmerDao().getAllSwimmers().associateBy { it.id }
            val allClubsMap = db.clubDao().getAllClubs().associateBy { it.id }
            val historicalPBs = db.personalBestDao().getAllPBs()
            val pbMap = historicalPBs.associateBy { "${it.swimmerId}_${it.eventId}_${it.isShortCourse}" }.toMutableMap()
            
            val historicalRecords = db.raceRecordDao().getAllRecords()
            val recordMap = historicalRecords.associateBy { 
                "${it.eventId}_${it.isShortCourse}_${it.recordType}_${it.tag}" 
            }.toMutableMap()

            val pbsToSave = mutableListOf<PersonalBest>()
            val recordsToSave = mutableListOf<RaceRecord>()
            val seasonBestsToSave = mutableListOf<SeasonBest>()

            // 2. ELABORAZIONE RECORD PERSONALI E GLOBALI
            for (sb in seasonBestsLight) {
                // Save to SeasonBests table for qualification logic next year
                seasonBestsToSave.add(SeasonBest(
                    swimmerId = sb.swimmerId,
                    eventId = sb.eventId,
                    isShortCourse = sb.isShortCourse,
                    timeMs = sb.timeMs,
                    year = completedYear
                ))

                val pbKey = "${sb.swimmerId}_${sb.eventId}_${sb.isShortCourse}"
                val existingPB = pbMap[pbKey]
                
                // Aggiornamento Personal Best
                if (existingPB == null || sb.timeMs < existingPB.timeMs) {
                    val newPB = PersonalBest(
                        swimmerId = sb.swimmerId, 
                        eventId = sb.eventId, 
                        isShortCourse = sb.isShortCourse, 
                        timeMs = sb.timeMs, 
                        year = completedYear
                    )
                    pbsToSave.add(newPB)
                    pbMap[pbKey] = newPB
                }

                // Aggiornamento Record del Mondo e Nazionali
                allSwimmersMap[sb.swimmerId]?.let { swimmer ->
                    // World Record
                    val wrKey = "${sb.eventId}_${sb.isShortCourse}_WORLD_ALL"
                    if (recordMap[wrKey] == null || sb.timeMs < recordMap[wrKey]!!.timeMs) {
                        val newWR = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "WORLD", tag = "ALL", swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                        recordsToSave.add(newWR)
                        recordMap[wrKey] = newWR
                    }

                    // National Record
                    val nation = swimmer.nationality ?: ""
                    if (nation.isNotEmpty()) {
                        val nrKey = "${sb.eventId}_${sb.isShortCourse}_NATIONAL_$nation"
                        if (recordMap[nrKey] == null || sb.timeMs < recordMap[nrKey]!!.timeMs) {
                            val newNR = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "NATIONAL", tag = nation, swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                            recordsToSave.add(newNR)
                            recordMap[nrKey] = newNR
                        }
                    }
                    
                    // Regional Record
                    allClubsMap[swimmer.clubId]?.let { club ->
                        val rrKey = "${sb.eventId}_${sb.isShortCourse}_REGIONAL_${club.region}"
                        if (recordMap[rrKey] == null || sb.timeMs < recordMap[rrKey]!!.timeMs) {
                            val r = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "REGIONAL", tag = club.region, swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                            recordsToSave.add(r)
                            recordMap[rrKey] = r
                        }
                    }
                }
            }

            // 3. ELABORAZIONE RECORD DELLA MANIFESTAZIONE (CR)
            for (mb in meetBests) {
                val mrKey = "${mb.eventId}_${mb.isShortCourse}_MEET_${mb.competitionName}"
                if (recordMap[mrKey] == null || mb.timeMs < recordMap[mrKey]!!.timeMs) {
                    val newMR = RaceRecord(
                        eventId = mb.eventId, 
                        isShortCourse = mb.isShortCourse, 
                        recordType = "MEET", 
                        tag = mb.competitionName, 
                        swimmerId = mb.swimmerId, 
                        timeMs = mb.timeMs, 
                        year = completedYear
                    )
                    recordsToSave.add(newMR)
                    recordMap[mrKey] = newMR
                }
            }

            // 4. SCRITTURA FINALE E PULIZIA (Atomic Transaction)
            db.withTransaction {
                if (pbsToSave.isNotEmpty()) db.personalBestDao().insertOrUpdatePersonalBests(pbsToSave)
                if (recordsToSave.isNotEmpty()) db.raceRecordDao().insertRecords(recordsToSave)
                if (seasonBestsToSave.isNotEmpty()) db.seasonBestDao().insertSeasonBests(seasonBestsToSave)
                
                db.raceRegistrationDao().clearAllRegistrations()
                db.raceResultDao().purgeAllPastIndividualResults(completedYear + 1)
                db.raceResultDao().purgeAllPastRelayResults(completedYear + 1)
                
                // Keep only the last 2 years of season bests to avoid DB bloating
                db.seasonBestDao().purgeOldSeasonBests(completedYear - 1)
            }
        }
    }
}
