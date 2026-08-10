package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "competitions")
data class Competition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // E.g., "Trofeo Settecolli", "Criteria Nazionali Giovanili", "Olympics"
    val name: String,

    // 1 to 52
    val week: Int,
    val year: Int,

    // Determines if it's 25m (true) or 50m (false). Affects the RaceEngine physics!
    val isShortCourse: Boolean,

    // "REGIONAL", "NATIONAL", "INTERNATIONAL"
    val tier: String,

    // For youth categories. E.g., minAge = 14, maxAge = 15 (Ragazzi).
    // If it's an absolute championship, we can set maxAge = 99
    val minAge: Int,
    val maxAge: Int,

    // If true, swimmers represent their 'nationality' (e.g., ITA) instead of their Club.
    val isNationalTeamEvent: Boolean,

    // If false, the UI will hide "Roll of Honor" and "Event Records" tabs
    val hasHistory: Boolean,
    // If true, the scoring system is club-based (like Coppa Brema)
    val isTeamEvent: Boolean
)
