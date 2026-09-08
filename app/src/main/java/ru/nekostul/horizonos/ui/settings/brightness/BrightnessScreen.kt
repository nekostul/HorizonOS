package ru.nekostul.horizonos.ui.settings.brightness

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun BrightnessScreen(
    settings: LauncherSettings,
    selectedIndex: Int,
    onToggleAuto: () -> Unit,
    onBrightnessChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val controller = BrightnessController(context)
    val activity = view.context as? Activity
    val automaticBrightness = controller.isAutomaticBrightnessEnabled() ?: settings.autoBrightness

    Column {
        Text(stringResource(R.string.settings_brightness_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_auto_brightness),
            checked = automaticBrightness,
            selected = selectedIndex == 0,
            description = stringResource(R.string.settings_auto_brightness_description),
            enabled = controller.canChangeSystemBrightness,
            onClick = onToggleAuto
        )
        SettingsSliderRow(
            title = stringResource(R.string.settings_brightness),
            value = settings.brightness,
            selected = selectedIndex == 1,
            enabled = !automaticBrightness,
            description = if (automaticBrightness) {
                stringResource(R.string.settings_brightness_manual_disabled)
            } else {
                stringResource(R.string.settings_brightness_window_description)
            },
            onValueChange = {
                onBrightnessChange(it)
                activity?.let { currentActivity ->
                    controller.setWindowBrightness(currentActivity.window, it)
                }
            }
        )
        if (!controller.canChangeSystemBrightness) {
            SettingsCapabilitiesNote(stringResource(R.string.settings_brightness_capability))
        }
    }
}
