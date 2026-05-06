package com.ctis487.smartwardrobe.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ctis487.smartwardrobe.db.AppDatabase
import com.ctis487.smartwardrobe.network.RetrofitClient

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Background sync started...")
        return try {
            com.ctis487.smartwardrobe.network.SyncManager.syncFromBackend(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
