package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.graphics.Bitmap
import ru.nekostul.horizonos.ui.games.Game
import java.io.File

class GameMetadataEditor(mediaDir: File) {

    private val imageProcessor = ImageProcessor(mediaDir)

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

    suspend fun applyLocalCover(game: Game, sourcePath: String): Game? {
        val path = imageProcessor.storeLocalCover(game.id, sourcePath) ?: return null
        return game.copy(coverPath = path, isCoverManuallySet = true)
    }

    suspend fun applyLocalScreenshot(game: Game, sourcePath: String): Game? {
        val path = imageProcessor.storeLocalScreenshot(game.id, sourcePath) ?: return null
        return game.copy(screenshotPath = path, isScreenshotManuallySet = true)
    }

    fun setTitle(game: Game, title: String): Game =
        game.copy(fullTitle = title.trim(), isTitleManuallySet = true)
}
