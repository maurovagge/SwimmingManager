package com.example.swimmingmanager.data

import androidx.room.*


data class ClubLight(
    val id: Int,
    val country: String,
    val prestige: Int
)

@Dao
interface ClubDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clubs: List<Club>)

    @Query("SELECT COUNT(*) FROM clubs")
    fun getClubsCount(): Int

    @Query("SELECT id, country, prestige FROM clubs")
    suspend fun getAllClubsLight(): List<ClubLight>

    @Query("SELECT * FROM clubs ORDER BY country, region, prestige DESC")
    suspend fun getAllClubs(): List<Club>

    @Query("SELECT * FROM clubs WHERE id = :clubId LIMIT 1")
    suspend fun getClubById(clubId: Int): Club?
}