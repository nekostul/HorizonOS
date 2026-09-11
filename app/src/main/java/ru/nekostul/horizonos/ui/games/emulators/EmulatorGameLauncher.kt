package ru.nekostul.horizonos.ui.games.emulators

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import ru.nekostul.horizonos.R

interface EmulatorGameLauncher {
    val emulator: Emulator

    fun launch(context: Context, game: Game): GameLaunchResult
}

@Suppress("DEPRECATION")
internal fun launchRomIntent(
    context: Context,
    game: Game,
    packageName: String,
    activityName: String,
    intent: Intent
): GameLaunchResult {    val uri = Uri.parse(game.romUri)
    val readable = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { } != null
    }.getOrDefault(false)

    if (!readable) {
        return GameLaunchResult.Failed(
            context.getString(R.string.games_error_file_unavailable)
        )
    }

    val packageManager = context.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        ?: return GameLaunchResult.Failed(
            context.getString(R.string.games_error_emulator_not_installed, packageName)
        )

    val component = ComponentName(packageName, activityName)
    val explicitIntent = intent.setComponent(component)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        .apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Keeping the URI in data gives the target process a temporary
            // read grant even when the emulator reads its own launch extra.
            data = uri
        }

    val canResolve = explicitIntent.resolveActivity(packageManager) != null
    if (!canResolve && launchIntent.component == null) {
        return GameLaunchResult.Failed(
            context.getString(R.string.games_error_emulator_activity_unavailable)
        )
    }

    runCatching {
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        context.startActivity(explicitIntent)
        // Suppress the native activity-open animation.
        (context as? Activity)?.overridePendingTransition(0, 0)
    }.onFailure { error ->
        return GameLaunchResult.Failed(
            if (error is ActivityNotFoundException) {
                context.getString(R.string.games_error_open_failed, packageName)
            } else {
                context.getString(R.string.games_error_launch_failed)
            }
        )
    }

    return GameLaunchResult.Launched
}
