package ru.nekostul.horizonos.ui.home.status

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

data class BatteryState(
    val level: Int,
    val charging: Boolean,
    val powerSave: Boolean
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun batteryBarColor(state: BatteryState): Color = LocalHorizonColors.current.text

private fun readBatteryState(context: Context): BatteryState {
    val intent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

    val percent = if (level >= 0 && scale > 0) {
        level * 100 / scale
    } else {
        0
    }

    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val powerSave = runCatching {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
    }.getOrDefault(false)

    return BatteryState(
        level = percent.coerceIn(0, 100),
        charging = charging,
        powerSave = powerSave
    )
}

@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readBatteryState(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            state = readBatteryState(context)
            delay(5000)
        }
    }

    return state
}

@Composable
fun StatusBatteryIcon(
    level: Int,
    fillColor: Color,
    height: Dp,
    percentageFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val percentage = level.coerceIn(0, 100)

    val batteryWidth = height * 1.70f
    val terminalWidth = height * 0.16f
    val terminalHeight = height * 0.34f
    val gap = height * 0.50f

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$percentage%",
            color = fillColor,
            fontSize = percentageFontSize,
            fontWeight = FontWeight.Normal
        )

        Spacer(Modifier.width(gap))

        Canvas(
            modifier = Modifier
                .width(batteryWidth + terminalWidth)
                .height(height)
        ) {
            val bodyWidth = batteryWidth.toPx()
            val terminalW = terminalWidth.toPx()
            val terminalH = terminalHeight.toPx()
            val h = size.height

            val stroke = (h * 0.18f).coerceAtLeast(2.2f)
            val radius = (h * 0.06f).coerceAtLeast(1f)

            drawRoundRect(
                color = fillColor,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(
                    width = bodyWidth - stroke,
                    height = h - stroke
                ),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke)
            )

            val innerLeft = stroke * 1.45f
            val innerTop = stroke * 1.45f
            val innerRight = bodyWidth - stroke * 1.45f
            val innerBottom = h - stroke * 1.45f
            val innerWidth = (innerRight - innerLeft).coerceAtLeast(0f)
            val innerHeight = (innerBottom - innerTop).coerceAtLeast(0f)

            val fraction = percentage / 100f
            val barWidth = innerWidth * fraction

            if (barWidth > 0f && innerHeight > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(innerLeft, innerTop),
                    size = Size(
                        width = barWidth,
                        height = innerHeight
                    ),
                    cornerRadius = CornerRadius(
                        innerHeight * 0.12f,
                        innerHeight * 0.12f
                    )
                )
            }

            drawRoundRect(
                color = fillColor,
                topLeft = Offset(
                    bodyWidth,
                    h / 2f - terminalH / 2f
                ),
                size = Size(
                    width = terminalW,
                    height = terminalH
                ),
                cornerRadius = CornerRadius(
                    terminalH * 0.10f,
                    terminalH * 0.10f
                )
            )
        }
    }
}
