package com.ctis487.smartwardrobe.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current_weather") val currentWeather: CurrentWeather?
)

data class CurrentWeather(
    val temperature: Double?,
    val windspeed: Double?,
    val winddirection: Double?,
    val weathercode: Int?,
    val is_day: Int?,
    val time: String?
)
