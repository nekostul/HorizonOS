package ru.nekostul.horizonos.ui.settings.lockscreen

import android.content.Context
import android.provider.Settings
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess

class LockScreenController(private val context: Context) {
    val canChangeSystemTimeout: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canChangeSystemTimeout

    fun setSystemTimeout(milliseconds: Int): Boolean {
        if (!canChangeSystemTimeout) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("settings put system screen_off_timeout ${milliseconds.coerceAtLeast(0)}")?.succeeded == true
        }
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, milliseconds)
        }.getOrDefault(false)
    }

    fun currentSystemTimeout(): Long? = runCatching {
        Settings.System.getLong(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    }.getOrNull()
}
