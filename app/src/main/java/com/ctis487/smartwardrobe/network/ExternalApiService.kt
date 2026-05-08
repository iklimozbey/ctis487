package com.ctis487.smartwardrobe.network

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ── Open-Meteo API ──────────────────────────────────────────
interface OpenMeteoService {
    @GET("v1/forecast")
    fun getForecast(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current_weather") current: Boolean = true,
        @Query("hourly") hourly: String = "temperature_2m,weathercode"
    ): Call<OpenMeteoResponse>
}

data class OpenMeteoResponse(
    val current_weather: OpenMeteoCurrentWeather,
    val hourly: HourlyData
)

data class OpenMeteoCurrentWeather(
    val temperature: Double,
    val weathercode: Int
)

data class HourlyData(
    val time: List<String>,
    val temperature_2m: List<Double>,
    val weathercode: List<Int>
)

// ── BigDataCloud Geocoding ──────────────────────────────────
interface GeoService {
    @GET("data/reverse-geocode-client")
    fun reverseGeocode(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("localityLanguage") lang: String = "en"
    ): Call<BigDataCloudResponse>
}

data class BigDataCloudResponse(
    val city: String?,
    val locality: String?,
    val principalSubdivision: String?
)

// ── External Retrofit Clients ───────────────────────────────
object ExternalRetrofit {
    val weather: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }

    val geo: GeoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.bigdatacloud.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeoService::class.java)
    }
}
