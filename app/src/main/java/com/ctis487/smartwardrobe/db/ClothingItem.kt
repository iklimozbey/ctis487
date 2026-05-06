package com.ctis487.smartwardrobe.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey val id: String,
    var imageUrl: String,
    val subcategory: String?,
    @SerializedName("primaryColor")
    var color: String?,
    var status: String?
)