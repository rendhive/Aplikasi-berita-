package com.example.data.scraper

import org.jsoup.Jsoup
import java.net.URL

object NewsScraper {
    private const val TIMEOUT_LIMIT = 10000 // up to 10 seconds for robustness

    data class ScrapedNews(
        val title: String,
        val cleanContent: String,
        val domain: String
    )

    suspend fun scrapeUrl(urlStr: String): ScrapedNews {
        val domain = try {
            val url = URL(urlStr)
            url.host.replace("www.", "")
        } catch (e: Exception) {
            "unknown.com"
        }

        // Connect using Jsoup with custom set timeout and browser user-agent
        val doc = Jsoup.connect(urlStr)
            .timeout(TIMEOUT_LIMIT)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .referrer("https://www.google.com")
            .get()

        // Extract Title from Meta Tags (og:title, twitter:title), header h1, or doc.title fallback
        val ogTitle = doc.select("meta[property=og:title]").attr("content").trim()
        val twitterTitle = doc.select("meta[name=twitter:title]").attr("content").trim()
        val h1Title = doc.select("h1").firstOrNull()?.text()?.trim() ?: ""
        val docTitle = doc.title().trim()

        var title = when {
            ogTitle.isNotEmpty() -> ogTitle
            twitterTitle.isNotEmpty() -> twitterTitle
            h1Title.isNotEmpty() -> h1Title
            else -> docTitle.ifEmpty { "Berita Bisnis" }
        }

        // Premium Title Cleaning: Strip common media suffixes
        val suffixes = listOf(
            " - Bisnis.com", " - CNBC Indonesia", " - Kompas.com", " - Detik Finance", " - Detik", 
            " | CNBC Indonesia", " | Kompas.com", " | Bisnis.com", " - Kontan.co.id", " | Kontan"
        )
        for (suffix in suffixes) {
            if (title.endsWith(suffix, ignoreCase = true)) {
                title = title.substring(0, title.length - suffix.length)
                break
            }
        }

        // Specific CSS Selektor-cascade used by popular news publishers
        val selectors = listOf(
            ".read__content",          // Kompas
            ".detail__body-text",      // Detik
            ".detail_text",            // CNBC Indonesia
            ".p_read",                 // Kontan
            ".p-read",                 // Kontan
            ".content-all",            // Bisnis.com
            "article",                 // Generic HTML5 article
            "[class*='detail-content']",
            "[class*='entry-content']",
            "[class*='article-body']",
            "[class*='post-content']",
            "[itemprop='articleBody']",
            "main"
        )

        var bestContent = ""
        var bestParagraphs = emptyList<String>()

        // Try selectors to extract only the actual article text
        for (selector in selectors) {
            val containers = doc.select(selector)
            if (containers.isNotEmpty()) {
                val paragraphs = mutableListOf<String>()
                for (container in containers) {
                    val pElements = container.select("p")
                    if (pElements.isNotEmpty()) {
                        paragraphs.addAll(pElements.map { it.text().trim() })
                    } else {
                        // Fallback if no <p> tags represent block contents
                        val directText = container.text().trim()
                        if (directText.isNotEmpty()) {
                            paragraphs.add(directText)
                        }
                    }
                }

                val cleaned = paragraphs
                    .map { cleanText(it) }
                    .filter { it.isNotEmpty() && it.length > 10 }
                
                val joined = cleaned.joinToString("\n\n")
                if (joined.length > bestContent.length) {
                    bestContent = joined
                    bestParagraphs = cleaned
                }

                // If content is substantial, we have isolated the real news body successfully
                if (joined.length >= 300) {
                    break
                }
            }
        }

        // Ultimate fallback to checking any paragraph under the document body
        if (bestContent.length < 150) {
            val allPElements = doc.body()?.select("p") ?: doc.select("p")
            val pTexts = allPElements.map { it.text().trim() }
                .map { cleanText(it) }
                .filter { it.isNotEmpty() && it.length > 10 }
            
            val joined = pTexts.joinToString("\n\n")
            if (joined.length > bestContent.length) {
                bestContent = joined
                bestParagraphs = pTexts
            }
        }

        if (bestContent.isEmpty()) {
            throw Exception("Gagal menarik konten tulisan berita dari link ini secara otomatis.")
        }

        return ScrapedNews(
            title = if (title.length > 100) title.take(97) + "..." else title,
            cleanContent = bestContent,
            domain = domain
        )
    }

    private fun cleanText(text: String): String {
        var temp = text.trim()
        
        // Discard any navigation paragraphs, reading guidelines, or pure layout junk completely
        if (temp.startsWith("baca juga", ignoreCase = true) ||
            temp.startsWith("simak juga", ignoreCase = true) ||
            temp.startsWith("simak selengkapnya", ignoreCase = true) ||
            temp.startsWith("baca selengkapnya", ignoreCase = true) ||
            temp.startsWith("baca berita tanpa iklan", ignoreCase = true) ||
            temp.startsWith("klik di sini", ignoreCase = true) ||
            temp.startsWith("pilihan editor", ignoreCase = true) ||
            temp.startsWith("baca:", ignoreCase = true) ||
            temp.equals("iklan", ignoreCase = true) ||
            temp.equals("advertisement", ignoreCase = true) ||
            temp.equals("ads", ignoreCase = true)
        ) {
            return ""
        }

        // Precise inline cleaning (replaces inside-text parenthesized suggestions safely)
        temp = temp.replace(Regex("(?i)\\(baca\\s*juga:?[^)]*\\)"), "")
        temp = temp.replace(Regex("(?i)\\[baca\\s*juga:?[^\\]]*\\]"), "")
        
        return temp.trim()
    }
}
