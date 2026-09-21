package ru.nekostul.horizonos.ui.games.emulators

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import java.io.File

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

        // Try explicit activity with TECH_DISCOVERED
        val baseFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        val explicit = Intent(ACTION_TECH_DISCOVERED).apply {
            component = ComponentName(target.packageName, target.activity)
            setDataAndType(uri, "*/*")
            addFlags(baseFlags)
        }
        if (startSafely(context, explicit)) return GameLaunchResult.Launched

        // Try VIEW with known extra
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setPackage(target.packageName)
            putExtra("AutoStartFile", game.romUri)
            addFlags(baseFlags)
        }
        if (startSafely(context, viewIntent)) return GameLaunchResult.Launched

        // Try dynamic activity discovery — query which activities can handle VIEW with the file
        val discoveryIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "*/*")
            setPackage(target.packageName)
        }
        val matchingActivities = context.packageManager.queryIntentActivities(
            discoveryIntent, PackageManager.MATCH_DEFAULT_ONLY
        )
        for (resolveInfo in matchingActivities) {
            val launchIntent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name
                )
                setDataAndType(uri, "*/*")
                putExtra("AutoStartFile", game.romUri)
                putExtra("GamePath", game.romUri)
                addFlags(baseFlags)
            }
            if (startSafely(context, launchIntent)) return GameLaunchResult.Launched
        }

        // Try MAIN activity with extras
        val mainIntent = context.packageManager.getLaunchIntentForPackage(target.packageName)
            ?.apply {
                setDataAndType(uri, "*/*")
                putExtra("AutoStartFile", game.romUri)
                putExtra("GamePath", game.romUri)
                putExtra("file", game.romUri)
                addFlags(baseFlags)
            }
        if (mainIntent != null && startSafely(context, mainIntent)) return GameLaunchResult.Launched

        // Last resort: try to open emulator app (game will not be auto-started)
        if (openSettings(context, emulator)) {
            return GameLaunchResult.Failed(
                context.getString(R.string.games_error_launch_failed)
            )
        }
        return GameLaunchResult.Failed(context.getString(R.string.games_error_switch_not_installed))
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

        private val TARGETS: Map<Emulator, List<Target>> = mapOf(
            Emulator.EDEN to listOf(
                Target("dev.eden.eden_emulator", "org.yuzu.yuzu_emu.activities.EmulationActivity"),
                Target("dev.legacy.eden_emulator", "org.yuzu.yuzu_emu.activities.EmulationActivity"),
                Target("com.miHoYo.Yuanshen", "org.yuzu.yuzu_emu.activities.EmulationActivity")
            ),
            Emulator.YUZU to listOf(
                Target("org.yuzu.yuzu_emu", "org.yuzu.yuzu_emu.activities.EmulationActivity"),
                Target("org.yuzu.yuzu_emu.ea", "org.yuzu.yuzu_emu.activities.EmulationActivity")
            ),
            Emulator.SUDACHI to listOf(
                Target("org.sudachi.sudachi_emu", "org.sudachi.sudachi_emu.activities.EmulationActivity")
            )
        )

        private fun findInstalledTarget(context: Context, emulator: Emulator): Target? {
            val configured = TARGETS[emulator]?.firstOrNull {
                runCatching { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
                    .getOrDefault(false)
            }
            if (configured != null) return configured

            // Auto-detect: scan installed packages for emulator-specific markers
            if (emulator == Emulator.EDEN) {
                return findEdenPackage(context)
            }
            return null
        }

        private fun findEdenPackage(context: Context): Target? {
            val pm = context.packageManager
            val edenPackages = listOf(
                "dev.eden.eden_emulator",
                "dev.legacy.eden_emulator",
                "com.miHoYo.Yuanshen"
            )
            for (pkg in edenPackages) {
                val launchIntent = runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull()
                if (launchIntent != null) {
                    return Target(
                        pkg,
                        launchIntent.component?.className
                            ?: "org.yuzu.yuzu_emu.activities.EmulationActivity"
                    )
                }
            }
            return null
        }

        fun openSettings(context: Context, emulator: Emulator): Boolean {
            val target = findInstalledTarget(context, emulator) ?: return false
            val intent = context.packageManager.getLaunchIntentForPackage(target.packageName)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { context.startActivity(intent) }.isSuccess
        }
    }
}