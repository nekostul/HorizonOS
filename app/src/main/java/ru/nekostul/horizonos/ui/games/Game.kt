package ru.nekostul.horizonos.ui.games

import java.util.UUID

data class Game(
    val id: String,
    val title: String,
    val platform: Platform,
    val emulator: Emulator,
    val romUri: String,
    val romName: String,
    val hidden: Boolean = false
) {
    val identityKey: String
        get() = "${emulator.name}|$romUri"

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
    }
}
