package ru.nekostul.horizonos.ui.home.status

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay

private fun isAirplaneModeOn(context: Context): Boolean =
    Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

@Composable
fun rememberAirplaneModeEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isAirplaneModeOn(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled = isAirplaneModeOn(context)
            delay(2000)
        }
    }
    return enabled
}

@Composable
fun StatusAirplaneIcon(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val half = w * 0.055f
        val path = Path().apply {
            moveTo(cx, h * 0.05f)
            lineTo(cx + half, h * 0.30f)
            lineTo(cx + half, h * 0.40f)
            lineTo(w * 0.05f, h * 0.60f)
            lineTo(cx + half * 0.85f, h * 0.52f)
            lineTo(cx + w * 0.20f, h * 0.95f)
            lineTo(cx + half * 0.85f, h * 0.72f)
            lineTo(cx + half * 0.85f, h * 0.82f)
            lineTo(cx - half * 0.85f, h * 0.82f)
            lineTo(cx - half * 0.85f, h * 0.72f)
            lineTo(cx - w * 0.20f, h * 0.95f)
            lineTo(cx - half * 0.85f, h * 0.52f)
            lineTo(w * 0.95f, h * 0.60f)
            lineTo(cx - half, h * 0.40f)
            lineTo(cx - half, h * 0.30f)
            close()
        }
        drawPath(path = path, color = color)
    }
}
