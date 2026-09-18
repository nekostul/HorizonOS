package ru.nekostul.horizonos.ui.settings.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun NotificationsScreen(settings: LauncherSettings, selectedIndex: Int, onToggle: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { NotificationController(context) }
    Column {
        Text(stringResource(R.string.settings_notifications_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_notifications_horizon),
            checked = settings.notificationsEnabled && controller.areNotificationsEnabled(),
            selected = selectedIndex == 0,
            description = stringResource(R.string.settings_notifications_description),
            onClick = onToggle
        )
        if (!controller.canPost) SettingsCapabilitiesNote(stringResource(R.string.settings_notifications_permission))
    }
}
