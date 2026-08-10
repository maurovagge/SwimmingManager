package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val type: String, // "CALLUP", "RETIREMENT", "RECRUIT", "SEASON_END"
    val year: Int,
    val week: Int,
    val isRead: Boolean = false
)
