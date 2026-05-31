package com.example.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.NewsViewModel
import com.example.presentation.viewmodel.ScrapingState
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewsScreen(
    newsViewModel: NewsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrapingState by newsViewModel.scrapingState.collectAsState()
    val categories = listOf("Finansial", "Investasi", "Teknologi", "Makro Ekonomi", "Regulasi")

    var urlInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // Manual Form Fields
    var manualTitle by remember { mutableStateOf("") }
    var manualContent by remember { mutableStateOf("") }
    var manualDomain by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf(categories[0]) }
    var isManualCategoryDropdownExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Handle scraping redirection or completion
    LaunchedEffect(scrapingState) {
        if (scrapingState is ScrapingState.Success) {
            newsViewModel.resetScrapingState()
            onNavigateBack()
        } else if (scrapingState is ScrapingState.FailToManual) {
            val failedState = scrapingState as ScrapingState.FailToManual
            // Attempt to pre-fill domain from the URL
            manualDomain = try {
                val urlObj = URL(failedState.url)
                urlObj.host.replace("www.", "")
            } catch (e: Exception) {
                "manual-input.com"
            }
            manualCategory = selectedCategory
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            newsViewModel.resetScrapingState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TAMBAH BERITA",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Beranda"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form state transitions: Scraper vs Manual
            when (val state = scrapingState) {
                is ScrapingState.Idle, is ScrapingState.Progress -> {
                    Text(
                        text = "Tarik Berita Otomatis dari URL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Scraper kami akan menarik teks berita utuh secara otomatis serta membesihkan teks sampah iklan/tautan internal media secara instan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("URL Berita Bisnis") },
                        placeholder = { Text("https://ekonomi.bisnis.com/...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field")
                    )

                    // Custom Dropdown Box for Category
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Berita") },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Pilih",
                                    modifier = Modifier.clickable { isCategoryDropdownExpanded = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCategoryDropdownExpanded = true }
                                .testTag("category_dropdown")
                        )
                        DropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state is ScrapingState.Progress) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Sedang Menghubungi Server & Menarik Konten...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (urlInput.trim().isNotEmpty()) {
                                    newsViewModel.scrapeAndSaveUrl(urlInput.trim(), selectedCategory)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("scrape_submit_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tarik & Simpan Berita")
                        }

                        OutlinedButton(
                            onClick = {
                                // Direct manual trigger bypass
                                newsViewModel.insertManualNews("", "", "", "") // Dummy start manual transition
                                newsViewModel.scrapeAndSaveUrl("http://trigger-manual-fallback.com", selectedCategory)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("manual_trigger_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tulis Berita Sendiri")
                        }
                    }
                }

                is ScrapingState.FailToManual -> {
                    // Elevated warning callout
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Scraping Otomatis Berhalangan",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Alasan: ${state.errorReason}. Silakan mengisikan berita bisnis ini secara manual di form bawah.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Form Input Berita Manual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = manualTitle,
                        onValueChange = { manualTitle = it },
                        label = { Text("Judul Berita") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_title_input")
                    )

                    OutlinedTextField(
                        value = manualContent,
                        onValueChange = { manualContent = it },
                        label = { Text("Isi Utama Berita (Teks Murni)") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 6,
                        maxLines = 15,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_content_input")
                    )

                    OutlinedTextField(
                        value = manualDomain,
                        onValueChange = { manualDomain = it },
                        label = { Text("Domain Sumber Berita") },
                        placeholder = { Text("ekonomi.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_domain_input")
                    )

                    // Dropdown for Category inside Manual Forms
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Berita") },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Pilih Kategori",
                                    modifier = Modifier.clickable { isManualCategoryDropdownExpanded = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isManualCategoryDropdownExpanded = true }
                                .testTag("manual_category_dropdown")
                        )
                        DropdownMenu(
                            expanded = isManualCategoryDropdownExpanded,
                            onDismissRequest = { isManualCategoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        manualCategory = category
                                        isManualCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (manualTitle.trim().isNotEmpty() && manualContent.trim().isNotEmpty()) {
                                newsViewModel.insertManualNews(
                                    title = manualTitle.trim(),
                                    content = manualContent.trim(),
                                    domain = manualDomain.trim(),
                                    category = manualCategory
                                )
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("manual_save_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan dan Posting Berita")
                    }

                    TextButton(
                        onClick = { newsViewModel.resetScrapingState() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kembali ke Scraper Otomatis")
                    }
                }
                else -> {}
            }
        }
    }
}
