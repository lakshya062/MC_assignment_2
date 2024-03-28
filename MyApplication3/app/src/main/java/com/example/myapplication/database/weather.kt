package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class weather(
    @PrimaryKey val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val temp: Double
)