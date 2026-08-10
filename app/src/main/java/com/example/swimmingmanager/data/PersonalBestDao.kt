package com.example.swimmingmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PersonalBestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePersonalBests(pbs: List<PersonalBest>)

    @Query("SELECT * FROM personal_bests")
    suspend fun getAllPBs(): List<PersonalBest>

    @Query("SELECT * FROM personal_bests WHERE swimmerId = :swimmerId")
    suspend fun getAllPBsForSwimmer(swimmerId: Int): List<PersonalBest>

    @Query("""
        SELECT timeMs FROM personal_bests 
        WHERE swimmerId = :swimmerId 
          AND eventId = :eventId 
          AND isShortCourse = :isShort
    """)
    suspend fun getPersonalBest(swimmerId: Int, eventId: String, isShort: Boolean): Int?

    @Query("""
        INSERT INTO personal_bests (swimmerId, eventId, isShortCourse, timeMs, year)
        SELECT swimmerId, eventId, isShortCourse, timeMs, :year 
        FROM race_results
        WHERE year = :year
        ON CONFLICT(swimmerId, eventId, isShortCourse) DO UPDATE SET
        timeMs = excluded.timeMs,
        year = excluded.year
        WHERE excluded.timeMs < personal_bests.timeMs
    """)
    suspend fun updateAllPBsFromSeason(year: Int)
}
