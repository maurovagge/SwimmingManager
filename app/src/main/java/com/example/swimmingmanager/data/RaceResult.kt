package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

data class SeasonBestLight(
    val swimmerId: Int,
    val eventId: String,
    val isShortCourse: Boolean,
    val timeMs: Int
)

@Entity(
    tableName = "race_results",
    indices = [
        Index(value = ["year", "isShortCourse"]),
        Index(value = ["swimmerId", "eventId"]),
        Index(value = ["competitionId"]), // Fondamentale per caricare i risultati dei meet
        Index(value = ["eventId", "year", "timeMs"]), // Fondamentale per i ranking
        Index(value = ["year", "roundType"]),
        Index(value = ["year", "isShortCourse", "eventId", "swimmerId", "timeMs"])
    ]
)
data class RaceResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val swimmerId: Int,
    val competitionId: Int,
    val eventId: String, 
    val isShortCourse: Boolean,
    val timeMs: Int, 
    val week: Int,
    val year: Int,
    val roundType: String 
)
