package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import ru.nekostul.horizonos.R

class NetherSx2Launcher : EmulatorGameLauncher {
    override val emulator = Emulator.NETHERSX2

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = PACKAGES.firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        } ?: return GameLaunchResult.Failed(context.getString(R.string.games_error_nethersx2_not_installed))

        return launchRomIntent(
            context = context,
            game = game,
            packageName = packageName,
            activityName = "$packageName.EmulationActivity",
            intent = Intent(Intent.ACTION_VIEW)
        )
    }

    private companion object {
        val PACKAGES = listOf(
            "xyz.aethersx2.android",
            "xyz.aethersx2.tturnip",
            "xyz.aethersx2.cturnip"
        )
    }
}
