package com.example.myapplication.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class weather_view(application: Application): AndroidViewModel(application) {
    private val repository: weather_repo

    init {
        val weatherDao = weather_db.getDatabase(application).weather_dao()
        repository = weather_repo(weatherDao)
    }
    fun addweather(weath: weather){
        viewModelScope.launch(Dispatchers.IO) {
            repository.addweather(weath)
        }
    }
    fun getWeatherByDate(date: String, callback: (weather?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val weatherData = repository.getWeatherByDate(date)
            withContext(Dispatchers.Main) {
                callback(weatherData)
            }
        }
    }
}
