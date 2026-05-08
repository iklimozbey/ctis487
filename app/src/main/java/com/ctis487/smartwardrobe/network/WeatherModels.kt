package com.ctis487.smartwardrobe.network

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val success: Boolean,
    val city: String?,
    val temp: Int?,
    val conditionText: String?,
    val icon: String?,
    val lat: Double?,
    val lon: Double?,
    val date: String?,
    val hourly: List<HourlyWeather>?,
    @SerializedName("current_weather") val currentWeather: CurrentWeather?
)

data class HourlyWeather(
    val time: String,
    val temp: Int,
    val condition: String,
    val icon: String
)

data class CurrentWeather(
    val temperature: Double?,
    val windspeed: Double?,
    val winddirection: Double?,
    val weathercode: Int?,
    val is_day: Int?,
    val time: String?
)

data class GeoResponse(
    val success: Boolean,
    val city: String?
)
