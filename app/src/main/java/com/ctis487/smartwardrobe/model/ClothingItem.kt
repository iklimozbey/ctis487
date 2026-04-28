package com.ctis487.smartwardrobe.model

data class ClothingItem(
    val id: String,
    val imageUrl: String,
    val subcategory: String,
    val color: String,
    var status: String // "closet" or "laundry"
)
