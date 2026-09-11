package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperResult
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId

/**
 * Contract every scraping source implements. The main scraper only depends on
 * this interface, so new sources can be added without touching the pipeline.
 */
interface GameMetadataSource {
    /** Stable identity, also used to order sources by priority. */
    val id: ScraperSourceId

    /** Whether the user enabled this source and it has everything it needs. */
    fun isAvailable(settings: ScraperSettings): Boolean

    /**
     * Search the source for metadata. Only fields the source could resolve must
     * be non-null; the scraper continues with lower-priority sources for the
     * remaining fields.
     */
    suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult

    /**
     * Returns every selectable image the source can offer for the game, for the
     * manual cover/screenshot picker. Sources that cannot list variants return
     * an empty list.
     */
    suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> = emptyList()
}
