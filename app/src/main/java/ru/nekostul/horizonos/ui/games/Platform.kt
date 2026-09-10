package ru.nekostul.horizonos.ui.games

/**
 * A platform owns the ROM extensions accepted by the scanner. Keeping the
 * list here prevents the picker and the folder scanner from drifting apart.
 */
enum class Platform(
    val title: String,
    val romExtensions: Set<String>
) {
    PLAYSTATION_1(
        title = "PlayStation 1",
        romExtensions = setOf("bin", "cue", "chd", "iso", "pbp", "m3u")
    ),
    PSP(
        title = "PSP",
        romExtensions = setOf("iso", "cso", "chd", "pbp")
    ),
    PLAYSTATION_2(
        title = "PlayStation 2",
        romExtensions = setOf("iso", "chd", "cso")
    ),
    GAMECUBE_WII(
        title = "GameCube / Wii",
        romExtensions = setOf("iso", "gcm", "rvz", "gcz", "ciso", "wbfs", "wad")
    );

    fun supportsFileName(name: String?): Boolean {
        val extension = name
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?: return false
        return extension in romExtensions
    }
}
