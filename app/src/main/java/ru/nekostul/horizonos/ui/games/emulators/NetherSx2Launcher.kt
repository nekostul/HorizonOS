package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class NetherSx2Launcher : EmulatorGameLauncher {
    override val emulator = Emulator.NETHERSX2

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = PACKAGES.firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        } ?: return GameLaunchResult.Failed("Эмулятор NetherSX2 не установлен.")

        return launchRomIntent(
            context = context,
            game = game,
            packageName = packageName,
            activityName = "$packageName.EmulationActivity",
            intent = Intent(Intent.ACTION_MAIN).apply {
                putExtra("bootPath", game.romUri)
                putExtra("resumeState", false)
            }
        )
    }

    private companion object {
        // NetherSX2 keeps the AetherSX2 application id. The turnip variants
        // are included because they use the same documented launch activity.
        val PACKAGES = listOf(
            "xyz.aethersx2.android",
            "xyz.aethersx2.tturnip",
            "xyz.aethersx2.cturnip"
        )
    }
}
