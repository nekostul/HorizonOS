package ru.nekostul.horizonos.ui.home.status

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay

private fun isWifiEnabled(context: Context): Boolean = runCatching {
    (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
}.getOrDefault(false)

@Composable
fun rememberWifiEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isWifiEnabled(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled = isWifiEnabled(context)
            delay(2000)
        }
    }
    return enabled
}

@Composable
fun StatusWifiIcon(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val dotY = h * 0.80f
        val strokeWidth = w * 0.11f
        drawCircle(color = color, radius = w * 0.075f, center = Offset(cx, dotY))
        listOf(0.24f, 0.44f, 0.64f).forEach { radiusFactor ->
            val r = w * radiusFactor
            drawArc(
                color = color,
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - r, dotY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
