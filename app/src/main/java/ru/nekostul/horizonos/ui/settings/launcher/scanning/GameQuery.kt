package ru.nekostul.horizonos.ui.settings.launcher.scanning

import ru.nekostul.horizonos.ui.games.Game

internal fun Game.searchName(): String =
    fullTitle?.takeIf { it.isNotBlank() } ?: TitleMatcher.searchQuery(romName)
