package com.ctis487.smartwardrobe.db


import androidx.room.*
import com.ctis487.smartwardrobe.db.ClothingItem

@Dao
interface ClothingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ClothingItem)

    @Query("SELECT * FROM clothing_items WHERE status = 'closet'")
    suspend fun getClosetItems(): List<ClothingItem>

    @Query("SELECT * FROM clothing_items")
    suspend fun getAllItems(): List<ClothingItem>

    @Delete
    suspend fun deleteItem(item: ClothingItem)

    @Query("DELETE FROM clothing_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("DELETE FROM clothing_items")
    suspend fun clearAll()

    @Update
    suspend fun updateItem(item: ClothingItem)
}