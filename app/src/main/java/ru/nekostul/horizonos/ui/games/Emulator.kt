package ru.nekostul.horizonos.ui.games

enum class Emulator(
    val title: String,
    val platform: Platform,
    val requiresBios: Boolean = false
) {
    DUCKSTATION("DuckStation", Platform.PLAYSTATION_1, requiresBios = true),
    PPSSPP("PPSSPP", Platform.PSP),
    NETHERSX2("NetherSX2", Platform.PLAYSTATION_2, requiresBios = true),
    DOLPHIN("Dolphin", Platform.GAMECUBE_WII)
}
