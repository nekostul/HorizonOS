package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import java.io.File

class DuckStationLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.DUCKSTATION

    override fun launch(context: Context, game: Game): GameLaunchResult {
        return launchRomIntent(
            context = context,
            game = resolveBootFile(game),
            packageName = PACKAGE_NAME,
            activityName = "$PACKAGE_NAME.EmulationActivity",
            intent = Intent(Intent.ACTION_VIEW)
        )
    }

    private fun resolveBootFile(game: Game): Game {
        if (!game.romUri.endsWith(".cue", ignoreCase = true)) return game
        if (game.romUri.startsWith("content://")) return game
        return runCatching {
            val cue = File(game.romUri)
            val dir = cue.parentFile ?: return game
            cue.useLines { lines ->
                for (line in lines) {
                    val match = FILE_PATTERN.find(line) ?: continue
                    val referenced = File(dir, match.groupValues[1])
                    if (referenced.canRead()) {
                        return game.copy(romUri = referenced.absolutePath, romName = referenced.name)
                    }
                    return game
                }
            }
            game
        }.getOrDefault(game)
    }

    private companion object {
        const val PACKAGE_NAME = "com.github.stenzek.duckstation"
        val FILE_PATTERN = Regex("""(?i)^\s*FILE\s+"([^"]+)"""")
    }
}
