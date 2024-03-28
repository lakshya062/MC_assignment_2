package com.example.myapplication.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface weather_dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWeather(weatherData: weather)

    @Query("SELECT * FROM weather WHERE date = :date")
    fun getWeatherByDate(date: String): weather?
}
