package com.example.swimmingmanager.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.swimmingmanager.data.AppDatabase
import com.example.swimmingmanager.data.Club
import com.example.swimmingmanager.data.Competition
import com.example.swimmingmanager.data.RaceRecord
import com.example.swimmingmanager.data.RaceResult
import com.example.swimmingmanager.data.RelayResult
import com.example.swimmingmanager.data.RelayRecord
import com.example.swimmingmanager.data.Swimmer
import com.example.swimmingmanager.engine.LiveRaceEngine
import com.example.swimmingmanager.engine.TournamentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Defines the 4 possible phases of a live meet
enum class LiveMeetPhase {
    INITIALIZING,
    READY_TO_RACE,
    SHOWING_RESULTS,
    MEET_FINISHED
}

class LiveMeetState(
    private val competition: Competition,
    private val playerClubId: Int,
    private val db: AppDatabase,
    private val currentYear: Int,
    private val coroutineScope: CoroutineScope
) {
    var phase by mutableStateOf(LiveMeetPhase.INITIALIZING)

    // Queue management
    var playerEvents = listOf<String>()
    var currentEventIndex by mutableIntStateOf(0)

    // Current Race Status
    var currentEventId by mutableStateOf("")
    var currentRound by mutableStateOf("")
    var activeSwimmerIds = listOf<Int>()

    // Results state
    var currentResults by mutableStateOf<List<RaceResult>>(emptyList())
    var currentRelayResults by mutableStateOf<List<RelayResult>>(emptyList())

    // --- RECORDS STATE ---
    var worldRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var nationalRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var regionalRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var meetRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())

    // --- RELAY RECORDS STATE ---
    var relayWorldRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var relayNationalRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var relayRegionalRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())
    var relayMeetRecordsMap by mutableStateOf<Map<String, Int>>(emptyMap())

    private var allSwimmersMap = mapOf<Int, Swimmer>()
    private var allClubsMap = mapOf<Int, Club>()

    // Tracked context for real-time record updates
    private var trackedNation: String = ""
    private var trackedRegion: String = ""

    /**
     * Initializes the meet state by loading all reference records.
     */
    fun initialize(clubNation: String, clubRegion: String) {
        coroutineScope.launch(Dispatchers.IO) {
            trackedNation = clubNation
            trackedRegion = clubRegion

            val swimmers = db.swimmerDao().getAllSwimmers()
            allSwimmersMap = swimmers.associateBy { it.id }

            val clubs = db.clubDao().getAllClubs()
            allClubsMap = clubs.associateBy { it.id }

            val isShort = competition.isShortCourse

            // Individual Records
            val wrs = db.raceRecordDao().getWorldRecordsList(isShort)
            val nrs = db.raceRecordDao().getNationalRecordsList(isShort, clubNation)
            val rrs = db.raceRecordDao().getRegionalRecordsList(isShort, clubRegion)
            val mrs = db.raceRecordDao().getMeetRecords(competition.name)

            // Relay Records
            val rwrs = db.relayRecordDao().getRecordsByType("WORLD", "WORLD")
            val rnrs = db.relayRecordDao().getRecordsByType("NATIONAL", clubNation)
            val rrrs = db.relayRecordDao().getRecordsByType("REGIONAL", clubRegion)
            val rmrs = db.relayRecordDao().getRecordsByType("MEET", competition.name)

            withContext(Dispatchers.Main) {
                worldRecordsMap = wrs.associate { it.eventId to it.timeMs }
                nationalRecordsMap = nrs.associate { it.eventId to it.timeMs }
                regionalRecordsMap = rrs.associate { it.eventId to it.timeMs }
                meetRecordsMap = mrs.associate { it.eventId to it.timeMs }

                relayWorldRecordsMap = rwrs.associate { it.eventId to it.timeMs }
                relayNationalRecordsMap = rnrs.associate { it.eventId to it.timeMs }
                relayRegionalRecordsMap = rrrs.associate { it.eventId to it.timeMs }
                relayMeetRecordsMap = rmrs.associate { it.eventId to it.timeMs }
            }

            val allRegs = db.raceRegistrationDao().getRegistrationsForCompetition(competition.id)
            playerEvents = allRegs.filter { reg ->
                allSwimmersMap[reg.swimmerId]?.clubId == playerClubId
            }.map { it.eventCode }.distinct().sorted()

            if (playerEvents.isEmpty()) {
                withContext(Dispatchers.Main) { phase = LiveMeetPhase.MEET_FINISHED }
            } else {
                setupEvent(0)
                withContext(Dispatchers.Main) { phase = LiveMeetPhase.READY_TO_RACE }
            }
        }
    }

    private suspend fun setupEvent(index: Int) {
        currentEventIndex = index
        currentEventId = playerEvents[index]

        val roundSeq = TournamentManager.getRoundSequence(currentEventId, competition.tier)
        currentRound = roundSeq.first()

        val eventRegs = db.raceRegistrationDao().getRegistrationsForCompetition(competition.id)
            .filter { it.eventCode == currentEventId }
        activeSwimmerIds = eventRegs.map { it.swimmerId }
    }

    fun simulateCurrentRound() {
        coroutineScope.launch(Dispatchers.IO) {
            val isRelay = currentEventId.startsWith("4x")

            if (isRelay) {
                val playerRegs = db.raceRegistrationDao().getRegistrationsForCompetition(competition.id)
                    .filter { it.eventCode == currentEventId && allSwimmersMap[it.swimmerId]?.clubId == playerClubId }

                val playerSwimmers = playerRegs.mapNotNull { allSwimmersMap[it.swimmerId] }

                val relayResults = LiveRaceEngine.simulateInteractiveRelayRound(
                    competition = competition,
                    eventId = currentEventId,
                    roundType = currentRound,
                    playerSwimmers = playerSwimmers,
                    playerClubId = playerClubId,
                    db = db,
                    currentYear = currentYear
                )

                withContext(Dispatchers.Main) {
                    currentRelayResults = relayResults
                    currentResults = emptyList()
                    phase = LiveMeetPhase.SHOWING_RESULTS
                }
                
                checkAndRecordRelayMilestones(relayResults)
            } else {
                val rawSwimmers = activeSwimmerIds.mapNotNull { allSwimmersMap[it] }
                val swimmersOnBlocks = applyAiTaper(rawSwimmers)

                val results = LiveRaceEngine.simulateInteractiveRound(
                    competition = competition,
                    eventId = currentEventId,
                    roundType = currentRound,
                    activeSwimmers = swimmersOnBlocks,
                    db = db,
                    currentYear = currentYear
                )

                withContext(Dispatchers.Main) {
                    currentResults = results
                    currentRelayResults = emptyList()
                    phase = LiveMeetPhase.SHOWING_RESULTS
                }

                checkAndRecordMilestones(results)
            }
        }
    }

    fun advanceToNextRoundOrEvent() {
        coroutineScope.launch(Dispatchers.IO) {
            val isRelay = currentEventId.startsWith("4x")

            if (!isRelay) {
                val roundSeq = TournamentManager.getRoundSequence(currentEventId, competition.tier)
                val currentRoundIndex = roundSeq.indexOf(currentRound)
                val nextRound = roundSeq.getOrNull(currentRoundIndex + 1)

                if (nextRound != null) {
                    val qualifiers = LiveRaceEngine.getQualifiersForNextRound(currentResults, currentRound, nextRound)
                    val playerStillAlive = qualifiers.any { allSwimmersMap[it]?.clubId == playerClubId }

                    if (playerStillAlive) {
                        activeSwimmerIds = qualifiers
                        currentRound = nextRound
                        withContext(Dispatchers.Main) { phase = LiveMeetPhase.READY_TO_RACE }
                        return@launch
                    } else {
                        silentSimulateRemainingRounds(qualifiers, nextRound, roundSeq)
                    }
                }
            }

            val nextEventIndex = currentEventIndex + 1
            if (nextEventIndex < playerEvents.size) {
                setupEvent(nextEventIndex)
                withContext(Dispatchers.Main) { phase = LiveMeetPhase.READY_TO_RACE }
            } else {
                withContext(Dispatchers.Main) { phase = LiveMeetPhase.MEET_FINISHED }
            }
        }
    }

    private suspend fun silentSimulateRemainingRounds(
        initialQualifiers: List<Int>,
        startingRound: String,
        fullSequence: List<String>
    ) {
        var currentActive = initialQualifiers
        val startIndex = fullSequence.indexOf(startingRound)

        for (i in startIndex until fullSequence.size) {
            val round = fullSequence[i]
            val nextRnd = fullSequence.getOrNull(i + 1)

            val rawSwimmers = currentActive.mapNotNull { allSwimmersMap[it] }
            val swimmersToSimulate = applyAiTaper(rawSwimmers)

            val results = LiveRaceEngine.simulateInteractiveRound(
                competition = competition,
                eventId = currentEventId,
                roundType = round,
                activeSwimmers = swimmersToSimulate,
                db = db,
                currentYear = currentYear
            )

            checkAndRecordMilestones(results)

            if (nextRnd != null) {
                currentActive = LiveRaceEngine.getQualifiersForNextRound(results, round, nextRnd)
            }
        }
    }

    private suspend fun checkAndRecordMilestones(results: List<RaceResult>) {
        if (results.isEmpty()) return
        val newRecordsToSave = mutableListOf<RaceRecord>()
        val updatedWRs = worldRecordsMap.toMutableMap()
        val updatedNRs = nationalRecordsMap.toMutableMap()
        val updatedRRs = regionalRecordsMap.toMutableMap()
        val updatedMRs = meetRecordsMap.toMutableMap()
        var anyChange = false

        for (res in results) {
            val swimmer = allSwimmersMap[res.swimmerId] ?: continue
            val club = allClubsMap[swimmer.clubId]

            if (res.timeMs < (updatedWRs[res.eventId] ?: Int.MAX_VALUE)) {
                val r = RaceRecord(eventId = res.eventId, isShortCourse = res.isShortCourse, recordType = "WORLD", tag = "ALL", swimmerId = res.swimmerId, timeMs = res.timeMs, year = currentYear)
                newRecordsToSave.add(r)
                updatedWRs[res.eventId] = res.timeMs
                anyChange = true
            }

            if (res.timeMs < (updatedMRs[res.eventId] ?: Int.MAX_VALUE)) {
                val r = RaceRecord(eventId = res.eventId, isShortCourse = res.isShortCourse, recordType = "MEET", tag = competition.name, swimmerId = res.swimmerId, timeMs = res.timeMs, year = currentYear)
                newRecordsToSave.add(r)
                updatedMRs[res.eventId] = res.timeMs
                anyChange = true
            }

            if (swimmer.nationality == trackedNation && res.timeMs < (updatedNRs[res.eventId] ?: Int.MAX_VALUE)) {
                val r = RaceRecord(eventId = res.eventId, isShortCourse = res.isShortCourse, recordType = "NATIONAL", tag = trackedNation, swimmerId = res.swimmerId, timeMs = res.timeMs, year = currentYear)
                newRecordsToSave.add(r)
                updatedNRs[res.eventId] = res.timeMs
                anyChange = true
            }

            if (club?.region == trackedRegion && res.timeMs < (updatedRRs[res.eventId] ?: Int.MAX_VALUE)) {
                val r = RaceRecord(eventId = res.eventId, isShortCourse = res.isShortCourse, recordType = "REGIONAL", tag = trackedRegion, swimmerId = res.swimmerId, timeMs = res.timeMs, year = currentYear)
                newRecordsToSave.add(r)
                updatedRRs[res.eventId] = res.timeMs
                anyChange = true
            }
        }

        if (anyChange) {
            withContext(Dispatchers.Main) {
                worldRecordsMap = updatedWRs
                nationalRecordsMap = updatedNRs
                regionalRecordsMap = updatedRRs
                meetRecordsMap = updatedMRs
            }
            db.raceRecordDao().insertRecords(newRecordsToSave)
        }
    }

    private suspend fun checkAndRecordRelayMilestones(results: List<RelayResult>) {
        if (results.isEmpty()) return
        val newRecordsToSave = mutableListOf<RelayRecord>()
        
        val updatedWRs = relayWorldRecordsMap.toMutableMap()
        val updatedNRs = relayNationalRecordsMap.toMutableMap()
        val updatedRRs = relayRegionalRecordsMap.toMutableMap()
        val updatedMRs = relayMeetRecordsMap.toMutableMap()
        var anyChange = false

        val isInternational = competition.tier == "WORLD" || competition.tier == "INTERNATIONAL" || competition.tier == "CONTINENTAL"

        for (res in results) {
            // Meet Record
            if (res.totalTimeMs < (updatedMRs[res.eventId] ?: Int.MAX_VALUE)) {
                val r = createRelayRecord(res, "MEET", competition.name)
                newRecordsToSave.add(r)
                updatedMRs[res.eventId] = res.totalTimeMs
                anyChange = true
            }

            if (isInternational) {
                // World Record
                if (res.totalTimeMs < (updatedWRs[res.eventId] ?: Int.MAX_VALUE)) {
                    val r = createRelayRecord(res, "WORLD", "WORLD")
                    newRecordsToSave.add(r)
                    updatedWRs[res.eventId] = res.totalTimeMs
                    anyChange = true
                }
            } else {
                val club = allClubsMap[res.teamId]
                if (club != null) {
                    // National Record
                    if (club.country == trackedNation && res.totalTimeMs < (updatedNRs[res.eventId] ?: Int.MAX_VALUE)) {
                        val r = createRelayRecord(res, "NATIONAL", trackedNation)
                        newRecordsToSave.add(r)
                        updatedNRs[res.eventId] = res.totalTimeMs
                        anyChange = true
                    }
                    // Regional Record
                    if (club.region == trackedRegion && res.totalTimeMs < (updatedRRs[res.eventId] ?: Int.MAX_VALUE)) {
                        val r = createRelayRecord(res, "REGIONAL", trackedRegion)
                        newRecordsToSave.add(r)
                        updatedRRs[res.eventId] = res.totalTimeMs
                        anyChange = true
                    }
                }
            }
        }

        if (anyChange) {
            withContext(Dispatchers.Main) {
                relayWorldRecordsMap = updatedWRs
                relayNationalRecordsMap = updatedNRs
                relayRegionalRecordsMap = updatedRRs
                relayMeetRecordsMap = updatedMRs
            }
            db.relayRecordDao().insertRecords(newRecordsToSave)
        }
    }

    private fun createRelayRecord(res: RelayResult, type: String, tag: String): RelayRecord {
        return RelayRecord(
            eventId = res.eventId,
            isShortCourse = res.isShortCourse,
            recordType = type,
            tag = tag,
            teamId = res.teamId,
            teamName = res.teamName,
            swimmer1Id = res.swimmer1Id,
            swimmer1SplitMs = res.swimmer1SplitMs,
            swimmer2Id = res.swimmer2Id,
            swimmer2SplitMs = res.swimmer2SplitMs,
            swimmer3Id = res.swimmer3Id,
            swimmer3SplitMs = res.swimmer3SplitMs,
            swimmer4Id = res.swimmer4Id,
            swimmer4SplitMs = res.swimmer4SplitMs,
            timeMs = res.totalTimeMs,
            year = currentYear
        )
    }

    private fun applyAiTaper(swimmers: List<Swimmer>): List<Swimmer> {
        return swimmers.map { swimmer ->
            if (swimmer.clubId == playerClubId) return@map swimmer

            val aiTargetTiredness = when (competition.tier) {
                "INTERNATIONAL" -> 0.0
                "NATIONAL" -> 0.5
                "REGIONAL" -> 1.0
                else -> swimmer.tiredness
            }
            swimmer.copy(tiredness = aiTargetTiredness)
        }
    }

    fun getSwimmer(swimmerId: Int): Swimmer? = allSwimmersMap[swimmerId]
    fun getClubName(clubId: Int): String = allClubsMap[clubId]?.name ?: "Sconosciuto"
}
