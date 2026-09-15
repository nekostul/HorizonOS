package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperResult
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId

interface GameMetadataSource {
    val id: ScraperSourceId

    fun isAvailable(settings: ScraperSettings): Boolean

    suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult

    suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> = emptyList()
}
