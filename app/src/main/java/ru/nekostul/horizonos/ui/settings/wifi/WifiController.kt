package ru.nekostul.horizonos.ui.settings.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.net.wifi.WifiNetworkSpecifier
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector
import ru.nekostul.horizonos.ui.settings.SystemRadioState

enum class WifiConnectionState {
    CONNECTING,
    CONNECTED,
    FAILED,
    LOST,
    UNAVAILABLE,
    ANDROID_10_REQUIRED
}

enum class WifiSecurity {
    OPEN,
    WPA3,
    WPA,
    WEP
}

data class WifiNetworkInfo(
    val ssid: String,
    val signalLevel: Int,
    val security: WifiSecurity,
    val capabilities: String
)

class WifiSettingsController(private val context: Context) {
    private val manager: WifiManager?
        get() = context.applicationContext.getSystemService(WifiManager::class.java)
    val canControl: Boolean get() = SystemCapabilitiesDetector.detect(context).canControlWifi

    fun enabled(): Boolean? = SystemRadioState.wifiEnabled(context)

    @Suppress("DEPRECATION")
    fun connectedSsid(): String? {
        val fromApi = runCatching {
            manager?.connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")
        }.getOrNull()
        if (!fromApi.isNullOrBlank() && fromApi != UNKNOWN_SSID) return fromApi
        if (!PrivilegedSystemAccess.hasRootAccess()) return null
        return rootConnectedSsid()
    }

    private fun rootConnectedSsid(): String? {
        val status = PrivilegedSystemAccess.run("cmd wifi status")?.output ?: return null
        return STATUS_SSID.find(status)?.groupValues?.getOrNull(1)
            ?.removePrefix("\"")?.removeSuffix("\"")
            ?.takeIf { it.isNotBlank() && it != UNKNOWN_SSID }
    }

