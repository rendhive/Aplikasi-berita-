package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.NewsRepository

class NewsCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("NewsCleanupWorker", "Starting scheduled 24-hour background news cleanup task")
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = NewsRepository(database.newsDao(), applicationContext)

            val retentionDays = 7 // [RETENTION_DAYS]
            repository.deleteOldNonBookmarkedNews(retentionDays)

            Log.d("NewsCleanupWorker", "Successfully cleaned up old news cache (retention: $retentionDays days)")
            Result.success()
        } catch (e: Exception) {
            Log.e("NewsCleanupWorker", "Error executing old news database cleanup: ${e.message}")
            Result.retry()
        }
    }
}
