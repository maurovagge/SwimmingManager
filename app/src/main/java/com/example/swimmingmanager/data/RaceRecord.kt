package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "race_records",
    indices = [
        // Ensures we only have ONE record per event/course/type/tag combination
        Index(value = ["eventId", "isShortCourse", "recordType", "tag"], unique = true)
    ]
)
data class RaceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: String,
    val isShortCourse: Boolean,
    val recordType: String, // "WORLD", "NATIONAL", "MEET"
    val tag: String,        // "ALL" for world, "ITA" for national, "Olimpiadi" for meet
    val swimmerId: Int,
    val timeMs: Int,
    val year: Int
)