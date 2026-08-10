package com.example.swimmingmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SeasonBestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasonBests(bests: List<SeasonBest>)

    @Query("SELECT * FROM season_bests WHERE year = :year")
    suspend fun getSeasonBestsForYear(year: Int): List<SeasonBest>

    @Query("SELECT * FROM season_bests WHERE swimmerId IN (:swimmerIds) AND year = :year")
    suspend fun getSeasonBestsForSwimmers(swimmerIds: List<Int>, year: Int): List<SeasonBest>

    @Query("DELETE FROM season_bests WHERE year < :minYear")
    suspend fun purgeOldSeasonBests(minYear: Int)
}
