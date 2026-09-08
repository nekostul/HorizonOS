package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.LanguageManager
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay

@Composable
fun SystemScreen(
    settings: LauncherSettings,
    language: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onInterfaceScaleChange: (Float) -> Unit,
    onAnimationsToggle: () -> Unit,
    onInterfaceSoundsToggle: () -> Unit,
    openOverlayIndex: Int? = null,
    onOverlayRequestConsumed: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val languageName = when (LanguageManager.effectiveLanguage(context, language)) {
        LanguageManager.RUSSIAN -> stringResource(R.string.language_russian)
        else -> stringResource(R.string.language_english)
    }
    val rows = listOf(
        SettingRow(stringResource(R.string.settings_system_date_time), "", stringResource(R.string.settings_system_date_time_description)),
        SettingRow(stringResource(R.string.settings_system_language), languageName, stringResource(R.string.settings_system_language_description)),
        SettingRow(stringResource(R.string.settings_system_display), "", stringResource(R.string.settings_system_display_description)),
        SettingRow(stringResource(R.string.settings_system_sound), "", stringResource(R.string.settings_system_sound_description)),
        SettingRow(stringResource(R.string.settings_system_accessibility), "", stringResource(R.string.settings_system_accessibility_description)),
        SettingRow(stringResource(R.string.settings_system_apps), "", stringResource(R.string.settings_system_apps_description)),
        SettingRow(stringResource(R.string.settings_system_battery), "", stringResource(R.string.settings_system_battery_description)),
        SettingRow(stringResource(R.string.settings_system_info), "", stringResource(R.string.settings_system_info_description))
    )
    var overlayIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    LaunchedEffect(openOverlayIndex) {
        openOverlayIndex?.let {
            overlayIndex = it.coerceIn(0, rows.lastIndex)
            onOverlayRequestConsumed()
        }
    }
    Column {
        Text(stringResource(R.string.settings_category_system), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { index, row ->
            ru.nekostul.horizonos.ui.settings.HorizonSettingRow(
                row,
                selectedIndex == index,
                onClick = { onSelect(index); overlayIndex = index }
            )
        }
    }
    overlayIndex?.let { index ->
        HorizonOverlay(
            title = rows[index].title,
            onDismiss = { overlayIndex = null }
        ) {
            when (index) {
                0 -> DateTimeScreen()
                1 -> LanguageSettingsScreen(language, onLanguageSelected)
                2 -> DisplaySettingsScreen(settings, onInterfaceScaleChange, onAnimationsToggle)
                3 -> SoundSettingsScreen(settings, onInterfaceSoundsToggle)
                4 -> AccessibilityScreen(settings, onAnimationsToggle, onInterfaceScaleChange)
                5 -> AppsSettingsScreen()
                6 -> BatterySettingsScreen()
                else -> SystemInfoScreen()
            }
        }
    }
}
