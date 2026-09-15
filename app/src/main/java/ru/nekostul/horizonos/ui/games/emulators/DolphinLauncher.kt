package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class DolphinLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.DOLPHIN

    override fun launch(context: Context, game: Game): GameLaunchResult {
        return launchRomIntent(
            context = context,
            game = game,
            packageName = PACKAGE_NAME,
            activityName = "$PACKAGE_NAME.ui.main.MainActivity",
            intent = Intent(Intent.ACTION_MAIN),
            localFileUri = { file -> RomContentProvider.uriForFile(file) }
        )
    }

    private companion object {
        const val PACKAGE_NAME = "org.dolphinemu.dolphinemu"
    }
}
