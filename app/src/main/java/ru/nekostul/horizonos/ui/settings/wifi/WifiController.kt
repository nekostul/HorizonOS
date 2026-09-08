package ru.nekostul.horizonos.ui.settings.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import android.net.wifi.WifiNetworkSpecifier
import ru.nekostul.horizonos.ui.settings.SystemCapabilitiesDetector

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
    fun enabled(): Boolean? = runCatching {
        @Suppress("DEPRECATION") manager?.isWifiEnabled
    }.getOrNull()
    fun connectedSsid(): String? = runCatching {
        @Suppress("DEPRECATION") manager?.connectionInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")
    }.getOrNull()

    @Suppress("DEPRECATION")
    fun scan(): Boolean = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return@runCatching false
        manager?.startScan() == true
    }.getOrDefault(false)

    @Suppress("DEPRECATION")
    fun availableNetworks(): List<WifiNetworkInfo> = runCatching {
        manager?.scanResults.orEmpty().mapNotNull { result ->
            val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.wifiSsid?.toString()
            } else {
                @Suppress("DEPRECATION")
                result.SSID
            }
            name?.takeIf(String::isNotBlank)?.let {
                WifiNetworkInfo(
                    ssid = it,
                    signalLevel = WifiManager.calculateSignalLevel(result.level, 4),
                    security = securityType(result.capabilities),
                    capabilities = result.capabilities
                )
            }
        }.distinctBy { it.ssid }.sortedByDescending { it.signalLevel }
    }.getOrDefault(emptyList())

    fun availableNetworkNames(): List<String> = availableNetworks().map { it.ssid }

    private fun securityType(capabilities: String): WifiSecurity = when {
        capabilities.contains("WPA3", ignoreCase = true) -> WifiSecurity.WPA3
        capabilities.contains("WPA", ignoreCase = true) -> WifiSecurity.WPA
        capabilities.contains("WEP", ignoreCase = true) -> WifiSecurity.WEP
        else -> WifiSecurity.OPEN
    }

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl) return false
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
}
