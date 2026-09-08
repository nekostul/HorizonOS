package ru.nekostul.horizonos.ui.settings.brightness

import android.content.Context
import android.provider.Settings
import android.view.Window
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class BrightnessController(private val context: Context) {
    val canChangeSystemBrightness: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canChangeSystemBrightness

    fun setWindowBrightness(window: Window, value: Float) {
        window.attributes = window.attributes.apply {
            screenBrightness = value.coerceIn(0f, 1f)
        }
    }

    fun isAutomaticBrightnessEnabled(): Boolean? = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }.getOrNull()

    fun setAutomaticBrightnessEnabled(enabled: Boolean): Boolean {
        if (!canChangeSystemBrightness) return false
        return runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }.getOrDefault(false)
    }
}
