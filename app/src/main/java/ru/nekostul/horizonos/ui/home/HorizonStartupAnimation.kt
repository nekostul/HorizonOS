package ru.nekostul.horizonos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.delay
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HorizonStartupAnimation() {
    val palette = LocalHorizonColors.current
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(70)
        val start = withFrameNanos { it }
        val durationNanos = 1_180_000_000L
        while (progress < 1f) {
            val frame = withFrameNanos { it }
            progress = ((frame - start).toFloat() / durationNanos).coerceIn(0f, 1f)
        }
    }

    val reveal = FastOutSlowInEasing.transform(progress)
    val logoScale = when {
        progress < 0.62f -> 0.54f + 0.56f * FastOutSlowInEasing.transform(progress / 0.62f)
        else -> 1.10f - 0.10f * FastOutSlowInEasing.transform(((progress - 0.62f) / 0.38f).coerceIn(0f, 1f))
    }
    val logoRotation = (1f - FastOutSlowInEasing.transform((progress / 0.72f).coerceIn(0f, 1f))) * -12f
    val logoAlpha = (progress / 0.18f).coerceIn(0f, 1f)
    val ringProgress = FastOutSlowInEasing.transform((progress / 0.88f).coerceIn(0f, 1f))
    val ringAlpha = (progress / 0.24f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(244.dp)
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.30f
                    drawCircle(
                        color = Color(0xFF00E8C8).copy(alpha = ringAlpha * 0.11f),
                        radius = radius * (2.05f - ringProgress * 0.26f),
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF00E8C8).copy(alpha = ringAlpha * 0.40f),
                        radius = radius * (1.44f - ringProgress * 0.13f),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFB4FDFF).copy(alpha = ringAlpha * 0.42f),
                        radius = radius * (1.68f - ringProgress * 0.15f),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFFFF8A65).copy(alpha = ringAlpha * 0.72f),
                        topLeft = Offset(center.x - radius * 1.84f, center.y - radius * 1.84f),
                        size = Size(radius * 3.68f, radius * 3.68f),
                        startAngle = 202f,
                        sweepAngle = 58f * ringProgress,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            HorizonLogoMark(
                modifier = Modifier
                    .size(154.dp)
                    .graphicsLayer {
                        alpha = logoAlpha
                        scaleX = logoScale
                        scaleY = logoScale
                        rotationZ = logoRotation
                    }
            )
        }
    }
}

@Composable
private fun HorizonLogoMark(modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.clip(CircleShape)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.49f
        val horizon = center.y + radius * 0.18f
        val stroke = Stroke(
            width = size.minDimension * 0.045f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF244C5B),
                    Color(0xFF132A3B),
                    Color(0xFF0C1726)
                ),
                center = Offset(center.x, center.y * 0.78f),
                radius = size.minDimension * 0.78f
            ),
            radius = radius,
            center = center
        )

        // A quiet star field gives the mark depth without competing with the H.
        listOf(
            Offset(0.28f, 0.29f),
            Offset(0.70f, 0.24f),
            Offset(0.78f, 0.40f),
            Offset(0.20f, 0.49f)
        ).forEach { point ->
            drawCircle(
                color = Color(0xFFB4FDFF).copy(alpha = 0.48f),
                radius = size.minDimension * 0.012f,
                center = Offset(size.width * point.x, size.height * point.y)
            )
        }

        // Rising sun, cut cleanly by the horizon line.
        drawCircle(
            color = Color(0xFFFF8A65).copy(alpha = 0.20f),
            radius = radius * 0.37f,
            center = Offset(center.x, horizon - radius * 0.34f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFC27A), Color(0xFFFF6F61))
            ),
            radius = radius * 0.23f,
            center = Offset(center.x, horizon - radius * 0.34f)
        )

        // Latitude lines turn the lower half into a stylised horizon globe.
        for (index in 1..3) {
            val y = horizon + radius * index * 0.16f
            val halfWidth = radius * (0.72f - index * 0.11f)
            drawLine(
                color = Color(0xFF66E8DF).copy(alpha = 0.20f),
                start = Offset(center.x - halfWidth, y),
                end = Offset(center.x + halfWidth, y),
                strokeWidth = size.minDimension * 0.012f,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = Color(0xFF66E8DF),
            start = Offset(center.x - radius * 0.82f, horizon),
            end = Offset(center.x + radius * 0.82f, horizon),
            strokeWidth = size.minDimension * 0.035f,
            cap = StrokeCap.Round
        )

        // HorizonOS monogram: two pillars and a bright crossing horizon.
        val hTop = horizon - radius * 0.31f
        val hBottom = horizon + radius * 0.39f
        val hLeft = center.x - radius * 0.33f
        val hRight = center.x + radius * 0.33f
        drawLine(Color.White, Offset(hLeft, hTop), Offset(hLeft, hBottom), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(hRight, hTop), Offset(hRight, hBottom), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(hLeft, horizon + radius * 0.03f), Offset(hRight, horizon + radius * 0.03f), strokeWidth = stroke.width, cap = StrokeCap.Round)

        // Small orbit ticks echo the navigation ring around the logo.
        repeat(4) { index ->
            val angle = index * PI / 2.0 + PI / 4.0
            val inner = radius * 1.02f
            val outer = radius * 1.10f
            drawLine(
                color = Color(0xFFB4FDFF).copy(alpha = 0.72f),
                start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = size.minDimension * 0.012f,
                cap = StrokeCap.Round
            )
        }
    }
}
