package com.example.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NewsEntity
import com.example.presentation.viewmodel.AuthState
import com.example.presentation.viewmodel.AuthViewModel
import com.example.presentation.viewmodel.FeedUiState
import com.example.presentation.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    newsViewModel: NewsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddNews: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by newsViewModel.feedState.collectAsState()
    val selectedCategory by newsViewModel.selectedCategory.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val categories = listOf("Semua", "Finansial", "Investasi", "Teknologi", "Makro Ekonomi", "Regulasi")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showPurgeConfirmDialog by remember { mutableStateOf(false) }

    // Redirect to login if user logs out
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            onLogout()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PORTAL BISNIS",
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Text-Only Business News Platform",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    // Manual clean configuration trigger
                    IconButton(
                        onClick = { showPurgeConfirmDialog = true },
                        modifier = Modifier.testTag("purge_button")
                    ) {
                        Icon(
                            Icons.Filled.DeleteSweep,
                            contentDescription = "Bersihkan Cache Non-Bookmark",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    // Bookmarks screen shortcut
                    IconButton(
                        onClick = onNavigateToBookmarks,
                        modifier = Modifier.testTag("bookmarks_shortcut_button")
                    ) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Buka Bookmark")
                    }

                    // Sign-out action
                    IconButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Keluar Akun",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            // Verify if user is authenticated to allow posting
            if (authState is AuthState.Authenticated) {
                FloatingActionButton(
                    onClick = onNavigateToAddNews,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_news_fab")
                        .padding(bottom = 8.dp, end = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Scrape & Tambah Berita")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal categories flow selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { newsViewModel.setCategory(category) },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("category_chip_${category.lowercase().replace(" ","_")}")
                    )
                }
            }

            // Central feed area
            when (val state = feedState) {
                is FeedUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FeedUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is FeedUiState.Success -> {
                    val newsList = state.news
                    if (newsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Business, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Text(
                                    "Tidak ada berita dalam kategori ini.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                                if (authState is AuthState.Authenticated) {
                                    Text(
                                        "Tekan tombol + di kanan bawah untuk scrape URL baru.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Light
                                    )
                                } else {
                                    Text(
                                        "Login untuk memposting berita bisnis baru.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Light
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(newsList, key = { it.id }) { newsItem ->
                                NewsItemRow(
                                    news = newsItem,
                                    onClick = { onNavigateToDetail(newsItem.id) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Purging confirmation dialog dialog
    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            title = { Text("Bersihkan Cache?") },
            text = { Text("Tindakan ini akan menghapus semua berita dari penyimpanan lokal SEGERA, kecuali berita yang telah Anda beri bookmark (favorit). Lanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        newsViewModel.clearNonBookmarkedCache()
                        showPurgeConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text("Batal")
                }
            },
            modifier = Modifier.testTag("purge_confirm_dialog")
        )
    }
}

@Composable
fun NewsItemRow(
    news: NewsEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(news.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(news.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Source and timestamp meta elements
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = news.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Title
        Text(
            text = news.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Text Content Snippet
        Text(
            text = news.fullContent,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Light,
            lineHeight = 20.sp
        )

        // Text Attribution Row without click
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sumber: ${news.sourceDomain}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace
            )
            if (news.isBookmarked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = "Di-bookmark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
