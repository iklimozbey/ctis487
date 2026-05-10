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
