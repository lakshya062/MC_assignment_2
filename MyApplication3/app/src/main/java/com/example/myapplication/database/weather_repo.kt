package com.example.myapplication.database


class weather_repo (private val weather_dao: weather_dao){
    suspend fun addweather(weath:weather){
        weather_dao.insertWeather(weath)
    }
    suspend fun getWeatherByDate(date: String): weather? {
        return weather_dao.getWeatherByDate(date)
    }


}