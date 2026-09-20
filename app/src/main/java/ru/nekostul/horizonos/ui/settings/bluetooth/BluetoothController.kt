package ru.nekostul.horizonos.ui.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.os.Build
import ru.nekostul.horizonos.ui.settings.BluetoothPermission
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector
import ru.nekostul.horizonos.ui.settings.SystemRadioState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class BluetoothDeviceCategory { AUDIO, TV, COMPUTER, PHONE, PERIPHERAL, OTHER }

class BluetoothSettingsController(private val context: Context) {
    init {
        loadPersistedNames()
    }

    private fun loadPersistedNames() {
        if (nameCacheLoaded) return
        synchronized(nameCache) {
            if (nameCacheLoaded) return
            val prefs = context.applicationContext
                .getSharedPreferences(NAME_CACHE_PREFS, Context.MODE_PRIVATE)
            prefs.all.forEach { (address, name) ->
                (name as? String)?.takeIf(String::isNotBlank)?.let {
                    nameCache[address] = it
                }
            }
            nameCacheLoaded = true
            if (nameCache.isNotEmpty()) nameCacheRevision.incrementAndGet()
        }
    }

    private val adapter: BluetoothAdapter?
        get() = context.applicationContext
            .getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter

    val available: Boolean get() = adapter != null
    val canControl: Boolean
        get() = SystemCapabilitiesDetector.detect(context).canControlBluetooth && BluetoothPermission.hasConnect(context)
    val canScan: Boolean
        get() = available &&
            BluetoothPermission.hasScan(context) &&
            BluetoothPermission.hasConnect(context)

    fun enabled(): Boolean? = SystemRadioState.bluetoothEnabled(context)

    fun bondedDevices(): List<BluetoothDevice> = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching emptyList()
        @Suppress("DEPRECATION")
        adapter?.bondedDevices.orEmpty().toList()
    }.getOrDefault(emptyList())

    fun bondedDeviceNames(): List<String> = bondedDevices()
        .mapNotNull { nameOf(it)?.takeIf(String::isNotBlank) }
        .sorted()

    fun nameOf(device: BluetoothDevice, advertisedName: String? = null): String? {
        nameCache[device.address]?.let { return it }

        advertisedName?.takeIf(String::isNotBlank)?.let { storeName(device.address, it); return it }

        val fromApi = runCatching {
            if (!BluetoothPermission.hasConnect(context)) null
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias?.takeIf(String::isNotBlank)
            else null
        }.getOrNull()

        if (fromApi != null) {
            storeName(device.address, fromApi)
            return fromApi
        }

        val name = runCatching {
            if (!BluetoothPermission.hasConnect(context)) null
            else device.name?.takeIf { it.isNotBlank() }
                ?: fetchNameViaReflection(device)
        }.getOrNull()

        if (name != null) {
            storeName(device.address, name)
        }
        return name
    }

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
        if (!canScan) return@runCatching false
        val current = adapter ?: return@runCatching false
        if (!current.isEnabled) return@runCatching false
        if (current.isDiscovering) {
            current.cancelDiscovery()
            Thread.sleep(150)
        }
        current.startDiscovery()
    }.getOrDefault(false)

    fun cancelDiscovery(): Boolean = runCatching { adapter?.cancelDiscovery() == true }.getOrDefault(false)

    fun isDiscovering(): Boolean = runCatching { adapter?.isDiscovering == true }.getOrDefault(false)

    fun startLeScan(callback: ScanCallback): Boolean = runCatching {
        if (!canScan) return@runCatching false
        adapter?.bluetoothLeScanner?.startScan(callback)
        adapter?.bluetoothLeScanner != null
    }.getOrDefault(false)

    fun stopLeScan(callback: ScanCallback) {
        runCatching { adapter?.bluetoothLeScanner?.stopScan(callback) }
    }

    fun createBond(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        device.createBond()
    }.getOrDefault(false)

    fun removeBond(device: BluetoothDevice): Boolean = runCatching {
        if (!BluetoothPermission.hasConnect(context)) return@runCatching false
        @Suppress("DEPRECATION")
        device.javaClass.getMethod("removeBond").invoke(device) as? Boolean ?: false
    }.getOrDefault(false)

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

    fun cacheName(address: String, name: String) {
        name.takeIf(String::isNotBlank)?.let { storeName(address, it) }
    }

    fun getNameCacheRevision(): Int = nameCacheRevision.get()

    suspend fun resolveNames(): Int {
        if (!canScan) return 0
        val targetAddresses = bondedDevices()
            .filter { nameOf(it) == null }
            .map { it.address }
            .toSet()
        if (targetAddresses.isEmpty()) return 0
        val before = nameCache.size
        startDiscovery()
        try {
            kotlinx.coroutines.delay(4_000L)
        } finally {
            cancelDiscovery()
        }
        return nameCache.size - before
    }

    private fun fetchNameViaReflection(device: BluetoothDevice): String? {
        return runCatching {
            val field = BluetoothDevice::class.java.getDeclaredField("mName")
            field.isAccessible = true
            (field.get(device) as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun storeName(address: String, name: String) {
        nameCache[address] = name
        nameCacheRevision.incrementAndGet()
        context.applicationContext
            .getSharedPreferences(NAME_CACHE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(address, name)
            .apply()
    }

    private companion object {
        private const val NAME_CACHE_PREFS = "bluetooth_names"

        val nameCache = ConcurrentHashMap<String, String>()
        val nameCacheRevision = AtomicInteger(0)
        @Volatile
        private var nameCacheLoaded = false

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