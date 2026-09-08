package ru.nekostul.horizonos.ui.settings

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class SystemCapabilities(
    val canControlAirplaneMode: Boolean,
    val canControlWifi: Boolean,
    val canControlBluetooth: Boolean,
    val canChangeSystemBrightness: Boolean,
    val canChangeSystemTimeout: Boolean,
    val canControlNotifications: Boolean,
    val canEnterSleep: Boolean,
    val isDeviceOwner: Boolean,
    val isPrivilegedApp: Boolean,
    val hasRootAccess: Boolean = false
)

object SystemCapabilitiesDetector {
    fun detect(context: Context): SystemCapabilities {
        val devicePolicy = context.getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = devicePolicy?.isDeviceOwnerApp(context.packageName) == true
        val appInfo = context.applicationInfo
        val isPrivilegedApp = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 ||
            appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        val canBrightness = Settings.System.canWrite(context)
        val bluetoothPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

        return SystemCapabilities(
            canControlAirplaneMode = isPrivilegedApp,
            canControlWifi = isPrivilegedApp || isDeviceOwner,
            canControlBluetooth = isPrivilegedApp,
            canChangeSystemBrightness = canBrightness,
            canChangeSystemTimeout = canBrightness,
            canControlNotifications = isPrivilegedApp,
            canEnterSleep = isPrivilegedApp,
            isDeviceOwner = isDeviceOwner,
            isPrivilegedApp = isPrivilegedApp,
            hasRootAccess = false
        )
    }
}
