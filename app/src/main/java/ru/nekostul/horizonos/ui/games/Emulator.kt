package ru.nekostul.horizonos.ui.games

import ru.nekostul.horizonos.R

enum class Emulator(
    val titleRes: Int,
    val platform: Platform,
    val requiresBios: Boolean = false,
    /** Switch emulators need a first-run setup before games can boot. */
    val requiresConfiguration: Boolean = false
) {
    DUCKSTATION(R.string.games_emulator_duckstation, Platform.PLAYSTATION_1, requiresBios = true),
    PPSSPP(R.string.games_emulator_ppsspp, Platform.PSP),
    NETHERSX2(R.string.games_emulator_nethersx2, Platform.PLAYSTATION_2, requiresBios = true),
    DOLPHIN(R.string.games_emulator_dolphin, Platform.GAMECUBE_WII),
    EDEN(R.string.games_emulator_eden, Platform.NINTENDO_SWITCH, requiresConfiguration = true),
    YUZU(R.string.games_emulator_yuzu, Platform.NINTENDO_SWITCH, requiresConfiguration = true),
    SUDACHI(R.string.games_emulator_sudachi, Platform.NINTENDO_SWITCH, requiresConfiguration = true),
    ANDROID(R.string.games_platform_android, Platform.ANDROID)
}
