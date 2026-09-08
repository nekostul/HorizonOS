package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.LanguageManager
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun LanguageSettingsScreen(language: String, onLanguageSelected: (String) -> Unit) {
    val effectiveLanguage = LanguageManager.effectiveLanguage(
        androidx.compose.ui.platform.LocalContext.current,
        language
    )
    val rows = listOf(
        SettingRow(stringResource(R.string.language_russian), if (effectiveLanguage == LanguageManager.RUSSIAN) stringResource(R.string.settings_status_selected) else ""),
        SettingRow(stringResource(R.string.language_english), if (effectiveLanguage == LanguageManager.ENGLISH) stringResource(R.string.settings_status_selected) else "")
    )
    androidx.compose.foundation.layout.Column {
        Text(stringResource(R.string.language_title), color = SettingsWhite, fontSize = 25.sp)
        Text(stringResource(R.string.language_description), color = SettingsWhite, fontSize = 17.sp)
        rows.forEachIndexed { index, row ->
            HorizonSettingRow(
                row,
                selected = (index == 0 && effectiveLanguage == LanguageManager.RUSSIAN) ||
                    (index == 1 && effectiveLanguage == LanguageManager.ENGLISH),
                onClick = {
                onLanguageSelected(when (index) {
                    0 -> LanguageManager.RUSSIAN
                    else -> LanguageManager.ENGLISH
                })
            })
        }
    }
}
