package ru.nekostul.horizonos.ui.games

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the platforms for which the silent launch rescan just added new games,
 * so the launcher can show a small notice. Cleared once consumed.
 */
object NewGamesNotifier {
    private val _platforms = MutableStateFlow<List<Platform>>(emptyList())
    val platforms: StateFlow<List<Platform>> = _platforms.asStateFlow()

    fun publish(list: List<Platform>) {
        if (list.isNotEmpty()) _platforms.value = list
    }

    fun consume() {
        _platforms.value = emptyList()
    }
}
