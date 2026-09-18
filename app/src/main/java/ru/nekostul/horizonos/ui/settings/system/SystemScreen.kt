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
        SettingRow(stringResource(R.string.settings_system_battery), "", stringResource(R.string.settings_system_battery_description))
    )
    var overlayIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var languageChoice by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(openOverlayIndex) {
        openOverlayIndex?.let {
            overlayIndex = it.coerceIn(0, rows.lastIndex)
            onOverlayRequestConsumed()
        }
    }
    LaunchedEffect(overlayIndex, language) {
        if (overlayIndex == 1) {
            languageChoice = if (LanguageManager.effectiveLanguage(context, language) == LanguageManager.RUSSIAN) 0 else 1
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
            onDismiss = { overlayIndex = null },
            onDirectionalKey = if (index == 1) {
                { key ->
                    when (key) {
                        androidx.compose.ui.input.key.Key.DirectionDown,
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            languageChoice = (languageChoice + 1).coerceAtMost(1)
                            true
                        }
                        androidx.compose.ui.input.key.Key.DirectionUp,
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            languageChoice = (languageChoice - 1).coerceAtLeast(0)
                            true
                        }
                        else -> false
                    }
                }
            } else null
        ) {
            when (index) {
                0 -> DateTimeScreen(rootAccessGranted = settings.rootAccessGranted)
                1 -> LanguageSettingsScreen(
                    language = language,
                    onLanguageSelected = { selected ->
                        onLanguageSelected(selected)
                        overlayIndex = null
                    },
                    highlightedIndex = languageChoice
                )
                else -> BatterySettingsScreen()
            }
        }
    }
}
