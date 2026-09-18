package ru.nekostul.horizonos.ui.settings.controllers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun ControllersScreen(
    selectedIndex: Int,
    gamesirOpenRequest: Int,
    onActivationConsumed: () -> Unit
) {
    val context = LocalContext.current
    val controllers = remember { ControllerManager.connectedControllers() }
    val gamesirConnected = controllers.any { it.name?.contains("gamesir", ignoreCase = true) == true }
    var gamesirMissing by remember { mutableStateOf(false) }
    var gamesirChoice by remember { mutableIntStateOf(0) }

    fun openGamesir(source: LauncherInputSource) {
        if (!GameSirLauncher.open(context)) {
            gamesirChoice = 0
            gamesirMissing = true
            LauncherAudioManager.playHint(source)
        }
    }

    LaunchedEffect(gamesirOpenRequest) {
        if (gamesirOpenRequest > 0) {
            openGamesir(LauncherInputSource.GAMEPAD)
            onActivationConsumed()
        }
    }

    Column {
        Text(stringResource(R.string.settings_controllers_title_runtime), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_connected_controllers),
                value = if (controllers.isEmpty()) stringResource(R.string.settings_controller_none) else controllers.size.toString()
            ),
            selectedIndex == 0,
            onClick = {}
        )
        controllers.forEachIndexed { index, controller ->
            val connection = stringResource(
                when (controller.connection) {
                    ControllerConnection.GAMEPAD -> R.string.settings_controller_gamepad
                    ControllerConnection.JOYSTICK_HID -> R.string.settings_controller_joystick_hid
                }
            )
            HorizonSettingRow(
                SettingRow(
                    title = controller.name ?: stringResource(R.string.settings_controller_unknown),
                    value = connection,
                    description = stringResource(R.string.settings_controller_id, controller.deviceId)
                ),
                selectedIndex == index + 1,
                onClick = {}
            )
        }
        if (gamesirConnected) {
            HorizonSettingRow(
                SettingRow(title = stringResource(R.string.settings_controllers_gamesir_button)),
                selected = selectedIndex == controllers.size + 1,
                onClick = { openGamesir(LauncherInputSource.TOUCH) }
            )
            SettingsCapabilitiesNote(stringResource(R.string.settings_controllers_gamesir_note))
        }
    }

    if (gamesirMissing) {
        val gamesirInputMode = remember { mutableStateOf(SettingsInputMode.GAMEPAD) }
        CompositionLocalProvider(LocalSettingsInputMode provides gamesirInputMode) {
            HorizonOverlay(
                title = stringResource(R.string.home_gamesir_missing_title),
                onDismiss = { gamesirMissing = false },
                onDirectionalKey = { key ->
                    when (key) {
                        Key.DirectionDown, Key.DirectionRight -> {
                            gamesirChoice = (gamesirChoice + 1).coerceAtMost(1)
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            gamesirChoice = (gamesirChoice - 1).coerceAtLeast(0)
                            true
                        }
                        else -> false
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.home_gamesir_missing_message),
                    color = SettingsWhite,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
                HorizonOverlayChoice(
                    title = stringResource(R.string.home_gamesir_download),
                    selected = gamesirChoice == 0,
                    onClick = {
                        gamesirMissing = false
                        GameSirLauncher.openStore(context)
                    }
                )
                HorizonOverlayChoice(
                    title = stringResource(R.string.home_gamesir_close),
                    selected = gamesirChoice == 1,
                    onClick = { gamesirMissing = false }
                )
            }
        }
    }
}
