package ru.nekostul.horizonos.ui.games

sealed interface GameLaunchResult {
    data object Launched : GameLaunchResult

    data class Failed(val message: String) : GameLaunchResult
}
