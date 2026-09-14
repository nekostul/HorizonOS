package ru.nekostul.horizonos.ui.games.emulators

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import java.io.File

/**
 * Launches Nintendo Switch titles in Eden / Yuzu / Sudachi.
 *
 * Yuzu and its forks expose a dedicated `EmulationActivity`. Front-ends such as
 * ES-DE start it explicitly with the `android.nfc.action.TECH_DISCOVERED` action
 * and the ROM as data, so the emulator boots straight into the game instead of
 * its main menu.
 */
class SwitchEmulatorLauncher(override val emulator: Emulator) : EmulatorGameLauncher {

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val target = findInstalledTarget(context, emulator)
            ?: return GameLaunchResult.Failed(
                context.getString(R.string.games_error_switch_not_installed)
            )

        val uri = resolveUri(context, game)
            ?: return GameLaunchResult.Failed(context.getString(R.string.games_error_file_unavailable))
        if (!isReadable(context, game)) {
            return GameLaunchResult.Failed(context.getString(R.string.games_error_file_unavailable))
        }

        runCatching {
            context.grantUriPermission(
                target.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        // Preferred: the emulator's own emulation activity, as ES-DE does it.
        val explicit = Intent(ACTION_TECH_DISCOVERED).apply {
            component = ComponentName(target.packageName, target.activity)
            setDataAndType(uri, "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        if (startSafely(context, explicit)) return GameLaunchResult.Launched

        // Fallback: implicit VIEW for the package (works on some forks).
        val implicit = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setPackage(target.packageName)
            putExtra("AutoStartFile", game.romUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (startSafely(context, implicit)) return GameLaunchResult.Launched

        // Last resort: open the emulator itself so the user is not stuck.
        return if (openSettings(context, emulator)) {
            GameLaunchResult.Launched
        } else {
            GameLaunchResult.Failed(context.getString(R.string.games_error_switch_not_installed))
        }
    }

    private fun resolveUri(context: Context, game: Game): Uri? {
        if (game.romUri.startsWith("content://")) return Uri.parse(game.romUri)
        val file = File(game.romUri)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(context, AUTHORITY, file)
        }.getOrNull() ?: Uri.fromFile(file)
    }

    private fun isReadable(context: Context, game: Game): Boolean {
        if (game.romUri.startsWith("content://")) {
            return runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(game.romUri), "r")?.use { } != null
            }.getOrDefault(false)
        }
        return File(game.romUri).canRead()
    }

    private fun startSafely(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent) }.isSuccess

    companion object {
        private const val ACTION_TECH_DISCOVERED = "android.nfc.action.TECH_DISCOVERED"
        private const val AUTHORITY = "ru.nekostul.horizonos.fileprovider"

        private data class Target(val packageName: String, val activity: String)

        // Activity names follow the Yuzu fork layout. The EA build keeps the base
        // package for the activity class.
        private val TARGETS: Map<Emulator, List<Target>> = mapOf(
            Emulator.EDEN to listOf(
                Target("dev.eden.eden_emulator", "dev.eden.eden_emulator.activities.EmulationActivity"),
                Target("org.eden.eden_emulator", "org.eden.eden_emulator.activities.EmulationActivity")
            ),
            Emulator.YUZU to listOf(
                Target("org.yuzu.yuzu_emu", "org.yuzu.yuzu_emu.activities.EmulationActivity"),
                Target("org.yuzu.yuzu_emu.ea", "org.yuzu.yuzu_emu.activities.EmulationActivity")
            ),
            Emulator.SUDACHI to listOf(
                Target("org.sudachi.sudachi_emu", "org.sudachi.sudachi_emu.activities.EmulationActivity")
            )
        )

        private fun findInstalledTarget(context: Context, emulator: Emulator): Target? =
            TARGETS[emulator]?.firstOrNull {
                runCatching { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
                    .getOrDefault(false)
            }

        /** Opens the emulator's own UI so the user can finish its setup. */
        fun openSettings(context: Context, emulator: Emulator): Boolean {
            val target = findInstalledTarget(context, emulator) ?: return false
            val intent = context.packageManager.getLaunchIntentForPackage(target.packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(intent) }.isSuccess
        }
    }
}
