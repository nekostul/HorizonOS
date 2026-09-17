package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File
import java.io.FileOutputStream

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val activityName: String?,
    val icon: Drawable?
)

class AndroidAppRepository(private val context: Context) {

    private val appIconDir = File(context.filesDir, "app_icons").apply { mkdirs() }

    fun installedApps(): List<InstalledAppInfo> =
        queryLaunchableApps(includeUsefulSystemApps = false)

    fun launchableApps(): List<InstalledAppInfo> =
        queryLaunchableApps(includeUsefulSystemApps = true)

    private fun queryLaunchableApps(includeUsefulSystemApps: Boolean): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            packageManager.queryIntentActivities(intent, 0)
        }.getOrDefault(emptyList())

        return resolved
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.applicationInfo != null }
            .filter { it.activityInfo.packageName != context.packageName }
            .filter { entry ->
                val info = entry.activityInfo.applicationInfo
                if (!isSystemApp(info)) return@filter true
                includeUsefulSystemApps && info.packageName in UsefulSystemApps
            }
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

    private fun isSystemApp(info: ApplicationInfo): Boolean {
        val flags = info.flags
        val isSystem = (flags and (ApplicationInfo.FLAG_SYSTEM or
            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        if (isSystem) return true
        val sourceDir = info.sourceDir ?: return false
        return sourceDir.startsWith("/system") || sourceDir.startsWith("/product") ||
            sourceDir.startsWith("/vendor") || sourceDir.startsWith("/system_ext") ||
            sourceDir.startsWith("/apex")
    }

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

    fun isInstalled(packageName: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(packageName) != null

    private companion object {
        val UsefulSystemApps = setOf(
            "com.android.settings",
            "com.android.chrome",
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.google.android.calendar",
            "com.android.calendar",
            "com.google.android.apps.maps",
            "com.google.android.gm",
            "com.google.android.apps.photos",
            "com.google.android.apps.docs",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.android.contacts",
            "com.google.android.contacts",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.android.gallery3d",
            "com.google.android.apps.wellbeing",
            "com.google.android.play.games",
            "com.android.vending",
            "com.google.android.apps.nbu.files",
            "com.google.android.documentsui",
            "com.android.documentsui",
            "com.google.android.music",
            "com.android.email",
            "com.google.android.apps.tachyon"
        )
    }
}
