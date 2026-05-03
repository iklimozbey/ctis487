package com.ctis487.smartwardrobe.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clothing_items")
data class ClothingItem(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val subcategory: String,
    val color: String,
    var status: String
)