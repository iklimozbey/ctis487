package com.ctis487.smartwardrobe.network

import android.content.Context
import android.util.Log
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.db.ClothingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncManager {

    suspend fun syncFromBackend(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getItems().execute()
                if (response.isSuccessful) {
                    val items = response.body()?.items ?: emptyList()
                    val db = AppDatabase.getDatabase(context)
                    
                    items.forEach { item ->
                        if (item.imageUrl.startsWith("/uploads")) {
                            item.imageUrl = "http://10.0.2.2:3001" + item.imageUrl
                        }
                        db.clothingDao().insertItem(item)
                    }
                    Log.d("SyncManager", "Synced ${items.size} items.")
                } else {
                    Log.e("SyncManager", "Sync error code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncManager", "Sync failed: ${e.message}")
            }
        }
    }

    suspend fun deleteFromBackend(id: String) {
        withContext(Dispatchers.IO) {
            try {
                RetrofitClient.instance.deleteItem(id).execute()
            } catch (e: Exception) {
                Log.e("SyncManager", "Delete on backend failed: ${e.message}")
            }
        }
    }

    suspend fun updateStatusOnBackend(id: String, status: String) {
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf("status" to status)
                RetrofitClient.instance.updateStatus(id, body).execute()
            } catch (e: Exception) {
                Log.e("SyncManager", "Status update on backend failed: ${e.message}")
            }
        }
    }
}
