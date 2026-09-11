package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream

/** A launchable, user-installed Android application. */
data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val activityName: String?,
    val icon: Drawable?
)

/**
 * Lists normal user applications that expose a launcher entry. System apps and
 * the HorizonOS launcher itself are excluded.
 */
class AndroidAppRepository(private val context: Context) {

    private val appIconDir = File(context.filesDir, "app_icons").apply { mkdirs() }

    fun installedApps(): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            packageManager.queryIntentActivities(intent, 0)
        }.getOrDefault(emptyList())

        return resolved
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.applicationInfo != null }
            .filter { !isSystemApp(it.activityInfo.applicationInfo) }
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                InstalledAppInfo(
                    label = runCatching { info.loadLabel(packageManager).toString() }
                        .getOrDefault(info.activityInfo.packageName),
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    icon = runCatching { info.loadIcon(packageManager) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /**
     * True when the app is part of the system image or preinstalled by the
     * vendor. Such apps are hidden from the picker to avoid launcher/settings
     * components leaking into the library.
     */
    private fun isSystemApp(info: ApplicationInfo): Boolean {
        val flags = info.flags
        val isSystem = (flags and (ApplicationInfo.FLAG_SYSTEM or
            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        if (isSystem) return true
        // Belt-and-braces: apps installed in the system partition are treated
        // as system even when their flags look ambiguous.
        val sourceDir = info.sourceDir ?: return false
        return sourceDir.startsWith("/system") || sourceDir.startsWith("/product") ||
            sourceDir.startsWith("/vendor") || sourceDir.startsWith("/system_ext") ||
            sourceDir.startsWith("/apex")
    }

    /** Saves the app icon as a square PNG and returns its absolute path. */
    fun persistIcon(info: InstalledAppInfo): String? {
        val drawable = info.icon ?: return null
        val bitmap = drawableToSquareBitmap(drawable) ?: return null
        val file = File(appIconDir, "${info.packageName}.png")
        return runCatching {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file.absolutePath
        }.getOrNull()
    }

    /**
     * Renders the drawable into a square bitmap using center-crop, so icons of
     * any aspect ratio are not stretched.
     */
    private fun drawableToSquareBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 192
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 192
        val raster = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            runCatching {
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = Canvas(bmp)
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

    /** True when [packageName] is still installed and has a launcher intent. */
    fun isInstalled(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null
}
