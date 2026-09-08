package ru.nekostul.horizonos.ui.settings

import android.content.Context
import android.os.Build
import android.provider.Settings

interface SystemRadioController {
    val capability: Boolean
    fun currentState(): Boolean?
    fun setEnabled(enabled: Boolean): Boolean
}

class AirplaneModeController(private val context: Context) : SystemRadioController {
    override val capability: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlAirplaneMode

    override fun currentState(): Boolean? = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    }.getOrNull()

    override fun setEnabled(enabled: Boolean): Boolean {
        if (!capability) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            val value = if (enabled) 1 else 0
            return PrivilegedSystemAccess.run(
                "settings put global airplane_mode_on $value && " +
                    "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled"
            )?.succeeded == true
        }
        return runCatching {
            Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (enabled) 1 else 0)
            context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", enabled))
            true
        }.getOrDefault(false)
    }
}

class WifiController(private val context: Context) : SystemRadioController {
    override val capability: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlWifi

    override fun currentState(): Boolean? = runCatching {
        val manager = context.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        @Suppress("DEPRECATION")
        manager?.isWifiEnabled
    }.getOrNull()

    override fun setEnabled(enabled: Boolean): Boolean {
        if (!capability) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("svc wifi ${if (enabled) "enable" else "disable"}")?.succeeded == true
        }
        return runCatching {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager).isWifiEnabled = enabled
            true
        }.getOrDefault(false)
    }
}

class BluetoothController(private val context: Context) : SystemRadioController {
    override val capability: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlBluetooth

    override fun currentState(): Boolean? = runCatching {
        val adapter = context.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter ?: return@runCatching null
        if (!BluetoothPermission.hasConnect(context)) return@runCatching null
        adapter.isEnabled
    }.getOrNull()

    override fun setEnabled(enabled: Boolean): Boolean {
        if (!capability || !BluetoothPermission.hasConnect(context)) return false
        if (SystemCapabilitiesDetector.detect(context).hasRootAccess) {
            return PrivilegedSystemAccess.run("cmd bluetooth_manager ${if (enabled) "enable" else "disable"}")?.succeeded == true
        }
        return runCatching {
            val adapter = context.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter ?: return@runCatching false
            @Suppress("DEPRECATION")
            if (enabled) adapter.enable() else adapter.disable()
        }.getOrDefault(false)
    }
}

object BluetoothPermission {
    fun hasConnect(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasScan(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}
