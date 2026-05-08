package com.ctis487.smartwardrobe.network

// ── Weather ────────────────────────────────────────────────────
data class WeatherResponse(
    val success: Boolean,
    val city: String?,
    val temp: Int?,
    val conditionText: String?,
    val icon: String?,
    val lat: Double?,
    val lon: Double?,
    val date: String?,
    val hourly: List<HourlyWeather>?
)

data class HourlyWeather(
    val time: String,
    val temp: Int,
    val condition: String,
    val icon: String
)

data class GeoResponse(
    val success: Boolean,
    val city: String?
)

// ── Search / OOTD ──────────────────────────────────────────────
data class OutfitSearchRequest(
    val query: String,
    val stylingMode: String? = null,
    val weather: WeatherContext? = null
)

data class WeatherContext(
    val temp: Int?,
    val conditionText: String?,
    val city: String?,
    val icon: String?
)

data class OutfitSearchResponse(
    val success: Boolean,
    val query: String?,
    val outfit: OutfitResult?,
    val message: String?,
    val error: String?
)

data class OutfitResult(
    val outfitName: String?,
    val name: String?,
    val reasoning: String?,
    val items: List<OutfitItem>?,
    val visualCohesion: Double?,
    val logicHarmony: Double?
)

data class OutfitItem(
    val id: String?,
    val imageUrl: String?,
    val subcategory: String?,
    val primaryColor: String?,
    val role: String?,
    val visualSimilarity: Double?,
    val logicScore: Double?
)
