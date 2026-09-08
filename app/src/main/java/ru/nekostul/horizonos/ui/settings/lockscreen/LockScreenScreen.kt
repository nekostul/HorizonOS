package ru.nekostul.horizonos.ui.settings.lockscreen

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

private val lockTimeouts = listOf(0, 1, 5, 10, 30)

@Composable
fun LockScreenScreen(
    context: Context,
    settings: LauncherSettings,
    selectedIndex: Int,
    onToggle: () -> Unit,
    onTimeoutSelected: (Int) -> Unit
) {
    val controller = LockScreenController(context)
    Column {
        Text(stringResource(R.string.settings_lock_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_auto_lock),
            checked = settings.lockScreenEnabled,
            selected = selectedIndex == 0,
            description = stringResource(R.string.settings_auto_lock_description),
            onClick = onToggle
        )
        lockTimeouts.forEachIndexed { index, timeout ->
            SettingsToggleRow(
                title = stringResource(R.string.settings_lock_after, minutesLabel(timeout)),
                checked = settings.lockScreenTimeoutMinutes == timeout,
                selected = selectedIndex == index + 1,
                enabled = settings.lockScreenEnabled,
                onClick = { onTimeoutSelected(timeout) }
            )
        }
        if (!controller.canChangeSystemTimeout) {
            SettingsCapabilitiesNote(stringResource(R.string.settings_lock_capability))
        }
    }
}
