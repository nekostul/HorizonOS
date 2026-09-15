package ru.nekostul.horizonos.ui.settings.launcher.scanning

import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.GameMetadataSource
import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.IgdbSource
import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.LibretroSource
import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.ScreenScraperSource
import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.SteamGridDbSource
import ru.nekostul.horizonos.ui.settings.launcher.scanning.sources.TheGamesDbSource

internal object ScraperSources {
    val ordered: List<GameMetadataSource> = listOf(
        ScreenScraperSource(),
        LibretroSource(),
        TheGamesDbSource(),
        IgdbSource(),
        SteamGridDbSource()
    )
}
