package com.ctis487.smartwardrobe.network

import com.ctis487.smartwardrobe.db.ClothingItem

data class OutfitSearchRequest(
    val query: String
)

data class OutfitSearchResponse(
    val success: Boolean,
    val query: String?,
    val outfit: OutfitResult?,
    val message: String?,
    val error: String?
)

data class OutfitResult(
    val name: String,
    val reasoning: String,
    val items: List<ClothingItem>
)
