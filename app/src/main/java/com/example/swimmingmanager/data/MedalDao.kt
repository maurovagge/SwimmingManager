package com.example.swimmingmanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MedalDao {
    @Insert
    suspend fun insertMedals(medals: List<Medal>)

    // Fetches all medals for a specific swimmer, ordered by Year (newest first)
    @Query("SELECT * FROM medals WHERE swimmerId = :swimmerId ORDER BY year DESC")
    suspend fun getMedalsForSwimmer(swimmerId: Int): List<Medal>
}