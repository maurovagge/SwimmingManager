package com.example.swimmingmanager.data

import androidx.room.*

data class CallupCandidate(
    val swimmerId: Int,
    val eventId: String,
    val timeMs: Int,
    val nationality: String,
    val age: Int,
    val clubId: Int // Added clubId to identify player's swimmers for the Inbox
)

data class MeetBestResult(
    val swimmerId: Int,
    val eventId: String,
    val isShortCourse: Boolean,
    val timeMs: Int,
    val competitionName: String
)

@Dao
interface RaceResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: RaceResult)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<RaceResult>)

    @Query("SELECT * FROM race_results WHERE eventId = :eventId AND year = :year ORDER BY timeMs ASC")
    suspend fun getRankingForEvent(eventId: String, year: Int): List<RaceResult>

    @Query("""
    SELECT r.swimmerId, r.eventId, r.timeMs, s.nationality, s.age, s.clubId 
    FROM race_results r
    JOIN swimmers s ON r.swimmerId = s.id
    WHERE r.year = :year AND r.isShortCourse = :isShort AND s.isRetired = 0
""")
    suspend fun getCallupCandidates(year: Int, isShort: Boolean): List<CallupCandidate>

    @Query("SELECT MIN(timeMs) FROM race_results WHERE swimmerId = :swimmerId AND eventId = :eventId")
    suspend fun getPersonalBest(swimmerId: Int, eventId: String): Int?

    @Query("SELECT * FROM race_results WHERE swimmerId = :swimmerId ORDER BY year DESC, week DESC")
    suspend fun getResultsForSwimmer(swimmerId: Int): List<RaceResult>

    @Query("SELECT MIN(timeMs) FROM race_results WHERE swimmerId = :swimmerId AND eventId = :eventId AND isShortCourse = :isShortCourse AND year >= :minYear")
    suspend fun getSeasonBest(swimmerId: Int, eventId: String, isShortCourse: Boolean, minYear: Int): Int?

    @Query("SELECT COUNT(DISTINCT swimmerId) + 1 FROM race_results WHERE eventId = :eventId AND isShortCourse = :isShortCourse AND year >= :minYear AND timeMs < :timeMs")
    suspend fun getRankForTime(eventId: String, isShortCourse: Boolean, minYear: Int, timeMs: Int): Int

    @Query("SELECT * FROM race_results WHERE competitionId = :compId AND eventId = :eventId AND roundType = :roundType ORDER BY timeMs ASC")
    suspend fun getResultsForRaceRound(compId: Int, eventId: String, roundType: String): List<RaceResult>

    @Query("SELECT * FROM race_results WHERE competitionId = :compId AND (roundType = 'FINAL' OR roundType = 'TIMED_FINAL') ORDER BY eventId ASC, timeMs ASC")
    suspend fun getFinalResultsForCompetition(compId: Int): List<RaceResult>

    @Query("SELECT * FROM race_results WHERE year >= :minYear AND isShortCourse = :isShortCourse")
    suspend fun getRecentResults(minYear: Int, isShortCourse: Boolean): List<RaceResult>

    @Query("SELECT swimmerId, eventId, isShortCourse, MIN(timeMs) as timeMs FROM race_results WHERE year >= :fromYear AND isShortCourse = :isShortCourse AND swimmerId IN (:swimmerIds) GROUP BY swimmerId, eventId")
    suspend fun getOptimizedRecentResults(fromYear: Int, isShortCourse: Boolean, swimmerIds: List<Int>): List<SeasonBestLight>

    @Query("DELETE FROM race_results WHERE year < :currentYear AND roundType != 'FINAL'")
    suspend fun purgeOldResults(currentYear: Int)

    @Query("""
        SELECT r.swimmerId, r.eventId, r.isShortCourse, MIN(r.timeMs) as timeMs, c.name as competitionName
        FROM race_results r
        JOIN competitions c ON r.competitionId = c.id
        WHERE r.year = :year
        GROUP BY c.name, r.eventId, r.isShortCourse
    """)
    suspend fun getBestResultsByMeet(year: Int): List<MeetBestResult>

    @Query("SELECT swimmerId, eventId, isShortCourse, MIN(timeMs) as timeMs FROM race_results WHERE year = :year GROUP BY swimmerId, eventId, isShortCourse")
    suspend fun getOptimizedSeasonBests(year: Int): List<SeasonBestLight>

    @Query("""
        SELECT r.id, r.swimmerId, r.competitionId, r.eventId, r.roundType, MIN(r.timeMs) as timeMs, r.week, r.year, r.isShortCourse 
        FROM race_results r
        INNER JOIN competitions c ON r.competitionId = c.id
        WHERE c.name = :competitionName
        GROUP BY r.eventId
    """)
    suspend fun getMeetRecords(competitionName: String): List<RaceResult>

    @Query("""
        SELECT race_results.id, race_results.swimmerId, race_results.competitionId, race_results.eventId, race_results.roundType, MIN(race_results.timeMs) as timeMs, race_results.week, race_results.year, race_results.isShortCourse 
        FROM race_results 
        INNER JOIN swimmers ON race_results.swimmerId = swimmers.id
        WHERE race_results.eventId = :eventId 
        AND race_results.year = :year
        AND race_results.isShortCourse = :isShortCourse
        AND swimmers.age BETWEEN :minAge AND :maxAge
        AND (:nationCode IS NULL OR swimmers.nationality = :nationCode)
        AND (:regionName IS NULL OR swimmers.clubId IN (SELECT id FROM clubs WHERE region = :regionName))
        GROUP BY race_results.swimmerId
        ORDER BY timeMs ASC 
        LIMIT 100
    """)
    suspend fun getTop100Ranking(
        eventId: String, year: Int, isShortCourse: Boolean, minAge: Int, maxAge: Int,
        nationCode: String?, regionName: String?
    ): List<RaceResult>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelayResults(results: List<RelayResult>)

    @Query("SELECT * FROM relay_results WHERE competitionId = :compId ORDER BY eventId ASC, totalTimeMs ASC")
    suspend fun getRelayResultsForCompetition(compId: Int): List<RelayResult>

    @Query("""
        SELECT DISTINCT r.year 
        FROM race_results r
        INNER JOIN competitions c ON r.competitionId = c.id
        WHERE c.name = :compName 
        ORDER BY r.year DESC
    """)
    suspend fun getAvailableYearsForCompetition(compName: String): List<Int>

    @Query("""
        SELECT r.* FROM race_results r
        INNER JOIN competitions c ON r.competitionId = c.id
        WHERE c.name = :compName AND r.year = :year
    """)
    suspend fun getHistoricalResults(compName: String, year: Int): List<RaceResult>

    @Query("""
        SELECT r.* FROM relay_results r
        INNER JOIN competitions c ON r.competitionId = c.id
        WHERE c.name = :compName AND r.year = :year
    """)
    suspend fun getHistoricalRelays(compName: String, year: Int): List<RelayResult>

    @Query("DELETE FROM race_results WHERE year < :currentYear")
    suspend fun purgeAllPastIndividualResults(currentYear: Int)

    @Query("DELETE FROM relay_results WHERE year < :currentYear")
    suspend fun purgeAllPastRelayResults(currentYear: Int)
}
