package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relay_results")
data class RelayResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val competitionId: Int,
    val eventId: String,
    val teamId: Int,
    val teamName: String,
    val totalTimeMs: Int,
    // Storage for individual split times
    val swimmer1Id: Int,
    val swimmer1SplitMs: Int, // NEW: Split of 1st swimmer
    val swimmer2Id: Int,
    val swimmer2SplitMs: Int, // NEW: Split of 2nd swimmer
    val swimmer3Id: Int,
    val swimmer3SplitMs: Int, // NEW: Split of 3rd swimmer
    val swimmer4Id: Int,
    val swimmer4SplitMs: Int, // NEW: Split of 4th swimmer
    val year: Int,
    val isShortCourse: Boolean,
    val roundType: String = "FINAL"
)