package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_table ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news_table WHERE id = :id")
    fun getNewsFlowById(id: String): Flow<NewsEntity?>

    @Query("SELECT * FROM news_table WHERE id = :id")
    suspend fun getNewsById(id: String): NewsEntity?

    @Query("SELECT * FROM news_table WHERE is_bookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedNews(): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: NewsEntity)

    @Update
    suspend fun updateNews(news: NewsEntity)

    @Query("UPDATE news_table SET is_bookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, isBookmarked: Boolean)

    @Query("DELETE FROM news_table WHERE timestamp < :cutoffTime AND is_bookmarked = 0")
    suspend fun deleteOldNonBookmarkedNews(cutoffTime: Long)

    @Query("DELETE FROM news_table WHERE is_bookmarked = 0")
    suspend fun clearNonBookmarkedCache()
}
