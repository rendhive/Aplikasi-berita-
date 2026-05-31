package com.example.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.NewsEntity
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val news: List<NewsEntity>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

sealed class ScrapingState {
    object Idle : ScrapingState()
    object Progress : ScrapingState()
    data class Success(val news: NewsEntity) : ScrapingState()
    data class FailToManual(val url: String, val errorReason: String) : ScrapingState()
}

class NewsViewModel(
    application: Application,
    private val repository: NewsRepository
) : AndroidViewModel(application) {

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _scrapingState = MutableStateFlow<ScrapingState>(ScrapingState.Idle)
    val scrapingState: StateFlow<ScrapingState> = _scrapingState.asStateFlow()

    // SSOT feed state: reactive to both category selection and repository news stream
    val feedState: StateFlow<FeedUiState> = combine(
        repository.allNews,
        _selectedCategory
    ) { newsList, category ->
        val filteredList = if (category == "Semua") {
            newsList
        } else {
            newsList.filter { it.category.equals(category, ignoreCase = true) }
        }
        FeedUiState.Success(filteredList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState.Loading
    )

    // Bookmarks reactive stream
    val bookmarkedNews: StateFlow<List<NewsEntity>> = repository.bookmarkedNews
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getNewsById(id: String): StateFlow<NewsEntity?> {
        val newsFlow = MutableStateFlow<NewsEntity?>(null)
        viewModelScope.launch {
            repository.getNewsFlow(id).collect {
                newsFlow.value = it
            }
        }
        return newsFlow
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch {
            repository.toggleBookmark(id)
        }
    }

    fun clearNonBookmarkedCache() {
        viewModelScope.launch {
            try {
                repository.clearNonBookmarkedCache()
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error manual purging: ${e.message}")
            }
        }
    }

    fun scrapeAndSaveUrl(url: String, category: String) {
        viewModelScope.launch {
            _scrapingState.value = ScrapingState.Progress
            try {
                val result = repository.scrapeAndSave(url, category)
                _scrapingState.value = ScrapingState.Success(result)
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Scraping failed, redirecting stream to manuals: ${e.message}")
                _scrapingState.value = ScrapingState.FailToManual(url, e.message ?: "Scrape Timeout / Jsoup Connection Timeout")
            }
        }
    }

    fun insertManualNews(title: String, content: String, domain: String, category: String) {
        viewModelScope.launch {
            try {
                repository.insertManualNews(title, content, domain, category)
                _scrapingState.value = ScrapingState.Idle // Reset status
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error saving manual news: ${e.message}")
            }
        }
    }

    fun resetScrapingState() {
        _scrapingState.value = ScrapingState.Idle
    }

    // Factory Class pattern to instantiate ViewModels with required parameters
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getDatabase(application)
                val repo = NewsRepository(db.newsDao(), application)
                return NewsViewModel(application, repo) as T
            }
        }
    }
}
