package ru.nekostul.horizonos.ui.settings.lockscreen

import android.content.Context
import android.provider.Settings
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class LockScreenController(private val context: Context) {
    val canChangeSystemTimeout: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canChangeSystemTimeout

    fun setSystemTimeout(milliseconds: Int): Boolean {
        if (!canChangeSystemTimeout) return false
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, milliseconds)
        }.getOrDefault(false)
    }
}
