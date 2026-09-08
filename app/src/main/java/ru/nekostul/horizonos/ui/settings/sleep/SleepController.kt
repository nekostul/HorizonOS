package ru.nekostul.horizonos.ui.settings.sleep

import android.content.Context
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess

class SleepController(private val context: Context) {
    val canEnterSleep: Boolean get() = SystemCapabilitiesDetector.detect(context).canEnterSleep

    fun enterSleep(): Boolean = if (canEnterSleep) {
        PrivilegedSystemAccess.run("input keyevent 26")?.succeeded == true
    } else false
}
