package ru.nekostul.horizonos.ui.settings.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class NotificationController(private val context: Context) {
    val canPost: Boolean
        get() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    val canChangeOtherApps: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlNotifications

    fun areNotificationsEnabled(): Boolean = context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true
}
