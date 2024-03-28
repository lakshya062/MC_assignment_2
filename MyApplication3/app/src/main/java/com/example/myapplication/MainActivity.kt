package com.example.myapplication

import android.app.Application
import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.database.weather
import com.example.myapplication.database.weather_view
import com.example.myapplication.ui.theme.MyApplicationTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherApp()
                }
            }
        }
    }
}

@Composable
fun WeatherApp() {
    var date by remember { mutableStateOf("") }
    var weatherInfo by remember { mutableStateOf("Select a date to get the weather info") }
    val context = LocalContext.current

    val myviewtemp = remember(context) {
        weather_view(context.applicationContext as Application)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Button to set the date and get weather from the API
        Button(onClick = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val adjustedMonth = month + 1 // Calendar month is zero-based
                    date = "$year-${adjustedMonth.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
                    getWeather(myviewtemp, date) { info ->
                        weatherInfo = info
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }) {
            Text("Set Date")
        }
        Button(onClick = {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val adjustedMonth = month + 1
                    val selectedDate = "$year-${adjustedMonth.toString().padStart(2, '0')}-${dayOfMonth.toString().padStart(2, '0')}"
                    readdata(myviewtemp, selectedDate) { dbWeatherInfo ->
                        weatherInfo = dbWeatherInfo.toString()
                        date = selectedDate
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Get Weather From Database")
        }

        Text("Date selected: $date", modifier = Modifier.padding(top = 8.dp))
        Text(weatherInfo, modifier = Modifier.padding(top = 8.dp))
    }
}


private fun insertdata(myview: weather_view,mydate: String,temp: Double,tempmin: Double, tempmax: Double){
    val mytempdata = weather(mydate,tempmax,tempmin,temp)
    myview.addweather(mytempdata)
}


private fun readdata(myview: weather_view, mydate: String, callback: (weather?) -> Unit) {
    return myview.getWeatherByDate(mydate, callback)
}





fun getWeather(mytempview: weather_view, date: String, callback: (String) -> Unit) {
    if (!isInternetAvailable(context)) {
        callback("No internet available")
        return
    }
    val currentDate = Calendar.getInstance()
    val selectedDateCalendar = Calendar.getInstance().apply {
        time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return
    }

    if (selectedDateCalendar.after(currentDate)) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(weather_response::class.java)
        val yearsToFetch = 10
        var fetchedYears = 0
        var sumMaxTemp = 0.0
        var sumMinTemp = 0.0
        var sumAvgTemp = 0.0

        for (i in 1..yearsToFetch) {
            val pastYear = currentDate.get(Calendar.YEAR) - i
            val pastDate = "$pastYear-${date.substring(5)}" // Adjust the date to the past year

            service.getWeather(pastDate).enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.days?.firstOrNull()?.let {
                            sumMaxTemp += it.tempMax
                            sumMinTemp += it.tempMin
                            sumAvgTemp += it.temp
                            fetchedYears++
                            if (fetchedYears == yearsToFetch) {
                                val averageMaxTemp = sumMaxTemp / yearsToFetch
                                val averageMinTemp = sumMinTemp / yearsToFetch
                                val averageTemp = sumAvgTemp / yearsToFetch
                                insertdata(mytempview, date, averageTemp, averageMinTemp, averageMaxTemp)
                                callback("Future weather forecast for $date - Avg Max Temp: $averageMaxTemp°C, Avg Min Temp: $averageMinTemp°C, Avg Temp: $averageTemp°C")
                            }
                        }
                    } else {
                        fetchedYears++
                        if (fetchedYears == yearsToFetch && fetchedYears > 0) {
                            val averageMaxTemp = if (fetchedYears > 0) sumMaxTemp / fetchedYears else 0.0
                            val averageMinTemp = if (fetchedYears > 0) sumMinTemp / fetchedYears else 0.0
                            val averageTemp = if (fetchedYears > 0) sumAvgTemp / fetchedYears else 0.0
                            insertdata(mytempview, date, averageTemp, averageMinTemp, averageMaxTemp)
                            callback("Partial future weather forecast for $date - Avg Max Temp: $averageMaxTemp°C, Avg Min Temp: $averageMinTemp°C, Avg Temp: $averageTemp°C due to some failed requests.")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    fetchedYears++
                    // Handle the failure similarly to a non-successful response
                    if (fetchedYears == yearsToFetch && fetchedYears > 0) {
                        // Handle case where all responses have been received but some failed
                        val averageMaxTemp = if (fetchedYears > 0) sumMaxTemp / fetchedYears else 0.0
                        val averageMinTemp = if (fetchedYears > 0) sumMinTemp / fetchedYears else 0.0
                        val averageTemp = if (fetchedYears > 0) sumAvgTemp / fetchedYears else 0.0
                        insertdata(mytempview, date, averageTemp, averageMinTemp, averageMaxTemp)
                        callback("Partial future weather forecast for $date - Avg Max Temp: $averageMaxTemp°C, Avg Min Temp: $averageMinTemp°C, Avg Temp: $averageTemp°C due to some failed requests.")
                    }
                }
            })
        }
    } else {
        // This is the continuation of the getWeather function for handling past dates.
        val retrofit = Retrofit.Builder()
            .baseUrl("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(weather_response::class.java)

        service.getWeather(date).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    val dayInfo = weatherData?.days?.firstOrNull()
                    if (dayInfo != null) {
                        insertdata(mytempview, date, dayInfo.temp, dayInfo.tempMin, dayInfo.tempMax)
                        callback("Max Temp: ${dayInfo.tempMax}°C, Min Temp: ${dayInfo.tempMin}°C, Avg Temp: ${dayInfo.temp}°C")
                    } else {
                        callback("Weather data not available for $date")
                    }
                } else {
                    callback("Failed to retrieve weather data for $date")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                callback("Error: ${t.message}")
            }
        })
    }
}


fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        WeatherApp()
    }
}
