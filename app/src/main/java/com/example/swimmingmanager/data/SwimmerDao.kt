package com.example.swimmingmanager.data

import androidx.room.*

// Small data class for high-speed RAM calculations
data class SwimmerLight(
    val id: Int,
    val nationality: String,
    val clubId: Int,
    val gender: String,
    val age: Int
)

@Dao
interface SwimmerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(swimmers: List<Swimmer>)

    @Query("SELECT COUNT(*) FROM swimmers")
    suspend fun getSwimmersCount(): Int

    @Query("SELECT id, nationality, clubId, gender, age FROM swimmers WHERE isRetired = 0")
    suspend fun getAllSwimmersLight(): List<SwimmerLight>

    @Query("""
        SELECT s.* FROM swimmers s
        INNER JOIN race_registrations r ON s.id = r.swimmerId
        WHERE r.competitionId = :compId AND s.isRetired = 0
    """)
    suspend fun getSwimmersByCompetitionId(compId: Int): List<Swimmer>

    @Query("""
        SELECT * FROM swimmers 
        WHERE id IN (SELECT DISTINCT swimmerId FROM race_registrations WHERE competitionId = :compId)
        AND isRetired = 0
    """)
    suspend fun getSwimmersByCompetition(compId: Int): List<Swimmer>

    @Query("""
    SELECT s.* FROM swimmers s
    INNER JOIN clubs c ON s.clubId = c.id
    WHERE s.age BETWEEN :minAge AND :maxAge
    AND s.isRetired = 0
    AND s.clubId != :playerClubId
    AND (
        :tier = 'INTERNATIONAL' 
        OR (s.nationality = :nation AND (:tier = 'NATIONAL' OR TRIM(c.region) = TRIM(:region)))
    )
""")
    suspend fun getEligibleSwimmersForMeet(
        minAge: Int,
        maxAge: Int,
        nation: String,
        region: String,
        tier: String,
        playerClubId: Int
    ): List<Swimmer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimmer(swimmer: Swimmer)

    @Query("SELECT * FROM swimmers")
    suspend fun getAllSwimmers(): List<Swimmer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimmers(swimmers: List<Swimmer>): List<Long> // FIXED: Returns generated IDs

    @Query("SELECT * FROM swimmers WHERE clubId = :id AND isRetired = 0 ORDER BY potentialPoints DESC")
    suspend fun getSwimmersByClub(id: Int): List<Swimmer>

    @Query("SELECT * FROM swimmers WHERE clubId = :id")
    suspend fun getFullClubRoster(id: Int): List<Swimmer>

    @Update
    suspend fun updateSwimmers(swimmers: List<Swimmer>)

    @Update
    suspend fun updateSwimmer(swimmer: Swimmer)

    @Query("""
    UPDATE swimmers 
    SET 
        speed = CASE 
            WHEN isDeclining = 1 THEN MAX(speed - (0.05 + ((age - 26) * 0.01)), 1.0) 
            WHEN potentialPoints > 0 AND speed >= 95.0 THEN MIN(speed + 0.015, 100.0)
            WHEN potentialPoints > 0 AND speed >= 85.0 THEN MIN(speed + 0.04, 100.0)
            WHEN potentialPoints > 0 THEN MIN(speed + 0.1, 100.0) 
            ELSE speed END,
        endurance = CASE 
            WHEN isDeclining = 1 THEN MAX(endurance - (0.025 + ((age - 26) * 0.005)), 1.0) 
            WHEN potentialPoints > 0 AND endurance >= 95.0 THEN MIN(endurance + 0.015, 100.0)
            WHEN potentialPoints > 0 AND endurance >= 85.0 THEN MIN(endurance + 0.04, 100.0)
            WHEN potentialPoints > 0 THEN MIN(endurance + 0.1, 100.0) 
            ELSE endurance END,
        sprintPower = CASE 
            WHEN isDeclining = 1 THEN MAX(sprintPower - (0.05 + ((age - 26) * 0.01)), 1.0) 
            WHEN potentialPoints > 0 AND sprintPower >= 95.0 THEN MIN(sprintPower + 0.015, 100.0)
            WHEN potentialPoints > 0 AND sprintPower >= 85.0 THEN MIN(sprintPower + 0.04, 100.0)
            WHEN potentialPoints > 0 THEN MIN(sprintPower + 0.1, 100.0) 
            ELSE sprintPower END,
        technique = CASE 
            WHEN potentialPoints > 0 AND isDeclining = 0 AND technique >= 95.0 THEN MIN(technique + 0.015, 100.0)
            WHEN potentialPoints > 0 AND isDeclining = 0 AND technique >= 85.0 THEN MIN(technique + 0.04, 100.0)
            WHEN potentialPoints > 0 AND isDeclining = 0 THEN MIN(technique + 0.1, 100.0) 
            ELSE technique END,
        potentialPoints = CASE 
            WHEN isDeclining = 0 AND (speed >= 90.0 OR endurance >= 90.0 OR sprintPower >= 90.0 OR technique >= 90.0) THEN MAX(potentialPoints - 0.8, 0.0) 
            WHEN isDeclining = 0 THEN MAX(potentialPoints - 0.4, 0.0) 
            ELSE potentialPoints END
    WHERE clubId != :playerClubId AND isRetired = 0
""")
    suspend fun simulateWeeklyAITraining(playerClubId: Int)

    @Query("SELECT * FROM swimmers WHERE id IN (:ids)")
    suspend fun getSwimmersByIds(ids: List<Int>): List<Swimmer>

    @Query("UPDATE swimmers SET potentialPoints = potentialPoints + 5.0 WHERE age <= 25 AND potentialPoints < 3.0")
    suspend fun applyRebirthBonus()

    @Query("""
    UPDATE swimmers 
    SET 
        age = age + 1,
        isRetired = CASE 
            WHEN isRetired = 0 AND isDeclining = 1 AND ABS(RANDOM() % 100) < ((age + 1 - 25) * 5) THEN 1 
            ELSE isRetired END,
        currentTraining = CASE 
            WHEN isRetired = 0 AND isDeclining = 1 AND ABS(RANDOM() % 100) < ((age + 1 - 25) * 5) THEN 'RETIRED' 
            ELSE currentTraining END,
        isDeclining = CASE 
            WHEN isRetired = 0 AND isDeclining = 0 AND (age + 1) >= 26 AND ABS(RANDOM() % 100) < ((age + 1 - 24) * 5) THEN 1 
            ELSE isDeclining END
    WHERE isRetired = 0
""")
    suspend fun executeGlobalEndSeasonUpdate()
}
