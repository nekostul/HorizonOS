package ru.nekostul.horizonos.ui.settings.sleep

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsAccentTeal
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.minutesLabel

private val standbyTimeouts = listOf(1, 3, 5, 10, 30)

@Composable
fun SleepScreen(
    context: Context,
    settings: LauncherSettings,
    selectedIndex: Int,
    activationRequest: Int,
    onTimeoutSelected: (Int) -> Unit,
    onMediaToggle: () -> Unit,
    onActivationConsumed: () -> Unit
) {
    val controller = SleepController(context)
    var showTimeoutPicker by remember { mutableStateOf(false) }
    var timeoutIndex by remember { mutableIntStateOf(2) }

    fun openTimeoutPicker() {
        timeoutIndex = standbyTimeouts.indexOf(settings.sleepTimeoutMinutes).coerceAtLeast(0)
        showTimeoutPicker = true
    }

    LaunchedEffect(activationRequest) {
        if (activationRequest > 0) {
            openTimeoutPicker()
            onActivationConsumed()
        }
    }

    Column {
        Text(stringResource(R.string.settings_sleep_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_sleep_standby),
                value = minutesLabel(settings.sleepTimeoutMinutes),
                description = stringResource(R.string.settings_sleep_standby_description)
            ),
            selected = selectedIndex == 0,
            valueColor = SettingsAccentTeal,
            onClick = { openTimeoutPicker() }
        )
        Spacer(Modifier.height(4.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_sleep_media),
            checked = settings.sleepMediaEnabled,
            selected = selectedIndex == 1,
            onClick = onMediaToggle
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.settings_sleep_media_hint),
            color = SettingsGray,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
        if (!settings.rootAccessGranted && !controller.canEnterSleep) SettingsCapabilitiesNote(stringResource(R.string.settings_sleep_capability))
    }

    if (showTimeoutPicker) {
        HorizonOverlay(
            title = stringResource(R.string.settings_sleep_standby),
            onDismiss = { showTimeoutPicker = false },
            onDirectionalKey = { key ->
                when (key) {
                    androidx.compose.ui.input.key.Key.DirectionDown,
                    androidx.compose.ui.input.key.Key.DirectionRight -> {
                        timeoutIndex = (timeoutIndex + 1).coerceAtMost(standbyTimeouts.lastIndex)
                        true
                    }
                    androidx.compose.ui.input.key.Key.DirectionUp,
                    androidx.compose.ui.input.key.Key.DirectionLeft -> {
                        timeoutIndex = (timeoutIndex - 1).coerceAtLeast(0)
                        true
                    }
                    else -> false
                }
            }
        ) {
            standbyTimeouts.forEachIndexed { index, timeout ->
                HorizonOverlayChoice(
                    title = minutesLabel(timeout),
                    selected = timeoutIndex == index,
                    titleColor = if (timeout == settings.sleepTimeoutMinutes) SettingsAccentTeal else null,
                    onClick = {
                        onTimeoutSelected(timeout)
                        showTimeoutPicker = false
                    }
                )
            }
        }
    }
}
