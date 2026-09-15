package ru.nekostul.horizonos.ui.home

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess

object PowerController {

    fun turnOffScreen(context: Context): Boolean {
        val rooted = PrivilegedSystemAccess.run("input keyevent 26")
        if (rooted?.succeeded == true) return true

        return runCatching {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val method = PowerManager::class.java
                .getMethod("goToSleep", Long::class.javaPrimitiveType)
            method.invoke(powerManager, SystemClock.uptimeMillis())
        }.isSuccess
    }
}
