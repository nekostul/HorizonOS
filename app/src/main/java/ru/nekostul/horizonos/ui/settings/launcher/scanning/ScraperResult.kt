package ru.nekostul.horizonos.ui.settings.launcher.scanning

/**
 * A single field resolved from one source. A null value means the source did
 * not provide this field and the scraper must continue with the next source.
 */
data class ScraperResult(
    val fullTitle: String? = null,
    val coverUrl: String? = null,
    val screenshotUrl: String? = null
) {
    fun isEmpty(): Boolean = fullTitle.isNullOrBlank() && coverUrl.isNullOrBlank() && screenshotUrl.isNullOrBlank()
}
