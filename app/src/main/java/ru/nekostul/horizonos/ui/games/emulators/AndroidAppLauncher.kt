package ru.nekostul.horizonos.ui.games.emulators

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class AndroidAppLauncher : EmulatorGameLauncher {

    override val emulator: Emulator = Emulator.ANDROID

    @Suppress("DEPRECATION")
    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = game.packageName?.takeIf { it.isNotBlank() }
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_app_not_installed, game.displayTitle)
            )

        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_app_not_installed, game.displayTitle)
            )

        val explicit = game.launchActivity
            ?.takeIf { it.isNotBlank() }
            ?.let { ComponentName(packageName, it) }

        val intent = Intent(launchIntent).apply {
            if (explicit != null &&
                Intent(this).setComponent(explicit).resolveActivity(packageManager) != null
            ) {
                component = explicit
            }
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            context.startActivity(intent)
            (context as? Activity)?.overridePendingTransition(0, 0)
        }.fold(
            onSuccess = { GameLaunchResult.Launched },
            onFailure = { error ->
                if (error is ActivityNotFoundException) {
                    GameLaunchResult.Failed(
                        context.getString(R.string.games_error_app_not_installed, game.displayTitle)
                    )
                } else {
                    GameLaunchResult.Failed(context.getString(R.string.games_error_launch_failed))
                }
            }
        )
    }
}
