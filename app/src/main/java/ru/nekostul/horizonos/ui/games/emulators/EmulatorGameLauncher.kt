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

interface EmulatorGameLauncher {
    val emulator: Emulator

    fun launch(context: Context, game: Game): GameLaunchResult
}

internal fun launchRomIntent(
    context: Context,
    game: Game,
    packageName: String,
    activityName: String,
    intent: Intent
): GameLaunchResult {
    val uri = Uri.parse(game.romUri)
    val readable = runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { } != null
    }.getOrDefault(false)

    if (!readable) {
        return GameLaunchResult.Failed(
            "Файл игры недоступен. Проверьте разрешение на доступ к ROM."
        )
    }

    val packageManager = context.packageManager
    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        ?: return GameLaunchResult.Failed("Эмулятор не установлен: $packageName")

    val component = ComponentName(packageName, activityName)
    val explicitIntent = intent.setComponent(component)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Keeping the URI in data gives the target process a temporary
            // read grant even when the emulator reads its own launch extra.
            data = uri
        }

    val canResolve = explicitIntent.resolveActivity(packageManager) != null
    if (!canResolve && launchIntent.component == null) {
        return GameLaunchResult.Failed(
            "Эмулятор установлен, но его экран запуска игры недоступен."
        )
    }

    runCatching {
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        context.startActivity(explicitIntent)
    }.onFailure { error ->
        return GameLaunchResult.Failed(
            if (error is ActivityNotFoundException) {
                "Не удалось открыть игру через $packageName."
            } else {
                "Не удалось запустить игру: ${error.message ?: "неизвестная ошибка"}"
            }
        )
    }

    return GameLaunchResult.Launched
}
