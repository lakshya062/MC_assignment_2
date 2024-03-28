package com.example.myapplication

import retrofit2.Call
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface weather_response {
    @GET("timeline/DELHI,IN/{date}?key=TLRTTA47NDK2QAG9Q7AVT8K86")
    fun getWeather(@Path("date") date: String): Call<WeatherResponse>
}


data class WeatherDayInfo(
    val tempMax: Double,
    val tempMin: Double,
    val temp: Double
)


data class WeatherResponse(
    val days: List<WeatherDayInfo>
)