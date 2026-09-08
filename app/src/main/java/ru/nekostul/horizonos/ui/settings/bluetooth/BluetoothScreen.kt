package ru.nekostul.horizonos.ui.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice

@Composable
fun BluetoothScreen(selectedIndex: Int, onSelect: (Int) -> Unit, onToggle: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { BluetoothSettingsController(context) }
    val discovered = remember { mutableStateListOf<BluetoothDevice>() }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var scanning by remember { mutableStateOf(false) }
    DisposableEffect(controller) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothDevice.ACTION_FOUND) {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null && discovered.none { it.address == device.address }) discovered += device
                }
                if (intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) scanning = false
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
            }
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val rows = buildList {
        add(SettingRow(stringResource(R.string.settings_bluetooth_title), controller.enabled()?.let { stringResource(if (it) R.string.settings_status_on else R.string.settings_status_off) } ?: stringResource(R.string.settings_status_permission_required), stringResource(R.string.settings_bluetooth_description)))
        add(SettingRow(stringResource(R.string.settings_bluetooth_add_device), if (scanning) stringResource(R.string.settings_bluetooth_scanning) else "", stringResource(R.string.settings_bluetooth_add_description)))
        controller.bondedDeviceNames().forEach { add(SettingRow(it, stringResource(R.string.settings_status_paired))) }
    }
    Column {
        Text(stringResource(R.string.settings_bluetooth_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_bluetooth_title),
            checked = controller.enabled() == true,
            selected = selectedIndex == 0,
            enabled = controller.canControl,
            onClick = { onSelect(0); onToggle() }
        )
        rows.drop(1).forEachIndexed { index, row -> HorizonSettingRow(row, selectedIndex == index + 1, onClick = {
            onSelect(index + 1)
            if (index == 0) {
                discovered.clear()
                scanning = controller.startDiscovery()
            }
        }) }
        discovered.forEachIndexed { index, device ->
            val name = runCatching { device.name }.getOrNull()?.takeIf(String::isNotBlank)
                ?: stringResource(R.string.settings_bluetooth_unknown_device)
            HorizonSettingRow(
                SettingRow(name, stringResource(R.string.settings_bluetooth_discovered)),
                selectedIndex == index + rows.size,
                onClick = { selectedDevice = device }
            )
        }
        if (!controller.available) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_missing))
        else if (!controller.canControl) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_permission))
    }
    selectedDevice?.let { device ->
        val name = runCatching { device.name }.getOrNull()?.takeIf(String::isNotBlank)
            ?: stringResource(R.string.settings_bluetooth_unknown_device)
        HorizonOverlay(stringResource(R.string.settings_bluetooth_device_actions, name), { selectedDevice = null }) {
            HorizonOverlayChoice(
                if (controller.bondedDeviceNames().contains(name)) stringResource(R.string.settings_bluetooth_unpair) else stringResource(R.string.settings_bluetooth_pair),
                selected = true,
                onClick = {
                    if (controller.bondedDeviceNames().contains(name)) controller.removeBond(device) else controller.createBond(device)
                }
            )
            HorizonOverlayChoice(stringResource(R.string.settings_action_cancel), false, { selectedDevice = null })
        }
    }
}
