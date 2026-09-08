package ru.nekostul.horizonos.ui.settings.sleep

import android.content.Context
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class SleepController(private val context: Context) {
    val canEnterSleep: Boolean get() = SystemCapabilitiesDetector.detect(context).canEnterSleep

    fun enterSleep(): Boolean = false
}
