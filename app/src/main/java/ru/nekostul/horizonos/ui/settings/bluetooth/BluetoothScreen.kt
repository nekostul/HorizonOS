package ru.nekostul.horizonos.ui.settings.bluetooth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun BluetoothScreen(selectedIndex: Int, onSelect: (Int) -> Unit, onToggle: () -> Unit) {
    val controller = BluetoothSettingsController(LocalContext.current)
    val rows = buildList {
        add(SettingRow(stringResource(R.string.settings_bluetooth_title), controller.enabled()?.let { stringResource(if (it) R.string.settings_status_on else R.string.settings_status_off) } ?: stringResource(R.string.settings_status_permission_required), stringResource(R.string.settings_bluetooth_description)))
        add(SettingRow(stringResource(R.string.settings_bluetooth_add_device), "", stringResource(R.string.settings_bluetooth_add_description)))
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
        rows.drop(1).forEachIndexed { index, row -> HorizonSettingRow(row, selectedIndex == index + 1, onClick = { onSelect(index + 1) }) }
        if (!controller.available) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_missing))
        else if (!controller.canControl) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_permission))
    }
}
