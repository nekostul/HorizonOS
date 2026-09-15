package ru.nekostul.horizonos.ui.games

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
