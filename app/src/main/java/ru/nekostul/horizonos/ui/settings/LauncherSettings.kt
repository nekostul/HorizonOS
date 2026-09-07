package ru.nekostul.horizonos.ui.settings

data class LauncherSettings(
    val animations: Boolean = true,
    val interfaceScale: Float = 1.0f,

    val showClock: Boolean = true,
    val showBattery: Boolean = true,

    val confirmGameLaunch: Boolean = true,
    val rememberLastGame: Boolean = true,

    val interfaceSounds: Boolean = true,

    val theme: String = "dark",
    val accentColor: String = "cyan"
)