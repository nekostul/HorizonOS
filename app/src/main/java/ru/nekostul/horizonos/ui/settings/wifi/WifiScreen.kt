package ru.nekostul.horizonos.ui.settings.wifi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonOverlayTextField
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.keyboard.HorizonKeyboardDialog

@Composable
private fun WifiSecurity.label(): String = stringResource(
    when (this) {
        WifiSecurity.OPEN -> R.string.settings_wifi_security_open
        WifiSecurity.WPA3 -> R.string.settings_wifi_security_wpa3
        WifiSecurity.WPA -> R.string.settings_wifi_security_wpa
        WifiSecurity.WEP -> R.string.settings_wifi_security_wep
    }
)

@Composable
fun WifiScreen(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    activationRequest: Int,
    rootAccessGranted: Boolean = false,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { WifiSettingsController(context) }
    val networks = controller.availableNetworks()
    var selectedNetwork by remember { mutableStateOf<WifiNetworkInfo?>(null) }
    var password by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf<WifiConnectionState?>(null) }

    LaunchedEffect(activationRequest) {
        if (activationRequest > 0 && selectedIndex >= 3) {
            selectedNetwork = networks.getOrNull(selectedIndex - 3)
        }
    }

    val enabled = controller.enabled()
    val connected = controller.connectedSsid()?.takeUnless { it == "<unknown ssid>" }
    Column {
        Text(stringResource(R.string.settings_wifi_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            stringResource(R.string.settings_wifi_title),
            enabled == true,
            selectedIndex == 0,
            enabled = controller.canControl,
            onClick = { onSelect(0) }
        )
        HorizonSettingRow(
            SettingRow(
                stringResource(R.string.settings_connected_network),
                connected ?: stringResource(R.string.settings_wifi_no_connection),
                stringResource(R.string.settings_connected_network_description)
            ),
            selectedIndex == 1,
            onClick = { onSelect(1) }
        )
        HorizonSettingRow(
            SettingRow(
                stringResource(R.string.settings_available_networks),
                stringResource(R.string.settings_network_count, networks.size),
                stringResource(R.string.settings_wifi_scan_description)
            ),
            selectedIndex == 2,
            onClick = { onSelect(2) }
        )
        networks.forEachIndexed { index, network ->
            HorizonSettingRow(
                SettingRow(
                    network.ssid,
                    network.security.label(),
                    stringResource(R.string.settings_wifi_signal, network.signalLevel)
                ),
                selectedIndex == index + 3,
                onClick = { onSelect(index + 3); selectedNetwork = network }
            )
        }
        if (!controller.canControl) SettingsCapabilitiesNote(stringResource(R.string.settings_wifi_capability))
    }

    selectedNetwork?.let { network ->
        val needsPassword = network.security != WifiSecurity.OPEN
        var dialogIndex by remember { mutableIntStateOf(if (needsPassword) 0 else 1) }
        var showKeyboard by remember(network.ssid) { mutableStateOf(false) }
        HorizonOverlay(
            title = stringResource(R.string.settings_wifi_connect_title, network.ssid),
            onDismiss = { selectedNetwork = null; password = "" },
            onDirectionalKey = { key ->
                if (!needsPassword) return@HorizonOverlay false
                when (key) {
                    Key.DirectionDown, Key.DirectionRight -> dialogIndex = (dialogIndex + 1).coerceAtMost(1)
                    Key.DirectionUp, Key.DirectionLeft -> dialogIndex = (dialogIndex - 1).coerceAtLeast(0)
                    else -> return@HorizonOverlay false
                }
                true
            }
        ) {
            Text(network.security.label(), color = SettingsGray, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            if (needsPassword) {
                HorizonOverlayTextField(
                    value = password,
                    onValueChange = { password = it },
                    password = true,
                    placeholder = stringResource(R.string.settings_wifi_password),
                    selected = dialogIndex == 0,
                    onEdit = { showKeyboard = true }
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                HorizonOverlayChoice(
                    stringResource(R.string.settings_wifi_connect),
                    selected = dialogIndex == 1,
                    onClick = {
                        connectionState = WifiConnectionState.CONNECTING
                        controller.connect(network.ssid, password, network.security) { connectionState = it }
                    },
                    value = connectionState?.let { state ->
                        stringResource(
                            when (state) {
                                WifiConnectionState.CONNECTING -> R.string.settings_wifi_connecting
                                WifiConnectionState.CONNECTED -> R.string.settings_wifi_connected
                                WifiConnectionState.FAILED -> R.string.settings_wifi_connection_failed
                                WifiConnectionState.LOST -> R.string.settings_wifi_connection_lost
                                WifiConnectionState.UNAVAILABLE -> R.string.settings_wifi_connection_unavailable
                                WifiConnectionState.ANDROID_10_REQUIRED -> R.string.settings_wifi_android10_required
                            }
                        )
                    } ?: ""
                )
            }
        }
        if (showKeyboard) {
            HorizonKeyboardDialog(
                title = stringResource(R.string.settings_wifi_password),
                initialValue = password,
                onConfirm = { password = it; showKeyboard = false },
                onCancel = { showKeyboard = false }
            )
        }
    }
}
