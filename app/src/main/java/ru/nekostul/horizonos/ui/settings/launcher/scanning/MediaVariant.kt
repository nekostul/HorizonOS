package ru.nekostul.horizonos.ui.settings.launcher.scanning

/** A single selectable image returned by a source. */
data class MediaVariant(
    val url: String,
    val source: ScraperSourceId,
    val type: MediaType,
    val width: Int = 0,
    val height: Int = 0
) {
    val isSquare: Boolean get() = width > 0 && width == height
    val hasDimensions: Boolean get() = width > 0 && height > 0
}

enum class MediaType { COVER, SCREENSHOT }
