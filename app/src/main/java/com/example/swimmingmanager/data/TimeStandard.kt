package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_standards")
data class TimeStandard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val eventId: String, // "100_FREE_M"
    val tier: String, // "NATIONAL_ABSOLUTE" or "NATIONAL_YOUTH"
    val nationCode: String?,
    val isShortCourse: Boolean,
    val timeMs: Int
)
