package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.worker.NewsCleanupWorker
import com.example.presentation.ui.AddNewsScreen
import com.example.presentation.ui.AuthScreen
import com.example.presentation.ui.BookmarkScreen
import com.example.presentation.ui.DetailScreen
import com.example.presentation.ui.FeedScreen
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.NewsViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Feed : Screen("feed")
    object Bookmarks : Screen("bookmarks")
    object AddNews : Screen("add_news")
    object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule 24-Hour clean-up WorkManager Task safely on launch
        scheduleBackgroundCleanup()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // ViewModels instantiated safely
                val authViewModel: AuthViewModel = viewModel()
                val newsViewModel: NewsViewModel = viewModel(
                    factory = NewsViewModel.provideFactory(application)
                )

                NavHost(
                    navController = navController,
                    startDestination = Screen.Auth.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Auth.route) {
                        AuthScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = {
                                navController.navigate(Screen.Feed.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Feed.route) {
                        FeedScreen(
                            newsViewModel = newsViewModel,
                            authViewModel = authViewModel,
                            onNavigateToDetail = { id ->
                                navController.navigate(Screen.Detail.createRoute(id))
                            },
                            onNavigateToAddNews = {
                                navController.navigate(Screen.AddNews.route)
                            },
                            onNavigateToBookmarks = {
                                navController.navigate(Screen.Bookmarks.route)
                            },
                            onLogout = {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Feed.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Bookmarks.route) {
                        BookmarkScreen(
                            newsViewModel = newsViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { id ->
                                navController.navigate(Screen.Detail.createRoute(id))
                            }
                        )
                    }

                    composable(Screen.AddNews.route) {
                        AddNewsScreen(
                            newsViewModel = newsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Screen.Detail.route,
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        DetailScreen(
                            newsId = id,
                            newsViewModel = newsViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleBackgroundCleanup() {
        try {
            val cleanupRequest = PeriodicWorkRequestBuilder<NewsCleanupWorker>(
                24, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "NewsCleanupWork",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            Log.d("MainActivity", "Old news cache auto-clean WorkManager job scheduled successfully.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error scheduling WorkManager task: ${e.message}")
        }
    }
}
