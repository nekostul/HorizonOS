package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class DuckStationLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.DUCKSTATION

    override fun launch(context: Context, game: Game): GameLaunchResult {
        return launchRomIntent(
            context = context,
            game = game,
            packageName = PACKAGE_NAME,
            activityName = "$PACKAGE_NAME.EmulationActivity",
            intent = Intent(Intent.ACTION_MAIN).apply {
                putExtra("bootPath", game.romUri)
                putExtra("resumeState", false)
            }
        )
    }

    private companion object {
        const val PACKAGE_NAME = "com.github.stenzek.duckstation"
    }
}
