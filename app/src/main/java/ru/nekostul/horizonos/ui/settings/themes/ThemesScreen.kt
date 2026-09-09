package ru.nekostul.horizonos.ui.settings.themes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsBlue
import ru.nekostul.horizonos.ui.settings.SettingsBackground
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(54.dp)
                                .height(30.dp)
                                .background(if (index == 0) Color.Black else Color.White)
                                .border(1.dp, SettingsGray)
                        )
                        Spacer(Modifier.width(12.dp))
                        if ((index == 0 && settings.theme == "dark") || (index == 1 && settings.theme == "light")) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .background(SettingsBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = SettingsBackground, fontSize = 18.sp)
                            }
                        }
                    }
                }
            )
        }
    }
}
