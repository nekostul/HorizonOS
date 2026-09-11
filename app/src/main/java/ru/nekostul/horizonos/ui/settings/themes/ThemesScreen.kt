package ru.nekostul.horizonos.ui.settings.themes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsGray

@Composable
fun ThemesScreen(settings: LauncherSettings, selectedIndex: Int, onThemeSelected: (String) -> Unit) {
    val rows = listOf(
        SettingRow(stringResource(R.string.settings_theme_dark)),
        SettingRow(stringResource(R.string.settings_theme_light))
    )
    Column {
        rows.forEachIndexed { index, row ->
            ru.nekostul.horizonos.ui.settings.HorizonSettingRow(
                row,
                selectedIndex == index,
                onClick = { onThemeSelected(if (index == 0) "dark" else "light") },
                trailing = {
                    Box(
                        Modifier
                            .width(54.dp)
                            .height(30.dp)
                            .background(if (index == 0) Color.Black else Color.White)
                            .border(1.dp, SettingsGray)
                    )
                }
            )
        }
    }
}
