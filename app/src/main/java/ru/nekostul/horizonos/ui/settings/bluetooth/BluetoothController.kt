package ru.nekostul.horizonos.ui.settings.bluetooth

import android.content.Context
import ru.nekostul.horizonos.ui.settings.BluetoothPermission
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

class BluetoothSettingsController(private val context: Context) {
    private val adapter: android.bluetooth.BluetoothAdapter?
        get() = context.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter

    val available: Boolean get() = adapter != null
    val canControl: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlBluetooth && BluetoothPermission.hasConnect(context)

    fun enabled(): Boolean? = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching null
        adapter?.isEnabled
    }.getOrNull()

    fun bondedDeviceNames(): List<String> = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching emptyList()
        @Suppress("DEPRECATION")
        adapter?.bondedDevices.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }.sorted()
    }.getOrDefault(emptyList())

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl || !BluetoothPermission.hasConnect(context)) return false
        return runCatching {
            @Suppress("DEPRECATION")
            if (enabled) adapter?.enable() == true else adapter?.disable() == true
        }.getOrDefault(false)
    }
}
