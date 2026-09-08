package ru.nekostul.horizonos.ui.settings.controllers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.controllers.ControllerConnection

@Composable
fun ControllersScreen(
    settings: LauncherSettings,
    selectedIndex: Int,
    onVibrationToggle: () -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onDeadZoneChange: (Float) -> Unit
) {
    val controllers = remember { ControllerManager.connectedControllers() }
    Column {
        Text(stringResource(R.string.settings_controllers_title_runtime), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(ru.nekostul.horizonos.ui.settings.SettingRow(stringResource(R.string.settings_connected_controllers), if (controllers.isEmpty()) stringResource(R.string.settings_controller_none) else controllers.size.toString()), selectedIndex == 0, onClick = {})
        controllers.forEachIndexed { index, controller ->
            val connection = stringResource(
                when (controller.connection) {
                    ControllerConnection.GAMEPAD -> R.string.settings_controller_gamepad
                    ControllerConnection.JOYSTICK_HID -> R.string.settings_controller_joystick_hid
                }
            )
            HorizonSettingRow(ru.nekostul.horizonos.ui.settings.SettingRow(controller.name ?: stringResource(R.string.settings_controller_unknown), connection, stringResource(R.string.settings_controller_id, controller.deviceId)), selectedIndex == index + 1, onClick = {})
        }
        SettingsToggleRow(stringResource(R.string.settings_vibration), settings.vibrationEnabled, selectedIndex == controllers.size + 1, onClick = onVibrationToggle)
        SettingsSliderRow(stringResource(R.string.settings_sensitivity), ((settings.controllerSensitivity - 0.5f) / 1.5f).coerceIn(0f, 1f), selectedIndex == controllers.size + 2, valueLabel = "${(settings.controllerSensitivity * 100).toInt()}%") { onSensitivityChange(0.5f + it * 1.5f) }
        SettingsSliderRow(stringResource(R.string.settings_dead_zone), settings.controllerDeadZone / 0.5f, selectedIndex == controllers.size + 3, valueLabel = "${(settings.controllerDeadZone * 100).toInt()}%") { onDeadZoneChange(it * 0.5f) }
    }
}
