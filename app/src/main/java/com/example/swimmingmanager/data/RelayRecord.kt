package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relay_records",
    indices = [
        Index(value = ["eventId", "isShortCourse", "recordType", "tag"], unique = true)
    ]
)
data class RelayRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: String,
    val isShortCourse: Boolean,
    val recordType: String, 
    val tag: String,        
    val teamId: Int,        
    val teamName: String,
    val swimmer1Id: Int,
    val swimmer1SplitMs: Int,
    val swimmer2Id: Int,
    val swimmer2SplitMs: Int,
    val swimmer3Id: Int,
    val swimmer3SplitMs: Int,
    val swimmer4Id: Int,
    val swimmer4SplitMs: Int,
    val timeMs: Int,
    val year: Int
)
