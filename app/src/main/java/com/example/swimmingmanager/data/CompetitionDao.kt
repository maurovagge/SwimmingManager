package com.example.swimmingmanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetitionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitions(competitions: List<Competition>)

    @Query("SELECT * FROM competitions ORDER BY week ASC")
    suspend fun getAllCompetitions(): List<Competition>

    @Query("SELECT * FROM competitions WHERE week = :week")
    suspend fun getCompetitionsByWeek(week: Int): List<Competition>

    @Query("DELETE FROM competitions")
    suspend fun clearCalendar()

    @Query("SELECT * FROM competitions WHERE year = :year ORDER BY week ASC")
    fun getCompetitionsFlowByYear(year: Int): Flow<List<Competition>>

    @Query("SELECT * FROM competitions WHERE year = :year")
    suspend fun getCompetitionsByYear(year: Int): List<Competition>

}