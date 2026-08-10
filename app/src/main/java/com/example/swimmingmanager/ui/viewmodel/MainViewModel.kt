package com.example.swimmingmanager.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.swimmingmanager.data.*
import com.example.swimmingmanager.engine.*
import com.example.swimmingmanager.ui.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sharedPreferences: SharedPreferences =
        application.getSharedPreferences("SwimmingManagerPrefs", Context.MODE_PRIVATE)

    var isLoading by mutableStateOf(true)
        private set
    var clubList by mutableStateOf<List<Club>>(emptyList())
        private set
    var competitionList by mutableStateOf<List<Competition>>(emptyList())
        private set
    var mySavedClubId by mutableIntStateOf(sharedPreferences.getInt("MY_CLUB_ID", -1))
        private set
    var myClub by mutableStateOf<Club?>(null)
        private set
    var myTimeStandards by mutableStateOf<List<TimeStandard>>(emptyList())
        private set
    var activeMeet by mutableStateOf<Competition?>(null)
    var myRosterList by mutableStateOf<List<Swimmer>>(emptyList())
        private set

    val messages: StateFlow<List<Message>> = db.messageDao().getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var currentScreen by mutableStateOf<Destination>(Destination.Dashboard)
        private set
    var currentWeek by mutableIntStateOf(sharedPreferences.getInt("CURRENT_WEEK", 1))
        private set
    var currentYear by mutableIntStateOf(sharedPreferences.getInt("CURRENT_YEAR", 2026))
        private set
    var isSimulating by mutableStateOf(false)

    init { loadInitialData() }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            WorldGenerator.loadNames(getApplication())
            if (db.clubDao().getClubsCount() == 0) {
                db.withTransaction {
                    val (clubs, swimmers) = WorldGenerator.createWorldData(getApplication())
                    db.clubDao().insertAll(clubs)
                    db.swimmerDao().insertAll(swimmers)
                    val results = YearZeroSimulator.simulatePastSeason(swimmers, 2025)
                    db.raceResultDao().insertResults(results)
                    processSeasonResultsInternal(2025)
                    val swimmersLight = swimmers.map { SwimmerLight(it.id, it.nationality ?: "", it.clubId, it.gender, it.age) }
                    val lightResults = results.map { SeasonBestLight(it.swimmerId, it.eventId, it.isShortCourse, it.timeMs) }
                    db.timeStandardDao().insertStandards(QualificationEngine.generateTimeStandardsForAllNations(2026, lightResults, swimmersLight))
                }
            }
            if (mySavedClubId != -1) {
                val club = db.clubDao().getClubById(mySavedClubId)
                val roster = db.swimmerDao().getSwimmersByClub(mySavedClubId)
                var comps = db.competitionDao().getCompetitionsByYear(currentYear)
                if (comps.isEmpty() && club != null) {
                    db.competitionDao().insertCompetitions(CalendarGenerator.generateYearlyCalendar(club.country, currentYear))
                    comps = db.competitionDao().getCompetitionsByYear(currentYear)
                }
                withContext(Dispatchers.Main) { myClub = club; myRosterList = roster; competitionList = comps }
            } else {
                val allClubs = db.clubDao().getAllClubs()
                withContext(Dispatchers.Main) { clubList = allClubs }
            }
            withContext(Dispatchers.Main) { isLoading = false }
        }
    }

    private suspend fun processSeasonResultsInternal(completedYear: Int, meetBests: List<MeetBestResult> = emptyList()) {
        // 1. Update PBs using optimized SQL (Massive reduction in RAM usage)
        db.personalBestDao().updateAllPBsFromSeason(completedYear)

        // 2. Optimized fetching for Record processing
        val seasonBests = db.raceResultDao().getOptimizedSeasonBests(completedYear)
        val allSwimmersLightMap = db.swimmerDao().getAllSwimmersLight().associateBy { it.id }
        val allClubsMap = db.clubDao().getAllClubs().associateBy { it.id }
        
        val historicalRecords = db.raceRecordDao().getAllRecords()
        val recordMap = historicalRecords.associateBy { "${it.eventId}_${it.isShortCourse}_${it.recordType}_${it.tag}" }.toMutableMap()
        val recordsToSave = mutableListOf<RaceRecord>()

        for (sb in seasonBests) {
            allSwimmersLightMap[sb.swimmerId]?.let { swimmer ->
                val wrKey = "${sb.eventId}_${sb.isShortCourse}_WORLD_ALL"
                if (recordMap[wrKey] == null || sb.timeMs < recordMap[wrKey]!!.timeMs) {
                    val r = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "WORLD", tag = "ALL", swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                    recordsToSave.add(r); recordMap[wrKey] = r
                }
                val nation = swimmer.nationality
                if (nation.isNotEmpty()) {
                    val nrKey = "${sb.eventId}_${sb.isShortCourse}_NATIONAL_$nation"
                    if (recordMap[nrKey] == null || sb.timeMs < recordMap[nrKey]!!.timeMs) {
                        val r = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "NATIONAL", tag = nation, swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                        recordsToSave.add(r); recordMap[nrKey] = r
                    }
                }
                allClubsMap[swimmer.clubId]?.let { club ->
                    val rrKey = "${sb.eventId}_${sb.isShortCourse}_REGIONAL_${club.region}"
                    if (recordMap[rrKey] == null || sb.timeMs < recordMap[rrKey]!!.timeMs) {
                        val r = RaceRecord(eventId = sb.eventId, isShortCourse = sb.isShortCourse, recordType = "REGIONAL", tag = club.region, swimmerId = sb.swimmerId, timeMs = sb.timeMs, year = completedYear)
                        recordsToSave.add(r); recordMap[rrKey] = r
                    }
                }
            }
        }
        val finalMeetBests = if (meetBests.isNotEmpty()) meetBests else db.raceResultDao().getBestResultsByMeet(completedYear)
        for (mb in finalMeetBests) {
            val mrKey = "${mb.eventId}_${mb.isShortCourse}_MEET_${mb.competitionName}"
            if (recordMap[mrKey] == null || mb.timeMs < recordMap[mrKey]!!.timeMs) {
                val newMR = RaceRecord(eventId = mb.eventId, isShortCourse = mb.isShortCourse, recordType = "MEET", tag = mb.competitionName, swimmerId = mb.swimmerId, timeMs = mb.timeMs, year = completedYear)
                recordsToSave.add(newMR); recordMap[mrKey] = newMR
            }
        }
        if (recordsToSave.isNotEmpty()) db.raceRecordDao().insertRecords(recordsToSave)
    }

    fun expelSwimmer(swimmer: Swimmer) {
        viewModelScope.launch(Dispatchers.IO) {
            val nation = swimmer.nationality ?: ""
            val otherClubs = db.clubDao().getAllClubs().filter { it.id != mySavedClubId && (nation.isEmpty() || it.country == nation) }
            val targetClubs = if (otherClubs.isNotEmpty()) otherClubs else db.clubDao().getAllClubs().filter { it.id != mySavedClubId }
            
            if (targetClubs.isNotEmpty()) {
                db.swimmerDao().updateSwimmer(swimmer.copy(clubId = targetClubs.random().id))
                val roster = db.swimmerDao().getSwimmersByClub(mySavedClubId)
                withContext(Dispatchers.Main) {
                    myRosterList = roster
                    db.messageDao().insertMessage(Message(title = "Atleta Svincolato", body = "${swimmer.firstName} ${swimmer.lastName} ha lasciato la squadra.", type = "RETIREMENT", year = currentYear, week = currentWeek))
                    navigateTo(Destination.Roster)
                }
            }
        }
    }

    fun markMessageAsRead(messageId: Int) { viewModelScope.launch(Dispatchers.IO) { db.messageDao().markAsRead(messageId) } }

    fun markAllMessagesAsRead() { viewModelScope.launch(Dispatchers.IO) { db.messageDao().markAllAsRead() } }

    fun startNewGame() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoading = true }
            db.clearAllTables()
            sharedPreferences.edit { clear(); apply() }
            withContext(Dispatchers.Main) { mySavedClubId = -1; myClub = null; myRosterList = emptyList(); competitionList = emptyList(); currentWeek = 1; currentYear = 2026; currentScreen = Destination.Dashboard; activeMeet = null }
            loadInitialData()
        }
    }

    fun navigateTo(destination: Destination) { currentScreen = destination }

    fun selectClub(selectedClub: Club) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferences.edit { putInt("MY_CLUB_ID", selectedClub.id) }
            val talents = listOf(
                SwimmerGenerator.generateYoungProdigy(selectedClub.id, selectedClub.country, selectedClub.prestige, WorldGenerator.firstNamesMap, WorldGenerator.lastNamesMap),
                SwimmerGenerator.generateRisingStar(selectedClub.id, selectedClub.country, selectedClub.prestige, WorldGenerator.firstNamesMap, WorldGenerator.lastNamesMap),
                SwimmerGenerator.generateSolidVeteran(selectedClub.id, selectedClub.country, selectedClub.prestige, WorldGenerator.firstNamesMap, WorldGenerator.lastNamesMap)
            )
            val generatedIds = db.swimmerDao().insertSwimmers(talents)
            val talentIds = generatedIds.toSet()
            db.messageDao().insertMessage(Message(title = "Benvenuto, Coach!", body = "Siamo entusiasti di averti al ${selectedClub.name}.", type = "SEASON_END", year = currentYear, week = currentWeek))
            db.messageDao().insertMessage(Message(title = "Nuovi innesti", body = "Abbiamo inserito tre nuovi atleti per rinforzare la rosa.", type = "RECRUIT", year = currentYear, week = currentWeek))
            withContext(Dispatchers.Main) { mySavedClubId = selectedClub.id; myClub = selectedClub }
            val fullRoster = db.swimmerDao().getFullClubRoster(selectedClub.id)
            if (fullRoster.size > 10) {
                val core = fullRoster.filter { talentIds.contains(it.id.toLong()) }
                // Modifica richiesta: seleziona i restanti atleti basandosi sul potenziale più alto invece dell'overall
                val others = (fullRoster - core.toSet()).sortedByDescending { it.potentialPoints }.take(10 - core.size)
                val finalRoster = core + others
                
                // Fix: keep athletes in their own country when pruned from the player's club
                val otherClubs = db.clubDao().getAllClubs().filter { it.id != selectedClub.id && it.country == selectedClub.country }
                val targetClubs = if (otherClubs.isNotEmpty()) otherClubs else db.clubDao().getAllClubs().filter { it.id != selectedClub.id }
                
                db.swimmerDao().updateSwimmers((fullRoster - finalRoster.toSet()).map { it.copy(clubId = targetClubs.random().id) })
            }
            if (db.competitionDao().getCompetitionsByYear(currentYear).isEmpty()) db.competitionDao().insertCompetitions(CalendarGenerator.generateYearlyCalendar(selectedClub.country, currentYear))
            val roster = db.swimmerDao().getSwimmersByClub(selectedClub.id)
            val comps = db.competitionDao().getCompetitionsByYear(currentYear)
            withContext(Dispatchers.Main) { myRosterList = roster; competitionList = comps }
        }
    }

    fun updateTeamTraining(newT: String, newF: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = myRosterList.map { it.copy(currentTraining = newT, focusStyle = newF) }
            db.swimmerDao().updateSwimmers(updated)
            withContext(Dispatchers.Main) { myRosterList = updated }
        }
    }

    fun loadTimeStandards() {
        viewModelScope.launch(Dispatchers.IO) {
            myClub?.let { club ->
                val standards = db.timeStandardDao().getAllStandardsForNation(currentYear, club.country)
                withContext(Dispatchers.Main) { myTimeStandards = standards }
            }
        }
    }

    fun advanceWeek() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isSimulating = true }
            competitionList.filter { it.year == currentYear && (it.week == currentWeek + 1 || it.week == currentWeek + 2) && it.tier in listOf("WORLD", "CONTINENTAL", "INTERNATIONAL") }
                .forEach { if (db.raceRegistrationDao().getRegistrationsForCompetition(it.id).isEmpty()) NationalCallupEngine.processInternationalCallups(db, it, currentYear, mySavedClubId) }
            val currentComp = competitionList.find { it.week == currentWeek && it.year == currentYear }
            if (currentComp != null) {
                if (currentComp.tier in listOf("WORLD", "CONTINENTAL", "INTERNATIONAL")) {
                    if (db.raceRegistrationDao().getRegistrationsForCompetition(currentComp.id).isEmpty()) NationalCallupEngine.processInternationalCallups(db, currentComp, currentYear, mySavedClubId)
                } else AiRegistrationEngine.processAiRegistrations(currentComp, myClub!!.country, myClub!!.region, mySavedClubId, db, currentYear)
                val allRegs = db.raceRegistrationDao().getRegistrationsForCompetition(currentComp.id)
                if (allRegs.any { reg -> myRosterList.any { it.id == reg.swimmerId } }) {
                    WeekendSimulationEngine.simulateAiOnlyEvents(currentComp, allRegs, mySavedClubId, db, currentYear)
                    withContext(Dispatchers.Main) { activeMeet = currentComp; isSimulating = false }
                } else {
                    WeekendSimulationEngine.simulateAiOnlyEvents(currentComp, allRegs, -999, db, currentYear); performAdvanceTimeline()
                }
            } else performAdvanceTimeline()
        }
    }

    fun onMeetFinished() { activeMeet = null; isSimulating = true; performAdvanceTimeline() }

    private fun performAdvanceTimeline() {
        viewModelScope.launch(Dispatchers.IO) {
            competitionList.find { it.week == currentWeek && it.year == currentYear }?.let { currentComp ->
                val swimmersAtMeet = db.swimmerDao().getSwimmersByCompetition(currentComp.id)
                RelaySimulator.simulateRelaysForCompetition(db, currentComp, swimmersAtMeet, currentComp.tier in listOf("WORLD", "CONTINENTAL", "INTERNATIONAL"), currentYear, mySavedClubId)
                MedalEngine.awardMedalsFromMeet(db, db.raceResultDao().getFinalResultsForCompetition(currentComp.id), currentComp)
                MedalEngine.awardRelayMedalsFromMeet(db, db.raceResultDao().getRelayResultsForCompetition(currentComp.id), currentComp)
            }
            db.swimmerDao().updateSwimmers(TimeEngine.advanceWeek(db.swimmerDao().getAllSwimmers()))
            GhostSimulationEngine.simulateWorld(db, currentYear, currentWeek, myClub!!.country, myClub!!.region)
            var nextWeek = currentWeek + 1; var nextYear = currentYear; var updatedComps = competitionList
            if (nextWeek > 52) {
                val completedYear = currentYear
                
                // SPLIT TRANSACTION: Part 1 - Swimmer status updates
                db.withTransaction {
                    val rosterBefore = db.swimmerDao().getFullClubRoster(mySavedClubId)
                    db.swimmerDao().executeGlobalEndSeasonUpdate()
                    val rosterAfter = db.swimmerDao().getFullClubRoster(mySavedClubId)
                    rosterAfter.filter { it.isRetired }.forEach { s ->
                        if (!rosterBefore.find { it.id == s.id }!!.isRetired) db.messageDao().insertMessage(Message(title = "Ritiro Atleta", body = "${s.firstName} ${s.lastName} si è ritirato.", type = "RETIREMENT", year = completedYear, week = 52))
                    }
                }

                // Part 2 - Records and Medals (Optimized to reduce RAM)
                val lastResults = db.raceResultDao().getOptimizedSeasonBests(completedYear)
                val meetBests = db.raceResultDao().getBestResultsByMeet(completedYear)
                val swimmersLight = db.swimmerDao().getAllSwimmersLight()
                MedalEngine.distributeGhostMedals(db, completedYear, myClub!!.country, lastResults, swimmersLight)
                processSeasonResultsInternal(completedYear, meetBests)

                // Part 3 - Purge and Rebirth
                db.raceRegistrationDao().clearAllRegistrations()
                db.raceResultDao().purgeAllPastIndividualResults(completedYear + 1)
                db.raceResultDao().purgeAllPastRelayResults(completedYear + 1)
                db.messageDao().deleteAllMessages()

                nextWeek = 1; nextYear += 1
                db.swimmerDao().applyRebirthBonus()

                // Part 4 - New Season Init
                db.withTransaction {
                    RecruitmentEngine.processAnnualRecruitment(db, mySavedClubId, nextYear)
                    db.competitionDao().insertCompetitions(CalendarGenerator.generateYearlyCalendar(myClub!!.country, nextYear))
                    updatedComps = db.competitionDao().getCompetitionsByYear(nextYear)
                    QualificationEngine.generateTimeStandardsForAllNations(nextYear, lastResults, swimmersLight).chunked(2000).forEach { db.timeStandardDao().insertStandards(it) }
                    db.messageDao().insertMessage(Message(title = "Inizio Nuova Stagione!", body = "Benvenuti nell'anno $nextYear.", type = "SEASON_END", year = nextYear, week = 1))
                }

                db.query(androidx.sqlite.db.SimpleSQLiteQuery("VACUUM"))
            }
            sharedPreferences.edit { putInt("CURRENT_WEEK", nextWeek); putInt("CURRENT_YEAR", nextYear) }
            withContext(Dispatchers.Main) { currentWeek = nextWeek; currentYear = nextYear; competitionList = updatedComps; isSimulating = false; myRosterList = db.swimmerDao().getSwimmersByClub(mySavedClubId) }
        }
    }

    fun updateSwimmerProfile(updated: Swimmer) {
        viewModelScope.launch(Dispatchers.IO) {
            db.swimmerDao().updateSwimmer(updated)
            val roster = db.swimmerDao().getSwimmersByClub(mySavedClubId)
            withContext(Dispatchers.Main) { myRosterList = roster; if (currentScreen is Destination.SwimmerProfile) currentScreen = Destination.SwimmerProfile(updated) }
        }
    }
}
