package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.graphics.Bitmap
import ru.nekostul.horizonos.ui.games.Game
import java.io.File

/**
 * Manual metadata editing: lists every available image variant and applies the
 * user's choice. Any manual choice locks the field so the automatic scraper
 * will not overwrite it later.
 */
class GameMetadataEditor(mediaDir: File) {

    private val imageProcessor = ImageProcessor(mediaDir)

    /** Variants for [type] from every enabled and configured source, in priority order. */
    suspend fun loadVariants(
        settings: ScraperSettings,
        game: Game,
        type: MediaType
    ): List<MediaVariant> {
        val variants = mutableListOf<MediaVariant>()
        ScraperSources.ordered.forEach { source ->
            if (!settings.isEnabled(source.id) || !source.isAvailable(settings)) return@forEach
            val sourceVariants = runCatching { source.searchVariants(game, settings) }
                .getOrDefault(emptyList())
            variants += sourceVariants.filter { it.type == type }
        }
        // Prefer 1:1 covers without changing the source priority order.
        return if (type == MediaType.COVER) {
            variants.sortedBy { !it.isSquare }
        } else {
            variants
        }
    }

    suspend fun loadThumbnail(url: String): Bitmap? = imageProcessor.loadThumbnail(url)

    suspend fun applyCover(game: Game, variant: MediaVariant): Game? {
        val path = imageProcessor.storeCover(game.id, variant.url) ?: return null
        return game.copy(coverPath = path, isCoverManuallySet = true)
    }

    suspend fun applyScreenshot(game: Game, variant: MediaVariant): Game? {
        val path = imageProcessor.storeScreenshot(game.id, variant.url) ?: return null
        return game.copy(screenshotPath = path, isScreenshotManuallySet = true)
    }

    /** Applies a user-picked local image as the cover (1:1, no stretching). */
    suspend fun applyLocalCover(game: Game, sourcePath: String): Game? {
        val path = imageProcessor.storeLocalCover(game.id, sourcePath) ?: return null
        return game.copy(coverPath = path, isCoverManuallySet = true)
    }

    /** Applies a user-picked local image as the screenshot (original ratio). */
    suspend fun applyLocalScreenshot(game: Game, sourcePath: String): Game? {
        val path = imageProcessor.storeLocalScreenshot(game.id, sourcePath) ?: return null
        return game.copy(screenshotPath = path, isScreenshotManuallySet = true)
    }

    /** Manual title override. The ROM file is never touched. */
    fun setTitle(game: Game, title: String): Game =
        game.copy(fullTitle = title.trim(), isTitleManuallySet = true)
}
