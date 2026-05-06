package com.ctis487.smartwardrobe.network

import com.ctis487.smartwardrobe.db.ClothingItem

data class ItemsResponse(
    val success: Boolean,
    val items: List<ClothingItem>
)
