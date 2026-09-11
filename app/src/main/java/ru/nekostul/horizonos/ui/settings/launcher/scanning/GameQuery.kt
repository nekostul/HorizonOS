package ru.nekostul.horizonos.ui.settings.launcher.scanning

import ru.nekostul.horizonos.ui.games.Game

/**
 * The name used for scraping requests. A manually set (or previously scraped)
 * title is preferred; otherwise the raw ROM name is normalized.
 */
internal fun Game.searchName(): String =
    fullTitle?.takeIf { it.isNotBlank() } ?: TitleMatcher.searchQuery(romName)
