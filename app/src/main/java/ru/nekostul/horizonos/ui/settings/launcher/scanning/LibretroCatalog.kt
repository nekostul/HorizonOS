package ru.nekostul.horizonos.ui.settings.launcher.scanning

import java.net.URLDecoder
import java.net.URLEncoder

internal object LibretroCatalog {

    data class Entry(val displayName: String, val href: String)

    private val cache = mutableMapOf<String, List<Entry>>()

    suspend fun matches(system: String, type: String, query: String, limit: Int = 12): List<Entry> {
        val entries = listing(system, type) ?: return emptyList()
        return entries
            .mapNotNull { entry ->
                val cleaned = TitleMatcher.searchQuery(entry.displayName.removeSuffix(".png"))
                val score = TitleMatcher.matchScore(query, cleaned)
                if (score >= 80) Scored(entry, score, preference(entry.displayName)) else null
            }
            .sortedWith(compareByDescending<Scored> { it.score }.thenByDescending { it.preference })
            .take(limit)
            .map { it.entry }
    }

    suspend fun best(system: String, type: String, query: String): Entry? =
        matches(system, type, query, limit = 1).firstOrNull()

    private data class Scored(val entry: Entry, val score: Int, val preference: Int)

    private fun preference(name: String): Int {
        var result = 0
        val lower = name.lowercase()
        if (lower.contains("(usa)") || lower.contains("(europe)") || lower.contains("(world)")) result += 4
        if (!lower.contains("(disc")) result += 2
        if (!lower.contains("(rev")) result += 2
        if (lower.contains("(japan)")) result -= 1
        return result
    }

    private suspend fun listing(system: String, type: String): List<Entry>? {
        val key = "$system|$type"
        cache[key]?.let { return it }
        val url = "https://thumbnails.libretro.com/${encodePath(system)}/$type/"
        val html = HttpClient.getText(url) ?: return null
        val entries = Regex("href=\"([^\"]+\\.png)\"", RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { match ->
                val href = match.groupValues[1]
                val decoded = runCatching { URLDecoder.decode(href, "UTF-8") }.getOrDefault(href)
                Entry(displayName = decoded, href = href)
            }
            .toList()
        if (entries.isNotEmpty()) cache[key] = entries
        return entries
    }

    private fun encodePath(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