    @Suppress("DEPRECATION")
    fun scan(): Boolean {
        if (PrivilegedSystemAccess.hasRootAccess() &&
            PrivilegedSystemAccess.run("cmd wifi start-scan")?.succeeded == true
        ) {
            return true
        }
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return@runCatching false
            manager?.startScan() == true
        }.getOrDefault(false)
    }

    fun availableNetworks(): List<WifiNetworkInfo> {
        val fromApi = apiScanResults()
        return when {
            fromApi.isNotEmpty() -> fromApi
            !PrivilegedSystemAccess.hasRootAccess() -> emptyList()
            else -> rootScanResults()
        }
    }

    private fun apiScanResults(): List<WifiNetworkInfo> = runCatching {
        @Suppress("DEPRECATION")
        manager?.scanResults.orEmpty().mapNotNull { result ->
            val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.wifiSsid?.toString()
            } else {
                @Suppress("DEPRECATION")
                result.SSID
            }
            name?.takeIf { it.isNotBlank() && it != UNKNOWN_SSID }?.let {
                WifiNetworkInfo(
                    ssid = it,
                    signalLevel = WifiManager.calculateSignalLevel(result.level, 4),
                    security = securityType(result.capabilities),
                    capabilities = result.capabilities
                )
            }
        }.distinctBy { it.ssid }.sortedByDescending { it.signalLevel }
    }.getOrDefault(emptyList())

    private fun rootScanResults(): List<WifiNetworkInfo> {
        val listed = PrivilegedSystemAccess.run("cmd wifi list-scan-results")?.output
        if (!listed.isNullOrBlank()) {
            parseListScanResults(listed).takeIf { it.isNotEmpty() }?.let { return it }
        }
        val iw = PrivilegedSystemAccess.run("iw dev wlan0 scan")?.output
        if (!iw.isNullOrBlank()) {
            parseIwScan(iw).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return emptyList()
    }

    private fun parseListScanResults(output: String): List<WifiNetworkInfo> {
        val networks = mutableListOf<WifiNetworkInfo>()
        output.lineSequence().forEach { line ->
            val parts = line.trim().split(Regex("\\s{2,}"))
            if (parts.size < 5) return@forEach
            val bssid = parts[0]
            if (!BSSID.matches(bssid)) return@forEach
            val rssi = parts[2].toIntOrNull() ?: return@forEach
            val ssid = parts[4].trim()
            if (ssid.isBlank() || ssid == UNKNOWN_SSID) return@forEach
            val flags = parts.getOrNull(5).orEmpty()
            networks += WifiNetworkInfo(
                ssid = ssid,
                signalLevel = WifiManager.calculateSignalLevel(rssi, 4),
                security = securityType(flags),
                capabilities = flags
            )
        }
        return networks.distinctBy { it.ssid }.sortedByDescending { it.signalLevel }
    }

    private fun parseIwScan(output: String): List<WifiNetworkInfo> {
        val networks = mutableListOf<WifiNetworkInfo>()
        output.split(Regex("(?m)^BSS ")).drop(1).forEach { block ->
            val ssid = Regex("(?m)^\\s*SSID:\\s*(.+)$").find(block)?.groupValues?.getOrNull(1)
                ?.trim()?.takeIf { it.isNotBlank() && it != UNKNOWN_SSID } ?: return@forEach
            val rssi = Regex("signal:\\s*(-?\\d+)").find(block)?.groupValues?.getOrNull(1)
                ?.toIntOrNull() ?: -100
            networks += WifiNetworkInfo(
                ssid = ssid,
                signalLevel = WifiManager.calculateSignalLevel(rssi, 4),
                security = securityType(block),
                capabilities = block
            )
        }
        return networks.distinctBy { it.ssid }.sortedByDescending { it.signalLevel }
    }

    private fun securityType(capabilities: String): WifiSecurity = when {
        capabilities.contains("WPA3", ignoreCase = true) ||
            capabilities.contains("SAE", ignoreCase = true) -> WifiSecurity.WPA3
        capabilities.contains("WPA", ignoreCase = true) -> WifiSecurity.WPA
        capabilities.contains("WEP", ignoreCase = true) ||
            capabilities.contains("Privacy", ignoreCase = true) -> WifiSecurity.WEP
        else -> WifiSecurity.OPEN
    }

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl) return false
        if (PrivilegedSystemAccess.hasRootAccess() &&
            PrivilegedSystemAccess.run("svc wifi ${if (enabled) "enable" else "disable"}")?.succeeded == true
        ) {
            return true
        }
        return runCatching {
            @Suppress("DEPRECATION")
            manager?.isWifiEnabled = enabled
            true
        }.getOrDefault(false)
    }

    private var activeCallback: ConnectivityManager.NetworkCallback? = null

    fun disconnect(): Boolean = runCatching {
        activeCallback?.let { context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        activeCallback = null
        true
    }.getOrDefault(false)

    fun connect(ssid: String, password: String, security: WifiSecurity, onState: (WifiConnectionState) -> Unit): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            onState(WifiConnectionState.ANDROID_10_REQUIRED)
            return false
        }
        return runCatching {
            onState(WifiConnectionState.CONNECTING)
            val builder = WifiNetworkSpecifier.Builder().setSsid(ssid)
            if (security != WifiSecurity.OPEN) {
                if (password.isBlank()) {
                    onState(WifiConnectionState.FAILED)
                    return@runCatching false
                }
                builder.setWpa2Passphrase(password)
            }
            val specifier = builder.build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onState(WifiConnectionState.CONNECTED)
                }

                override fun onUnavailable() {
                    onState(WifiConnectionState.FAILED)
                }

                override fun onLost(network: Network) {
                    onState(WifiConnectionState.LOST)
                }
            }
            activeCallback = callback
            manager?.requestNetwork(request, callback)
            manager != null
        }.getOrElse {
            onState(WifiConnectionState.UNAVAILABLE)
            false
        }
    }

    private companion object {
        const val UNKNOWN_SSID = "<unknown ssid>"
        val STATUS_SSID = Regex("SSID:\\s*(\"[^\"]*\"|[^,\\n]+)")
        val BSSID = Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")
    }
}