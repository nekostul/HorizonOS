package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class PpssppLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.PPSSPP

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = PACKAGES.firstOrNull {
            context.packageManager.getLaunchIntentForPackage(it) != null
        } ?: return GameLaunchResult.Failed("Эмулятор PPSSPP не установлен.")

        return launchRomIntent(
            context = context,
            game = game,
            packageName = packageName,
            activityName = "$packageName.PpssppActivity",
            intent = Intent(Intent.ACTION_VIEW).apply {
                type = "application/octet-stream"
            }
        )
    }

    private companion object {
        val PACKAGES = listOf(
            "org.ppsspp.ppsspp",
            "org.ppsspp.ppssppgold"
        )
    }
}
