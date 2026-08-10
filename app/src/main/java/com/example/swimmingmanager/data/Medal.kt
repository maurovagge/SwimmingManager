package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medals")
data class Medal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val swimmerId: Int,
    val year: Int,
    val tier: String,      // "WORLD", "CONTINENTAL", "NATIONAL"
    val course: String,    // "LONG" (50m) or "SHORT" (25m)
    val category: String,  // "ASSOLUTI", "CADETTI", "JUNIORES", "RAGAZZI"
    val color: String,     // "GOLD", "SILVER", "BRONZE"
    val eventId: String    // e.g., "100_FREE_M"
)
