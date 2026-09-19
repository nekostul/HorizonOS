package ru.nekostul.horizonos.ui.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import ru.nekostul.horizonos.ui.settings.BluetoothPermission
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector
import ru.nekostul.horizonos.ui.settings.SystemRadioState

enum class BluetoothDeviceCategory { AUDIO, TV, COMPUTER, PHONE, PERIPHERAL, OTHER }

class BluetoothSettingsController(private val context: Context) {
    private val adapter: BluetoothAdapter?
        get() = context.applicationContext
            .getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter

    val available: Boolean get() = adapter != null
    val canControl: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlBluetooth && BluetoothPermission.hasConnect(context)
    val canScan: Boolean
        get() = available && BluetoothPermission.hasScan(context)

    fun enabled(): Boolean? = SystemRadioState.bluetoothEnabled(context)

    fun bondedDevices(): List<BluetoothDevice> = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching emptyList()
        @Suppress("DEPRECATION")
        adapter?.bondedDevices.orEmpty().toList()
    }.getOrDefault(emptyList())

    fun bondedDeviceNames(): List<String> = bondedDevices()
        .mapNotNull { nameOf(it)?.takeIf(String::isNotBlank) }
        .sorted()

    fun nameOf(device: BluetoothDevice): String? = runCatching {
        if (!BluetoothPermission.hasConnect(context)) null else device.name
    }.getOrNull()

    fun isBonded(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        @Suppress("DEPRECATION")
        device.bondState == BluetoothDevice.BOND_BONDED
    }.getOrDefault(false)

    fun isConnected(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        val manager = context.applicationContext
            .getSystemService(android.bluetooth.BluetoothManager::class.java) ?: return@runCatching false
        manager.getConnectionState(device, BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED ||
            manager.getConnectionState(device, BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
    }.getOrDefault(false)

    fun categoryOf(device: BluetoothDevice): BluetoothDeviceCategory {
        val fromClass = runCatching {
            if (!BluetoothPermission.hasConnect(context)) return@runCatching null
            val bluetoothClass = device.bluetoothClass ?: return@runCatching null
            when (bluetoothClass.majorDeviceClass) {
                BluetoothClass.Device.Major.AUDIO_VIDEO -> when (bluetoothClass.deviceClass) {
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING,
                    BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> BluetoothDeviceCategory.TV

                    BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED,
                    BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA -> BluetoothDeviceCategory.OTHER

                    else -> BluetoothDeviceCategory.AUDIO
                }

                BluetoothClass.Device.Major.COMPUTER -> BluetoothDeviceCategory.COMPUTER
                BluetoothClass.Device.Major.PHONE -> BluetoothDeviceCategory.PHONE
                BluetoothClass.Device.Major.PERIPHERAL -> BluetoothDeviceCategory.PERIPHERAL
                else -> BluetoothDeviceCategory.OTHER
            }
        }.getOrNull()
        if (fromClass != null && fromClass != BluetoothDeviceCategory.OTHER) return fromClass
        return categoryFromName(nameOf(device)) ?: fromClass ?: BluetoothDeviceCategory.OTHER
    }

    private fun categoryFromName(name: String?): BluetoothDeviceCategory? {
        val normalized = name?.lowercase() ?: return null
        if (normalized.isBlank()) return null
        return when {
            AUDIO_KEYWORDS.any { normalized.contains(it) } -> BluetoothDeviceCategory.AUDIO
            TV_KEYWORDS.any { normalized.contains(it) } -> BluetoothDeviceCategory.TV
            COMPUTER_KEYWORDS.any { normalized.contains(it) } -> BluetoothDeviceCategory.COMPUTER
            PHONE_KEYWORDS.any { normalized.contains(it) } -> BluetoothDeviceCategory.PHONE
            else -> null
        }
    }

    fun startDiscovery(): Boolean = runCatching {
        if (!canScan || !BluetoothPermission.hasConnect(context)) return@runCatching false
        val current = adapter ?: return@runCatching false
        if (current.isDiscovering) current.cancelDiscovery()
        current.startDiscovery()
    }.getOrDefault(false)

    fun cancelDiscovery(): Boolean = runCatching { adapter?.cancelDiscovery() == true }.getOrDefault(false)

    fun isDiscovering(): Boolean = runCatching { adapter?.isDiscovering == true }.getOrDefault(false)

    fun createBond(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        device.createBond()
    }.getOrDefault(false)

    fun removeBond(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        @Suppress("DEPRECATION")
        device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
    }.getOrDefault(false)

    /**
     * Pairs the device if needed and connects the A2DP/headset profiles so
     * audio is routed through e.g. Bluetooth headphones.
     */
    fun connect(device: BluetoothDevice): Boolean {
        if (!BluetoothPermission.hasConnect(context)) return false
        if (!isBonded(device)) return createBond(device)
        val a2dp = connectProfile(BluetoothProfile.A2DP, device)
        val headset = connectProfile(BluetoothProfile.HEADSET, device)
        return a2dp || headset
    }

    private fun connectProfile(profile: Int, device: BluetoothDevice): Boolean {
        val adapter = adapter ?: return false
        return runCatching {
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile) {
                    runCatching {
                        proxy.javaClass.getMethod("connect", BluetoothDevice::class.java).invoke(proxy, device)
                    }
                    runCatching { adapter.closeProfileProxy(profileId, proxy) }
                }

                override fun onServiceDisconnected(profileId: Int) {}
            }, profile)
        }.getOrDefault(false)
    }

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl || !BluetoothPermission.hasConnect(context)) return false
        if (PrivilegedSystemAccess.hasRootAccess() &&
            PrivilegedSystemAccess.run("cmd bluetooth_manager ${if (enabled) "enable" else "disable"}")?.succeeded == true
        ) {
            return true
        }
        return runCatching {
            @Suppress("DEPRECATION")
            if (enabled) adapter?.enable() == true else adapter?.disable() == true
        }.getOrDefault(false)
    }

    private companion object {
        val AUDIO_KEYWORDS = listOf(
            "headphone", "headset", "earphone", "earbud", "buds", "speaker", "soundbar",
            "airpods", "beats", "soundcore", "jbl", "bose", "sennheiser", "wh-", "wf-",
            "audio", "a2dp", "hands-free", "airpod"
        )
        val TV_KEYWORDS = listOf("tv", "television", "bravia", "webos", "smart tv", "chromecast")
        val COMPUTER_KEYWORDS = listOf(
            "pc", "laptop", "desktop", "computer", "macbook", "thinkpad", "surface", "notebook", "windows"
        )
        val PHONE_KEYWORDS = listOf(
            "phone", "iphone", "pixel", "smartphone", "galaxy s", "galaxy note", "redmi", "oneplus"
        )
    }
}