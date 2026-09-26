package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

/**
 * Starts a Windows game by launching the Android shortcut the runner app
 * (GameHub, GameNative, Winlator) published for it.
 */
class WindowsShortcutLauncher(override val emulator: Emulator) : EmulatorGameLauncher {

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = game.packageName?.takeIf { it.isNotBlank() }
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_shortcut_unavailable)
            )
        val shortcutId = game.launchActivity?.takeIf { it.isNotBlank() }
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_shortcut_unavailable)
            )
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_shortcut_unavailable)
            )

        return runCatching {
            launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
        }.fold(
            onSuccess = { GameLaunchResult.Launched },
            onFailure = { GameLaunchResult.Failed(context.getString(R.string.games_error_launch_failed)) }
        )
    }
}