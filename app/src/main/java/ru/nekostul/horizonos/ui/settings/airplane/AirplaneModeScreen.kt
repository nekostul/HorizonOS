package ru.nekostul.horizonos.ui.settings.airplane

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow

@Composable
fun AirplaneModeScreen(
    context: Context,
    settings: LauncherSettings,
    selectedIndex: Int,
    onToggleAirplane: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleBluetooth: () -> Unit
) {
    val controller = AirplaneModeStateController(context)
    val systemState = controller.systemState()
    Column {
        HorizonSettingRow(
            row = SettingRow(
                title = stringResource(R.string.settings_airplane_title),
                value = systemState?.let { if (it) stringResource(R.string.settings_status_on) else stringResource(R.string.settings_status_off) }
                    ?: stringResource(R.string.settings_status_unavailable),
                description = stringResource(R.string.settings_airplane_description),
                enabled = controller.canControlSystemMode
            ),
            selected = selectedIndex == 0,
            onClick = onToggleAirplane
        )
        if (settings.airplaneMode) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_category_wifi),
                checked = settings.airplaneWifiAllowed,
                selected = selectedIndex == 1,
                description = stringResource(R.string.settings_airplane_wifi_description),
                onClick = onToggleWifi
            )
            SettingsToggleRow(
                title = stringResource(R.string.settings_category_bluetooth),
                checked = settings.airplaneBluetoothAllowed,
                selected = selectedIndex == 2,
                description = stringResource(R.string.settings_airplane_bluetooth_description),
                onClick = onToggleBluetooth
            )
        }
        if (!settings.rootAccessGranted && !controller.canControlSystemMode) {
            SettingsCapabilitiesNote(stringResource(R.string.capability_airplane))
        }
    }
}
