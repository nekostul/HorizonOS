package ru.nekostul.horizonos.ui.settings.lockscreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun LockScreenScreen(
    settings: LauncherSettings,
    selectedIndex: Int,
    onToggle: () -> Unit
) {
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
    }
}
