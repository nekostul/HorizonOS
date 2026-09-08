package ru.nekostul.horizonos.ui.settings.system

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class DateTimeController(private val context: Context) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    val canChange: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canChangeDateTime

    fun automaticTimeEnabled(): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 1) == 1
    }.getOrNull()

    fun setAutomaticTimeEnabled(enabled: Boolean): Boolean {
        if (!canChange) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("settings put global auto_time ${if (enabled) 1 else 0}")?.succeeded == true
        }
        return runCatching {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AUTO_TIME, if (enabled) 1 else 0)
        }.getOrDefault(false)
    }

    fun setDateTime(timeInMillis: Long): Boolean {
        if (!canChange) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("date -s @${timeInMillis / 1000L}")?.succeeded == true
        }
        return runCatching {
            alarmManager?.setTime(timeInMillis)
            alarmManager != null
        }.getOrDefault(false)
    }

    fun setTimeZone(timeZoneId: String): Boolean {
        if (!canChange || timeZoneId.isBlank()) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("setprop persist.sys.timezone '${timeZoneId.replace("'", "")}'")?.succeeded == true
        }
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setTimeZone(timeZoneId)
                alarmManager != null
            } else false
        }.getOrDefault(false)
    }
}
