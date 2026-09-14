package ru.nekostul.horizonos.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Xbox HOME button glyph. Drawn in code so no bitmap asset is needed; the
 * filled circle matches the other [HorizonButtonGlyph] buttons used across
 * HorizonOS.
 */
@Composable
fun HorizonXboxGlyph(
    size: Dp = 22.dp,
    fill: Color = Color.White,
    contentColor: Color = Color(0xFF2B2B2B)
) {
    Canvas(modifier = Modifier.size(size)) {
        val d = this.size.minDimension
        drawCircle(color = fill, radius = d / 2f)
        val stroke = d * 0.11f
        val path = Path().apply {
            moveTo(d * 0.30f, d * 0.30f)
            quadraticBezierTo(d * 0.40f, d * 0.55f, d * 0.70f, d * 0.70f)
            moveTo(d * 0.70f, d * 0.30f)
            quadraticBezierTo(d * 0.60f, d * 0.55f, d * 0.30f, d * 0.70f)
        }
        drawPath(
            path = path,
            color = contentColor,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/** Gamepad START button glyph: a filled button with three horizontal bars. */
@Composable
fun HorizonStartGlyph(
    size: Dp = 22.dp,
    fill: Color = Color.White,
    contentColor: Color = Color(0xFF2B2B2B)
) {
    Canvas(modifier = Modifier.size(size)) {
        val d = this.size.minDimension
        drawCircle(color = fill, radius = d / 2f)
        val barWidth = d * 0.54f
        val barHeight = d * 0.11f
        val gap = d * 0.10f
        val x = (d - barWidth) / 2f
        val startY = (d - (barHeight * 3f + gap * 2f)) / 2f
        repeat(3) { index ->
            drawRoundRect(
                color = contentColor,
                topLeft = Offset(x, startY + index * (barHeight + gap)),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f)
            )
        }
    }
}
