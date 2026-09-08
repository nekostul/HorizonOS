package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow

@Composable
fun AccessibilityScreen(
    settings: LauncherSettings,
    onAnimationsToggle: () -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column {
        SettingsSliderRow(
            title = stringResource(R.string.settings_text_scale),
            value = ((settings.interfaceScale - 0.8f) / 0.4f).coerceIn(0f, 1f),
            selected = false,
            valueLabel = "${(settings.interfaceScale * 100).toInt()}%",
            onValueChange = { onScaleChange(0.8f + it * 0.4f) }
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_reduce_motion),
            checked = !settings.animations,
            selected = false,
            description = stringResource(R.string.settings_reduce_motion_description),
            onClick = onAnimationsToggle
        )
    }
}
