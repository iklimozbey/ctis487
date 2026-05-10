package com.ctis487.smartwardrobe.network

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    val success: Boolean,
    val profile: UserProfile
)

data class UserProfile(
    var gender: String? = "unisex",
    var profession: String? = "student",
    var ageGroup: String? = "millennial",
    var location: String? = null,
    var lat: Double? = null,
    var lon: Double? = null,
    var icalUrl: String? = null,
    var ootdCountMode: String? = "single",
    var ootdTime: String? = "21:00",
    var height: Int? = 175,
    var weight: Int? = 70,
    var bodyType: String? = "rectangle",
    var skinTone: String? = "neutral",
    var stylePref: String? = "minimalist",
    var bio: String? = null
)

data class AnalyticsResponse(
    val success: Boolean,
    val analytics: AnalyticsData
)

data class AnalyticsData(
    val stats: BasicStats,
    val colors: ColorStats,
    val categories: CategoryStats,
    val dormant: List<DormantItem>,
    val topWorn: List<TopWornItem>
)

data class BasicStats(
    val totalItems: Int,
    val totalWears: Int
)

data class ColorStats(
    val owned: Map<String, Int>,
    val worn: Map<String, Int>
)

data class CategoryStats(
    val owned: Map<String, Int>,
    val worn: Map<String, Int>
)

data class DormantItem(
    val id: String,
    val name: String,
    val reason: String
)

data class TopWornItem(
    val id: String,
    val name: String,
    val count: Int,
    val image: String?
)
