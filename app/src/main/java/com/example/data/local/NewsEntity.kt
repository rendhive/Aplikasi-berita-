package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_table")
data class NewsEntity(
    @PrimaryKey val id: String, // String UUID or URL
    val title: String,
    @ColumnInfo(name = "full_content") val fullContent: String,
    @ColumnInfo(name = "source_domain") val sourceDomain: String,
    val category: String,
    val timestamp: Long,
    @ColumnInfo(name = "is_bookmarked") val isBookmarked: Boolean = false
)
