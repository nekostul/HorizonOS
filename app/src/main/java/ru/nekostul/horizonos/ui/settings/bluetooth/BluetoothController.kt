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
    val canScan: Boolean
        get() = available && BluetoothPermission.hasScan(context)

    fun enabled(): Boolean? = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching null
        adapter?.isEnabled
    }.getOrNull()

    fun bondedDeviceNames(): List<String> = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching emptyList()
        @Suppress("DEPRECATION")
        adapter?.bondedDevices.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }.sorted()
    }.getOrDefault(emptyList())

    fun startDiscovery(): Boolean = runCatching {
        if (!canScan || !BluetoothPermission.hasConnect(context)) return@runCatching false
        adapter?.startDiscovery() == true
    }.getOrDefault(false)

    fun cancelDiscovery(): Boolean = runCatching { adapter?.cancelDiscovery() == true }.getOrDefault(false)

    fun createBond(device: android.bluetooth.BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        device.createBond()
    }.getOrDefault(false)

    fun removeBond(device: android.bluetooth.BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        @Suppress("DEPRECATION")
        device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
    }.getOrDefault(false)

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl || !BluetoothPermission.hasConnect(context)) return false
        return runCatching {
            @Suppress("DEPRECATION")
            if (enabled) adapter?.enable() == true else adapter?.disable() == true
        }.getOrDefault(false)
    }
}
