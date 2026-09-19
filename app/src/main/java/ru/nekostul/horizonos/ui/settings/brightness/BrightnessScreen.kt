package ru.nekostul.horizonos.ui.settings.brightness

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import kotlin.math.cos
import kotlin.math.sin

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

    var dragValue by remember { mutableFloatStateOf(settings.brightness) }
    LaunchedEffect(settings.brightness) {
        dragValue = settings.brightness
    }

    Column {
        SettingsToggleRow(
            title = stringResource(R.string.settings_auto_brightness),
            checked = automaticBrightness,
            selected = selectedIndex == 0,
            enabled = controller.canChangeSystemBrightness,
            onClick = onToggleAuto
        )
        SettingsSliderRow(
            title = "",
            value = dragValue,
            selected = selectedIndex == 1,
            enabled = !automaticBrightness,
            description = if (automaticBrightness) {
                stringResource(R.string.settings_brightness_manual_disabled)
            } else {
                stringResource(R.string.settings_brightness_window_description)
            },
            onValueChange = { value ->
                dragValue = value
                activity?.let { currentActivity ->
                    controller.setWindowBrightness(currentActivity.window, value)
                }
            },
            onValueCommit = { finalValue ->
                onBrightnessChange(finalValue)
            },
            leadingIcon = { BrightnessGlyph() }
        )
        if (!settings.rootAccessGranted && !controller.canChangeSystemBrightness) {
            SettingsCapabilitiesNote(stringResource(R.string.settings_brightness_capability))
        }
    }
}

@Composable
private fun BrightnessGlyph() {
    val textColor = SettingsWhite
    Canvas(Modifier.size(34.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.22f
        val rayStart = size.minDimension * 0.34f
        val rayEnd = size.minDimension * 0.47f
        val stroke = Stroke(width = size.minDimension * 0.075f, cap = StrokeCap.Round)
        drawCircle(color = textColor, radius = radius, center = center, style = stroke)
        repeat(8) { index ->
            val angle = Math.PI * index / 4.0
            drawLine(
                color = textColor,
                start = Offset(
                    center.x + cos(angle).toFloat() * rayStart,
                    center.y + sin(angle).toFloat() * rayStart
                ),
                end = Offset(
                    center.x + cos(angle).toFloat() * rayEnd,
                    center.y + sin(angle).toFloat() * rayEnd
                ),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round
            )
        }
    }
}
