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
    CONNECTED,
    FAILED,
    LOST,
    UNAVAILABLE,
    ANDROID_10_REQUIRED
}

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

    fun availableNetworkNames(): List<String> = runCatching {
        manager?.scanResults.orEmpty().mapNotNull { result ->
            val name = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.wifiSsid?.toString()
            } else {
                @Suppress("DEPRECATION")
                result.SSID
            }
            name?.takeIf(String::isNotBlank)
        }.distinct().sorted()
    }.getOrDefault(emptyList())

    fun setEnabled(enabled: Boolean): Boolean {
        if (!canControl) return false
        return runCatching {
            @Suppress("DEPRECATION")
            manager?.isWifiEnabled = enabled
            true
        }.getOrDefault(false)
    }

    fun connect(ssid: String, password: String, onState: (WifiConnectionState) -> Unit): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            onState(WifiConnectionState.ANDROID_10_REQUIRED)
            return false
        }
        return runCatching {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build()
            val manager = context.getSystemService(ConnectivityManager::class.java)
            manager?.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onState(WifiConnectionState.CONNECTED)
                }

                override fun onUnavailable() {
                    onState(WifiConnectionState.FAILED)
                }

                override fun onLost(network: Network) {
                    onState(WifiConnectionState.LOST)
                }
            })
            true
        }.getOrElse {
            onState(WifiConnectionState.UNAVAILABLE)
            false
        }
    }
}
