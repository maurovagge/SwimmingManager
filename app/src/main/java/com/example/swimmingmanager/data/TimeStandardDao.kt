package com.example.swimmingmanager.data

import androidx.room.*

@Dao
interface TimeStandardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandards(standards: List<TimeStandard>)

    // Gets the specific standard for a pool length
    @Query("SELECT timeMs FROM time_standards WHERE eventId = :eventId AND isShortCourse = :isShortCourse AND year = :year AND tier = :tier AND nationCode = :nationCode LIMIT 1")
    suspend fun getStandardTime(eventId: String, isShortCourse: Boolean, year: Int, tier: String, nationCode: String): Int?

    @Query("SELECT * FROM time_standards WHERE year = :year AND tier = :tier")
    suspend fun getAllStandardsForTier(year: Int, tier: String): List<TimeStandard>

    // Gets all time standards for a specific nation and year
    @Query("SELECT * FROM time_standards WHERE year = :year AND nationCode = :nationCode")
    suspend fun getAllStandardsForNation(year: Int, nationCode: String): List<TimeStandard>
}