package com.ctis487.smartwardrobe.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ClothingItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clothingDao(): ClothingDao
}