package ru.nekostul.horizonos.ui.games

import java.util.UUID

data class Game(
    val id: String,
    val title: String,
    val platform: Platform,
    val emulator: Emulator,
    val romUri: String,
    val romName: String,
    val hidden: Boolean = false,
    val fullTitle: String? = null,
    val coverPath: String? = null,
    val screenshotPath: String? = null,
    val isTitleManuallySet: Boolean = false,
    val isCoverManuallySet: Boolean = false,
    val isScreenshotManuallySet: Boolean = false,
    val packageName: String? = null,
    val launchActivity: String? = null,
    val iconPath: String? = null,
    val fromDownload: Boolean = false
) {
    val identityKey: String
        get() = "${emulator.name}|$romUri"

    val displayTitle: String
        get() = fullTitle?.takeIf { it.isNotBlank() } ?: title

    companion object {
        fun fromRom(
            title: String,
            platform: Platform,
            emulator: Emulator,
            romUri: String,
            romName: String = title
        ): Game {
            val stableId = UUID.nameUUIDFromBytes(
                "${emulator.name}|$romUri".toByteArray(Charsets.UTF_8)
            ).toString()
            return Game(stableId, title, platform, emulator, romUri, romName)
        }

        fun fromAndroidApp(
            label: String,
            packageName: String,
            launchActivity: String?,
            iconPath: String?
        ): Game {
            val stableId = UUID.nameUUIDFromBytes(
                "ANDROID|$packageName".toByteArray(Charsets.UTF_8)
            ).toString()
            return Game(
                id = stableId,
                title = label,
                platform = Platform.ANDROID,
                emulator = Emulator.ANDROID,
                romUri = "package:$packageName",
                romName = label,
                packageName = packageName,
                launchActivity = launchActivity,
                iconPath = iconPath
            )
        }
    }
}
