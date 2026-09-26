package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Reads the Android launcher shortcuts ("ярлыки") created by Windows game
 * runners such as GameHub, GameNative and Winlator.
 *
 * These apps publish a shortcut per game through [android.content.pm.ShortcutManager].
 * Because HorizonOS is a home launcher, it can enumerate those shortcuts with
 * [LauncherApps] and start them, and the package that owns the shortcut tells us
 * where it came from, so every such shortcut becomes a Windows game.
 */
class WindowsShortcutRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    private val iconDir = File(context.filesDir, "shortcut_icons").apply { mkdirs() }

    fun scan(): List<Game> {
        val apps = launcherApps ?: return emptyList()
        val user = Process.myUserHandle()
        val games = mutableListOf<Game>()

        candidatePackages().forEach { packageName ->
            val shortcuts = runCatching {
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                }
                apps.getShortcuts(query, user)
            }.onFailure {
                Log.d(TAG, "getShortcuts($packageName) failed: ${it.javaClass.simpleName}")
            }.getOrNull().orEmpty()

            shortcuts.forEach { info -> gameFrom(info)?.let { games += it } }
        }
        return games.distinctBy { it.identityKey }
    }

    fun gameFrom(info: ShortcutInfo): Game? {
        val packageName = info.getPackage()?.takeIf { it.isNotBlank() } ?: return null
        val emulator = emulatorFor(packageName) ?: return null
        val shortcutId = info.id?.takeIf { it.isNotBlank() } ?: return null
        val label = (info.shortLabel ?: info.longLabel)
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: shortcutId
        val game = Game.fromWindowsShortcut(
            label = label,
            emulator = emulator,
            packageName = packageName,
            shortcutId = shortcutId
        )
        return game.copy(iconPath = persistIcon(info, game.id))
    }

    private fun persistIcon(info: ShortcutInfo, gameId: String): String? {
        val apps = launcherApps ?: return null
        val density = context.resources.displayMetrics.densityDpi
        val drawable = runCatching { apps.getShortcutIconDrawable(info, density) }
            .getOrNull() ?: return null
        val bitmap = drawableToSquareBitmap(drawable) ?: return null
        val file = File(iconDir, "$gameId.png")
        return runCatching {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun drawableToSquareBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
        val raster = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            runCatching {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }.getOrNull()
        } ?: return null

        val side = minOf(raster.width, raster.height)
        if (side <= 0) return null
        val x = (raster.width - side) / 2
        val y = (raster.height - side) / 2
        val cropped = Bitmap.createBitmap(raster, x, y, side, side)
        val target = 192
        return if (side == target) {
            cropped
        } else {
            val scaled = Bitmap.createScaledBitmap(cropped, target, target, true)
            if (scaled != cropped) cropped.recycle()
            scaled
        }
    }

    /** Installed packages that look like a Windows game runner. */
    private fun candidatePackages(): List<String> {
        val installed = runCatching {
            context.packageManager.getInstalledPackages(0).map { it.packageName }
        }.getOrDefault(emptyList())
        return installed.filter { packageName ->
            val lower = packageName.lowercase(Locale.ROOT)
            Keywords.any { lower.contains(it) }
        }
    }

    private fun emulatorFor(packageName: String): Emulator? {
        val lower = packageName.lowercase(Locale.ROOT)
        return when {
            lower.contains("gamenative") -> Emulator.GAMENATIVE
            lower.contains("winlator") -> Emulator.WINLATOR
            lower.contains("gamehub") || lower.contains("banner") || lower.contains("xiaoji") ->
                Emulator.GAMEHUB
            else -> null
        }
    }

    private companion object {
        const val TAG = "WindowsShortcut"

        val Keywords = listOf("gamenative", "gamehub", "banner", "xiaoji", "winlator")
    }
}