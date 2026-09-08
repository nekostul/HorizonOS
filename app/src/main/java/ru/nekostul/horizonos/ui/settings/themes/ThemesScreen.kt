package ru.nekostul.horizonos.ui.settings.themes

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice

@Composable
fun ThemesScreen(settings: LauncherSettings, selectedIndex: Int, onThemeSelected: (String) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val rows = listOf(
        SettingRow(stringResource(R.string.settings_theme_dark), if (settings.theme == "dark") stringResource(R.string.settings_status_selected) else ""),
        SettingRow(stringResource(R.string.settings_theme_light), if (settings.theme == "light") stringResource(R.string.settings_status_selected) else "")
    )
    Column {
        Text(stringResource(R.string.settings_themes_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { index, row ->
            ru.nekostul.horizonos.ui.settings.HorizonSettingRow(row, selectedIndex == index, onClick = { showPicker = true })
        }
    }
    if (showPicker) {
        HorizonOverlay(
            title = stringResource(R.string.settings_themes_title),
            onDismiss = { showPicker = false }
        ) {
            HorizonOverlayChoice(
                stringResource(R.string.settings_theme_dark),
                selected = settings.theme == "dark",
                onClick = { onThemeSelected("dark"); showPicker = false }
            )
            HorizonOverlayChoice(
                stringResource(R.string.settings_theme_light),
                selected = settings.theme == "light",
                onClick = { onThemeSelected("light"); showPicker = false }
            )
            HorizonOverlayChoice(stringResource(R.string.settings_action_cancel), false, { showPicker = false })
        }
    }
}
