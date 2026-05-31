package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.NewsDao
import com.example.data.local.NewsEntity
import com.example.data.scraper.NewsScraper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class NewsRepository(
    private val newsDao: NewsDao,
    private val context: Context
) {
    val allNews: Flow<List<NewsEntity>> = newsDao.getAllNews()
    val bookmarkedNews: Flow<List<NewsEntity>> = newsDao.getBookmarkedNews()

    fun getNewsFlow(id: String): Flow<NewsEntity?> = newsDao.getNewsFlowById(id)

    suspend fun getNewsById(id: String): NewsEntity? = withContext(Dispatchers.IO) {
        newsDao.getNewsById(id)
    }

    suspend fun toggleBookmark(id: String) = withContext(Dispatchers.IO) {
        val news = newsDao.getNewsById(id)
        if (news != null) {
            newsDao.setBookmarked(id, !news.isBookmarked)
        }
    }

    suspend fun insertManualNews(title: String, content: String, domain: String, category: String) = withContext(Dispatchers.IO) {
        val news = NewsEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            fullContent = content,
            sourceDomain = domain.ifEmpty { "manual-input.com" },
            category = category,
            timestamp = System.currentTimeMillis(),
            isBookmarked = false
        )
        newsDao.insertNews(news)
        backupToRemoteFirestore(news)
    }

    /**
     * Scrapes a URL, cleans it automatically with Regex, and saves to Room Database as SSOT
     */
    suspend fun scrapeAndSave(url: String, category: String): NewsEntity = withContext(Dispatchers.IO) {
        val scraped = NewsScraper.scrapeUrl(url)
        val news = NewsEntity(
            id = UUID.nameUUIDFromBytes(url.toByteArray()).toString(),
            title = scraped.title,
            fullContent = scraped.cleanContent,
            sourceDomain = scraped.domain,
            category = category,
            timestamp = System.currentTimeMillis(),
            isBookmarked = false
        )
        newsDao.insertNews(news)
        backupToRemoteFirestore(news)
        news
    }

    /**
     * Clean all cached non-bookmarked news from database
     */
    suspend fun clearNonBookmarkedCache() = withContext(Dispatchers.IO) {
        newsDao.clearNonBookmarkedCache()
    }

    /**
     * Cleanup old non-bookmarked news based on retention days
     */
    suspend fun deleteOldNonBookmarkedNews(retentionDays: Int) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L)
        newsDao.deleteOldNonBookmarkedNews(cutoffTime)
    }

    /**
     * Safely attempts to sync/backup saved news to remote Firestore
     */
    private fun backupToRemoteFirestore(news: NewsEntity) {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val db = FirebaseFirestore.getInstance()
                val firestoreMap = hashMapOf(
                    "id" to news.id,
                    "title" to news.title,
                    "fullContent" to news.fullContent,
                    "sourceDomain" to news.sourceDomain,
                    "category" to news.category,
                    "timestamp" to news.timestamp,
                    "postedBy" to (user.email ?: user.uid)
                )
                db.collection("news").document(news.id)
                    .set(firestoreMap)
                    .addOnSuccessListener {
                        Log.d("FirestoreBackup", "News successfully backed up to Firestore: ${news.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreBackup", "Error writing to Firestore: ${e.message}")
                    }
            } else {
                Log.d("FirestoreBackup", "User is not logged in; Firestore post skipped.")
            }
        } catch (e: Exception) {
            Log.e("FirestoreBackup", "Firestore backend unconfigured or threw exception: ${e.message}")
        }
    }
}
