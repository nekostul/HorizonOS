package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.Platform
import ru.nekostul.horizonos.ui.settings.launcher.scanning.LibretroCatalog
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaType
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperResult
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId
import ru.nekostul.horizonos.ui.settings.launcher.scanning.TitleMatcher
import ru.nekostul.horizonos.ui.settings.launcher.scanning.searchName

class LibretroSource : GameMetadataSource {

    override val id = ScraperSourceId.LIBRETRO

    override fun isAvailable(settings: ScraperSettings): Boolean = true

    private fun systemNames(platform: Platform): List<String> = when (platform) {
        Platform.PLAYSTATION_1 -> listOf("Sony - PlayStation")
        Platform.PSP -> listOf("Sony - PlayStation Portable")
        Platform.PLAYSTATION_2 -> listOf("Sony - PlayStation 2")
        Platform.GAMECUBE_WII -> listOf("Nintendo - GameCube", "Nintendo - Wii")
        Platform.NINTENDO_SWITCH -> listOf("Nintendo - Nintendo Switch")
        Platform.ANDROID -> emptyList()
    }

    override suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult {
        val query = game.searchName()
        var bestTitle: String? = null
        var cover: String? = null
        var screenshot: String? = null

        for (system in systemNames(game.platform)) {
            if (cover == null) {
                val entry = LibretroCatalog.best(system, "Named_Boxarts", query)
                if (entry != null) {
                    cover = thumbnailUrl(system, "Named_Boxarts", entry.href)
                    bestTitle = bestTitle ?: cleanTitle(entry.displayName)
                }
            }
            if (screenshot == null) {
                val snaps = LibretroCatalog.best(system, "Named_Snaps", query)
                if (snaps != null) {
                    screenshot = thumbnailUrl(system, "Named_Snaps", snaps.href)
                } else {
                    val titles = LibretroCatalog.best(system, "Named_Titles", query)
                    if (titles != null) screenshot = thumbnailUrl(system, "Named_Titles", titles.href)
                }
            }
        }

        return ScraperResult(fullTitle = bestTitle, coverUrl = cover, screenshotUrl = screenshot)
    }

    override suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> {
        val query = game.searchName()
        val result = mutableListOf<MediaVariant>()
        for (system in systemNames(game.platform)) {
            LibretroCatalog.matches(system, "Named_Boxarts", query).forEach { entry ->
                result += MediaVariant(thumbnailUrl(system, "Named_Boxarts", entry.href), id, MediaType.COVER)
            }
            LibretroCatalog.matches(system, "Named_Snaps", query).forEach { entry ->
                result += MediaVariant(thumbnailUrl(system, "Named_Snaps", entry.href), id, MediaType.SCREENSHOT)
            }
        }
        return result
    }

    private fun cleanTitle(displayName: String): String? =
        TitleMatcher.searchQuery(displayName.removeSuffix(".png")).takeIf { it.isNotBlank() }

    private fun thumbnailUrl(system: String, type: String, href: String): String =
        "https://thumbnails.libretro.com/${java.net.URLEncoder.encode(system, "UTF-8").replace("+", "%20")}/$type/$href"
}
