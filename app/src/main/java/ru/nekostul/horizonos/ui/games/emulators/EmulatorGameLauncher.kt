package ru.nekostul.horizonos.ui.games.emulators

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import ru.nekostul.horizonos.R
import java.io.File

interface EmulatorGameLauncher {
    val emulator: Emulator

    fun launch(context: Context, game: Game): GameLaunchResult
}

internal fun launchRomIntent(
    context: Context,
    game: Game,
    packageName: String,
    activityName: String,
    intent: Intent,
    localFileUri: (File) -> Uri? = { file ->
        FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    }
): GameLaunchResult {
    val isContentUri = game.romUri.startsWith("content://")
    val uri = if (isContentUri) {
        Uri.parse(game.romUri)
    } else {
        val file = File(game.romUri)
        if (!file.canRead()) return GameLaunchResult.Failed(
            context.getString(R.string.games_error_file_unavailable)
        )
        runCatching { localFileUri(file) }.getOrNull()
    }
    if (uri == null) {
        return GameLaunchResult.Failed(
            context.getString(R.string.games_error_file_unavailable)
        )
    }

    val readable = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { } != null ||
            context.contentResolver.openInputStream(uri)?.use { } != null
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

    val explicitIntent = intent.setComponent(ComponentName(packageName, activityName))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        .apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(uri, type ?: "*/*")
        }

    runCatching {
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    val started = runCatching {
        context.startActivity(explicitIntent)
        (context as? Activity)?.overridePendingTransition(0, 0)
        true
    }.getOrDefault(false)

    if (started) return GameLaunchResult.Launched

    return runCatching {
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        true
    }.getOrDefault(false).let { launched ->
        if (launched) {
            GameLaunchResult.Launched
        } else {
            GameLaunchResult.Failed(
                context.getString(R.string.games_error_launch_failed)
            )
        }
    }
}

private const val FILE_PROVIDER_AUTHORITY = "ru.nekostul.horizonos.fileprovider"
