package com.example.swimmingmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clubs")
data class Club(
    @PrimaryKey val id: Int=0,
    val name: String,
    val country: String,
    val city: String,
    val region: String,
    var prestige: Int, //from 1 to 5
    val infrastructureLevel: Int //from 1 to 5
)