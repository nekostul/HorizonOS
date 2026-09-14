package ru.nekostul.horizonos.ui.home

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess

/**
 * Turns the device screen off for the launcher's power button.
 *
 * The device has root, so the reliable path is sending the power key through
 * `su -c "input keyevent 26"`. This is exactly the hardware power button, so the
 * screen locks normally and biometric unlock keeps working (unlike a
 * device-admin lock). A privileged/platform build may still use the hidden
 * PowerManager.goToSleep as a fallback.
 */
object PowerController {

    fun turnOffScreen(context: Context): Boolean {
        // Root: press the power button.
        val rooted = PrivilegedSystemAccess.run("input keyevent 26")
        if (rooted?.succeeded == true) return true

        // Fallback for a privileged/platform-signed build.
        return runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val method = PowerManager::class.java
                .getMethod("goToSleep", Long::class.javaPrimitiveType)
            method.invoke(powerManager, SystemClock.uptimeMillis())
        }.isSuccess
    }
}
