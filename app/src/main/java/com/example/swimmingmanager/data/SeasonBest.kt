package com.example.swimmingmanager.data

import androidx.room.Entity

@Entity(tableName = "season_bests", primaryKeys = ["swimmerId", "eventId", "isShortCourse", "year"])
data class SeasonBest(
    val swimmerId: Int,
    val eventId: String,
    val isShortCourse: Boolean,
    val timeMs: Int,
    val year: Int
)
