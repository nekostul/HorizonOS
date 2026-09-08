package ru.nekostul.horizonos.ui.settings.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class NotificationController(private val context: Context) {
    data class AppNotificationInfo(val label: String, val packageName: String)
    val canPost: Boolean
        get() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    val canChangeOtherApps: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlNotifications

    fun areNotificationsEnabled(): Boolean = context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true

    fun installedApps(): List<AppNotificationInfo> = runCatching {
        val packageManager = context.packageManager
        val applications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getInstalledApplications(0)
        }
        applications.map { info ->
            AppNotificationInfo(packageManager.getApplicationLabel(info).toString(), info.packageName)
        }.filter { it.packageName != context.packageName }.sortedBy { it.label.lowercase() }
    }.getOrDefault(emptyList())
}
