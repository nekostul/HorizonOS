package ru.nekostul.horizonos.ui.games

import ru.nekostul.horizonos.R

/**
 * A platform owns the ROM extensions accepted by the scanner. Keeping the
 * list here prevents the picker and the folder scanner from drifting apart.
 */
enum class Platform(
    val titleRes: Int,
    val romExtensions: Set<String>
) {
    PLAYSTATION_1(
        titleRes = R.string.games_platform_ps1,
        romExtensions = setOf("bin", "cue", "chd", "iso", "pbp", "m3u")
    ),
    PSP(
        titleRes = R.string.games_platform_psp,
        romExtensions = setOf("iso", "cso", "chd", "pbp")
    ),
    PLAYSTATION_2(
        titleRes = R.string.games_platform_ps2,
        romExtensions = setOf("iso", "chd", "cso")
    ),
    GAMECUBE_WII(
        titleRes = R.string.games_platform_gamecube_wii,
        romExtensions = setOf("iso", "gcm", "rvz", "gcz", "ciso", "wbfs", "wad")
    ),
    ANDROID(
        titleRes = R.string.games_platform_android,
        romExtensions = emptySet()
    );

    fun supportsFileName(name: String?): Boolean {
        val extension = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?: return false
        return extension in romExtensions
    }
}
