package com.ctis487.smartwardrobe.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey val id: String,
    var imageUrl: String,
    val category: String? = null,
    val subcategory: String? = null,
    @SerializedName("primaryColor")
    var color: String? = null,
    @SerializedName("secondaryColor")
    val secondaryColor: String? = null,
    val colorTone: String? = null,
    val pattern: String? = null,
    val material: String? = null,
    val style: String? = null,
    val fit: String? = null,
    val description: String? = null,
    val gender: String? = null,
    val season: List<String>? = null,
    val occasionTags: List<String>? = null,
    var status: String? = "closet",
    var lastWorn: String? = null,
    var wornCount: Int = 0
)