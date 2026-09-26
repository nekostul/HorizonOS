package ru.nekostul.horizonos.ui.games.emulators

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import java.io.File

class DolphinLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.DOLPHIN

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val packageName = KNOWN_PACKAGES.firstOrNull {
            runCatching { context.packageManager.getLaunchIntentForPackage(it) }.getOrNull() != null
        } ?: return GameLaunchResult.Failed(
            context.getString(R.string.games_error_emulator_not_installed, "Dolphin")
        )

        val uri = resolveUri(context, game)
            ?: return GameLaunchResult.Failed(context.getString(R.string.games_error_file_unavailable))
        if (!isReadable(context, uri)) {
            return GameLaunchResult.Failed(context.getString(R.string.games_error_file_unavailable))
        }

        runCatching {
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_NO_ANIMATION or
            Intent.FLAG_ACTIVITY_NEW_TASK

        val activityCandidates = buildList {
            add("$packageName.ui.main.MainActivity")
            add("org.dolphinemu.dolphinemu.ui.main.MainActivity")
            add("$packageName.EmulationActivity")
        }

        for (activityName in activityCandidates) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(packageName, activityName)
                setDataAndType(uri, "*/*")
                addFlags(flags)
            }
            if (startSafely(context, intent)) return GameLaunchResult.Launched
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setPackage(packageName)
            addFlags(flags)
        }
        if (startSafely(context, viewIntent)) return GameLaunchResult.Launched

        return GameLaunchResult.Failed(
            context.getString(R.string.games_error_launch_failed)
        )
    }

    private fun resolveUri(context: Context, game: Game): Uri? {
        if (game.romUri.startsWith("content://")) return Uri.parse(game.romUri)
        val file = File(game.romUri)
        if (!file.canRead()) return null
        return RomContentProvider.uriForFile(file)
    }

    private fun isReadable(context: Context, uri: Uri): Boolean {
        if (uri.scheme == "content") {
            return runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { } != null
            }.getOrDefault(false)
        }
        return File(uri.path ?: return false).canRead()
    }

    private fun startSafely(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess

    private companion object {
        val KNOWN_PACKAGES = listOf(
            "org.dolphinemu.dolphinemu",
            "org.dolphinemu.mmjr",
            "org.dolphinemu.mmjr2"
        )
    }
}