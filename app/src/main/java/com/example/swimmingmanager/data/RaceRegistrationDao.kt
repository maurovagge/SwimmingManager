package com.example.swimmingmanager.data

import androidx.room.*

@Dao
interface RaceRegistrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registerSwimmer(registration: RaceRegistration)

    @Query("DELETE FROM race_registrations WHERE swimmerId = :swimmerId AND competitionId = :compId AND eventCode = :event")
    suspend fun unregisterSwimmer(swimmerId: Int, compId: Int, event: String)

    // Gets all registrations for a specific competition (to build the heat sheets)
    @Query("SELECT * FROM race_registrations WHERE competitionId = :compId")
    suspend fun getRegistrationsForCompetition(compId: Int): List<RaceRegistration>

    // Gets all registrations for a specific swimmer (to show in their profile)
    @Query("SELECT * FROM race_registrations WHERE swimmerId = :swimmerId")
    suspend fun getRegistrationsForSwimmer(swimmerId: Int): List<RaceRegistration>

    @Query("DELETE FROM race_registrations")
    suspend fun clearAllRegistrations()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistrations(registrations: List<RaceRegistration>)

    @Query("""
        SELECT 
            s.firstName, 
            s.lastName, 
            s.clubId, 
            MIN(rr.timeMs) as qualificationTimeMs
        FROM race_registrations AS reg
        INNER JOIN swimmers AS s ON reg.swimmerId = s.id
        INNER JOIN race_results AS rr ON s.id = rr.swimmerId
        WHERE reg.competitionId = :compId 
          AND s.nationality = :nationCode
          AND reg.eventCode = :eventCode
          AND rr.eventId = :eventCode
          AND rr.year = :currentYear
        GROUP BY s.id
        ORDER BY qualificationTimeMs ASC
    """)
    suspend fun getDetailedNationalCallUps(
        compId: Int,
        nationCode: String,
        eventCode: String,
        currentYear: Int
    ): List<CallUpDisplayInfo>

    @Query("""
    SELECT 
        reg.eventCode,
        s.firstName, 
        s.lastName, 
        s.clubId, 
        MIN(rr.timeMs) as qualificationTimeMs
    FROM race_registrations AS reg
    INNER JOIN swimmers AS s ON reg.swimmerId = s.id
    INNER JOIN race_results AS rr ON s.id = rr.swimmerId AND rr.eventId = reg.eventCode
    WHERE reg.competitionId = :compId 
      AND s.nationality = :nationCode
      AND rr.year = :currentYear
      AND rr.isShortCourse = :isShortCourse
    GROUP BY reg.eventCode, s.id
    ORDER BY qualificationTimeMs ASC
""")
    suspend fun getAllDetailedNationalCallUps(
        compId: Int,
        nationCode: String,
        currentYear: Int,
        isShortCourse: Boolean
    ): List<EventCallUpInfo>

    // A simple POJO to hold the joined data
    data class CallUpDisplayInfo(
        val firstName: String,
        val lastName: String,
        val clubId: Int,
        val qualificationTimeMs: Int
    )

    data class EventCallUpInfo(
        val eventCode: String,
        val firstName: String,
        val lastName: String,
        val clubId: Int,
        val qualificationTimeMs: Int
    )
}