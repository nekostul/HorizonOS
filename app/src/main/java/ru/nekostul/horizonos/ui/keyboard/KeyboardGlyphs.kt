package ru.nekostul.horizonos.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun HorizonGamepadGlyph(
    size: Dp = 54.dp,
    color: Color
) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val stroke = d * 0.075f
        val body = Path().apply {
            moveTo(d * 0.24f, d * 0.33f)
            cubicTo(d * 0.12f, d * 0.34f, d * 0.08f, d * 0.53f, d * 0.12f, d * 0.76f)
            cubicTo(d * 0.15f, d * 0.91f, d * 0.29f, d * 0.91f, d * 0.37f, d * 0.75f)
            lineTo(d * 0.63f, d * 0.75f)
            cubicTo(d * 0.71f, d * 0.91f, d * 0.85f, d * 0.91f, d * 0.88f, d * 0.76f)
            cubicTo(d * 0.92f, d * 0.53f, d * 0.88f, d * 0.34f, d * 0.76f, d * 0.33f)
            close()
        }
        drawPath(body, color, style = Stroke(stroke, join = StrokeJoin.Round))
        drawLine(color, Offset(d * 0.25f, d * 0.49f), Offset(d * 0.25f, d * 0.64f), stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(d * 0.18f, d * 0.565f), Offset(d * 0.32f, d * 0.565f), stroke, cap = StrokeCap.Round)
        drawCircle(color, d * 0.045f, Offset(d * 0.70f, d * 0.50f))
        drawCircle(color, d * 0.045f, Offset(d * 0.78f, d * 0.58f))
        drawCircle(color, d * 0.035f, Offset(d * 0.61f, d * 0.45f), style = Stroke(stroke * 0.55f))
        drawCircle(color, d * 0.035f, Offset(d * 0.85f, d * 0.45f), style = Stroke(stroke * 0.55f))
    }
}

@Composable
internal fun HorizonGlobeGlyph(
    size: Dp = 24.dp,
    color: Color
) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val stroke = d * 0.085f
        drawCircle(color, d * 0.42f, center = Offset(d / 2f, d / 2f), style = Stroke(stroke))
        drawOval(
            color = color,
            topLeft = Offset(d * 0.31f, d * 0.08f),
            size = androidx.compose.ui.geometry.Size(d * 0.38f, d * 0.84f),
            style = Stroke(stroke)
        )
        drawLine(color, Offset(d * 0.10f, d * 0.50f), Offset(d * 0.90f, d * 0.50f), stroke)
    }
}

@Composable
internal fun HorizonShiftGlyph(
    size: Dp = 24.dp,
    color: Color
) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val path = Path().apply {
            moveTo(d * 0.50f, d * 0.10f)
            lineTo(d * 0.15f, d * 0.48f)
            lineTo(d * 0.34f, d * 0.48f)
            lineTo(d * 0.34f, d * 0.82f)
            lineTo(d * 0.66f, d * 0.82f)
            lineTo(d * 0.66f, d * 0.48f)
            lineTo(d * 0.85f, d * 0.48f)
            close()
        }
        drawPath(path, color, style = Stroke(d * 0.09f, join = StrokeJoin.Round))
    }
}

@Composable
internal fun HorizonBackspaceGlyph(
    size: Dp = 23.dp,
    color: Color
) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val path = Path().apply {
            moveTo(d * 0.08f, d * 0.50f)
            lineTo(d * 0.29f, d * 0.18f)
            lineTo(d * 0.90f, d * 0.18f)
            lineTo(d * 0.90f, d * 0.82f)
            lineTo(d * 0.29f, d * 0.82f)
            close()
        }
        drawPath(path, color, style = Stroke(d * 0.08f, join = StrokeJoin.Round))
        drawLine(color, Offset(d * 0.43f, d * 0.39f), Offset(d * 0.70f, d * 0.61f), d * 0.08f, cap = StrokeCap.Round)
        drawLine(color, Offset(d * 0.70f, d * 0.39f), Offset(d * 0.43f, d * 0.61f), d * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
internal fun HorizonLeftStickGlyph(
    size: Dp = 24.dp,
    color: Color
) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        drawCircle(color, d * 0.25f, center = Offset(d / 2f, d * 0.59f), style = Stroke(d * 0.08f))
        drawLine(color, Offset(d / 2f, d * 0.16f), Offset(d / 2f, d * 0.40f), d * 0.09f, cap = StrokeCap.Round)
        drawCircle(color, d * 0.12f, center = Offset(d / 2f, d * 0.14f))
    }
}

@Composable
internal fun HorizonBumperGlyph(
    label: String,
    size: Dp = 28.dp,
    color: Color
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val d = this.size.minDimension
            drawRoundRect(
                color = color,
                topLeft = Offset(d * 0.08f, d * 0.25f),
                size = androidx.compose.ui.geometry.Size(d * 0.84f, d * 0.50f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(d * 0.12f),
                style = Stroke(d * 0.08f)
            )
        }
        Text(
            label,
            color = color,
            fontSize = (size.value * 0.27f).sp,
            lineHeight = (size.value * 0.27f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}
