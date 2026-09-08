package ru.nekostul.horizonos.ui.settings.sleep

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.minutesLabel

private val sleepTimeouts = listOf(0, 5, 10, 30, 60)

@Composable
fun SleepScreen(context: Context, settings: LauncherSettings, selectedIndex: Int, onToggle: () -> Unit, onTimeoutSelected: (Int) -> Unit) {
    val controller = SleepController(context)
    Column {
        Text(stringResource(R.string.settings_sleep_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(stringResource(R.string.settings_auto_sleep), settings.sleepEnabled, selectedIndex == 0, stringResource(R.string.settings_sleep_description), onClick = onToggle)
        sleepTimeouts.forEachIndexed { index, timeout ->
            SettingsToggleRow(stringResource(R.string.settings_sleep_after, minutesLabel(timeout)), settings.sleepTimeoutMinutes == timeout, selectedIndex == index + 1, enabled = settings.sleepEnabled, onClick = { onTimeoutSelected(timeout) })
        }
        if (!controller.canEnterSleep) SettingsCapabilitiesNote(stringResource(R.string.settings_sleep_capability))
    }
}
