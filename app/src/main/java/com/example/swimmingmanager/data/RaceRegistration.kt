package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "race_registrations",
    indices = [
        Index(value = ["competitionId"]),
        Index(value = ["swimmerId"]),
        Index(value = ["competitionId", "eventCode"])
    ]
)
data class RaceRegistration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val swimmerId: Int,
    val competitionId: Int,
    val eventCode: String,
    val week: Int
)
