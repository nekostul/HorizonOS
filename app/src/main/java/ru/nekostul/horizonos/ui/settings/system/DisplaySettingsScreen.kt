package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow

@Composable
fun DisplaySettingsScreen(
    settings: LauncherSettings,
    onScaleChange: (Float) -> Unit,
    onAnimationsToggle: () -> Unit
) {
    Column {
        SettingsSliderRow(
            title = stringResource(R.string.settings_interface_scale),
            value = ((settings.interfaceScale - 0.8f) / 0.4f).coerceIn(0f, 1f),
            selected = false,
            valueLabel = "${(settings.interfaceScale * 100).toInt()}%",
            onValueChange = { onScaleChange(0.8f + it * 0.4f) }
        )
        Spacer(Modifier.height(8.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_animations),
            checked = settings.animations,
            selected = false,
            description = stringResource(R.string.settings_animations_description),
            onClick = onAnimationsToggle
        )
    }
}
