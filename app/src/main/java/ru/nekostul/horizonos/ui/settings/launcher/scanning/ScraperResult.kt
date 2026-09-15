package ru.nekostul.horizonos.ui.settings.launcher.scanning

data class ScraperResult(
    val fullTitle: String? = null,
    val coverUrl: String? = null,
    val screenshotUrl: String? = null
) {
    fun isEmpty(): Boolean = fullTitle.isNullOrBlank() && coverUrl.isNullOrBlank() && screenshotUrl.isNullOrBlank()
}
