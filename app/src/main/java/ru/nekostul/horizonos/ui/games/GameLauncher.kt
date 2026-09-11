package ru.nekostul.horizonos.ui.games

import android.content.Context
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.emulators.AndroidAppLauncher
import ru.nekostul.horizonos.ui.games.emulators.DolphinLauncher
import ru.nekostul.horizonos.ui.games.emulators.DuckStationLauncher
import ru.nekostul.horizonos.ui.games.emulators.EmulatorGameLauncher
import ru.nekostul.horizonos.ui.games.emulators.NetherSx2Launcher
import ru.nekostul.horizonos.ui.games.emulators.PpssppLauncher

class GameLauncher(
    launchers: List<EmulatorGameLauncher> = listOf(
        DuckStationLauncher(),
        PpssppLauncher(),
        NetherSx2Launcher(),
        DolphinLauncher(),
        AndroidAppLauncher()
    )
) {
    private val launchersByEmulator = launchers.associateBy { it.emulator }

    fun launch(context: Context, game: Game): GameLaunchResult {
        return launchersByEmulator[game.emulator]?.launch(context, game)
            ?: GameLaunchResult.Failed(context.getString(R.string.games_error_unsupported_emulator))
    }
}
