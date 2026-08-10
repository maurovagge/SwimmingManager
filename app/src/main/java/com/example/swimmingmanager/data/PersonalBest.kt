package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Lightweight entity to store all-time personal bests for each swimmer
@Entity(
    tableName = "personal_bests",
    indices = [
        Index(value = ["swimmerId", "eventId", "isShortCourse"], unique = true)
    ]
)
data class PersonalBest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val swimmerId: Int,
    val eventId: String,
    val isShortCourse: Boolean,
    val timeMs: Int,
    val year: Int
)
