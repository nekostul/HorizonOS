package ru.nekostul.horizonos.ui.settings.wifi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R

@Composable
fun WifiScreen(selectedIndex: Int, onSelect: (Int) -> Unit, onToggle: () -> Unit) {
    val controller = WifiSettingsController(LocalContext.current)
    val state = controller.enabled()?.let { stringResource(if (it) R.string.settings_status_on else R.string.settings_status_off) } ?: stringResource(R.string.settings_status_unavailable)
    val ssid = controller.connectedSsid()?.takeUnless { it == "<unknown ssid>" } ?: stringResource(R.string.settings_wifi_no_connection)
    val networks = controller.availableNetworkNames()
    var selectedSsid by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var connectionState by remember { mutableStateOf<WifiConnectionState?>(null) }
    LaunchedEffect(selectedIndex, networks) {
        if (selectedIndex >= 3) selectedSsid = networks.getOrNull(selectedIndex - 3)
    }
    val rows = buildList {
        add(SettingRow(stringResource(R.string.settings_wifi_title), state, stringResource(R.string.settings_wifi_description)))
        add(SettingRow(stringResource(R.string.settings_connected_network), ssid, stringResource(R.string.settings_connected_network_description)))
        add(SettingRow(stringResource(R.string.settings_available_networks), if (networks.isEmpty()) stringResource(R.string.settings_status_no_data) else stringResource(R.string.settings_network_count, networks.size)))
        networks.forEach { add(SettingRow(it, stringResource(R.string.settings_wifi_network_label))) }
    }
    Column {
        Text(stringResource(R.string.settings_wifi_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(stringResource(R.string.settings_wifi_title), controller.enabled() == true, selectedIndex == 0, enabled = controller.canControl, onClick = { onSelect(0); onToggle() })
        rows.drop(1).forEachIndexed { index, row -> HorizonSettingRow(row, selectedIndex == index + 1, onClick = { onSelect(index + 1) }) }
        selectedSsid?.let { ssidValue ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.settings_wifi_connect_title, ssidValue), color = SettingsWhite, fontSize = 20.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.settings_wifi_password)) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(
                onClick = {
                    controller.connect(ssidValue, password) { connectionState = it }
                },
                modifier = Modifier.padding(top = 8.dp)
            ) { Text(stringResource(R.string.settings_wifi_connect)) }
            connectionState?.let { stateValue ->
                val stateText = stringResource(
                    when (stateValue) {
                        WifiConnectionState.CONNECTED -> R.string.settings_wifi_connected
                        WifiConnectionState.FAILED -> R.string.settings_wifi_connection_failed
                        WifiConnectionState.LOST -> R.string.settings_wifi_connection_lost
                        WifiConnectionState.UNAVAILABLE -> R.string.settings_wifi_connection_unavailable
                        WifiConnectionState.ANDROID_10_REQUIRED -> R.string.settings_wifi_android10_required
                    }
                )
                Text(stateText, color = SettingsGray, fontSize = 14.sp)
            }
        }
        if (!controller.canControl) SettingsCapabilitiesNote(stringResource(R.string.settings_wifi_capability))
    }
}
