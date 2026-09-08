package ru.nekostul.horizonos.ui.settings.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice

@Composable
fun NotificationsScreen(settings: LauncherSettings, selectedIndex: Int, onToggle: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { NotificationController(context) }
    val apps = remember { controller.installedApps() }
    var selectedApp by remember { mutableStateOf<NotificationController.AppNotificationInfo?>(null) }
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
        HorizonSettingRow(ru.nekostul.horizonos.ui.settings.SettingRow(stringResource(R.string.settings_notifications_apps), apps.size.toString(), stringResource(R.string.settings_notifications_apps_description)), selectedIndex == 1, onClick = {})
        apps.take(30).forEach { app ->
            HorizonSettingRow(
                ru.nekostul.horizonos.ui.settings.SettingRow(app.label, stringResource(R.string.settings_notifications_view_only), app.packageName),
                selected = false,
                onClick = { selectedApp = app }
            )
        }
        if (!controller.canPost) SettingsCapabilitiesNote(stringResource(R.string.settings_notifications_permission))
        if (!controller.canChangeOtherApps) SettingsCapabilitiesNote(stringResource(R.string.settings_notifications_capability))
    }
    selectedApp?.let { app ->
        HorizonOverlay(app.label, { selectedApp = null }) {
            Text(app.packageName, color = SettingsWhite, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.settings_notifications_app_state_unavailable), color = SettingsGray, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            HorizonOverlayChoice(stringResource(R.string.settings_action_cancel), false, { selectedApp = null })
        }
    }
}
