package ru.nekostul.horizonos.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun OnboardingWaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalHorizonColors.current
    val transition = rememberInfiniteTransition(label = "onboardingWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingWavePhase"
    )
    val base = palette.background
    val deep = lerp(base, Color.Black, if (base.luminance() > 0.45f) 0.10f else 0.28f)
    val accent = palette.accent

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(deep, base, lerp(base, accent, 0.08f))
                )
            )

            val width = size.width
            val height = size.height
            val segments = 28
            val layers = listOf(
                Triple(0.18f, 0.070f, accent.copy(alpha = 0.16f)),
                Triple(0.38f, 0.095f, lerp(accent, Color.White, 0.24f).copy(alpha = 0.10f)),
                Triple(0.62f, 0.120f, accent.copy(alpha = 0.12f)),
                Triple(0.82f, 0.075f, lerp(accent, Color.Black, 0.18f).copy(alpha = 0.14f))
            )

            layers.forEachIndexed { index, (center, amplitude, color) ->
                val path = Path()
                val yBase = height * center
                path.moveTo(0f, height)
                path.lineTo(0f, yBase)
                for (point in 0..segments) {
                    val x = width * point / segments
                    val angle = (point.toFloat() / segments) * (PI * 2.0) +
                        phase * (PI * 2.0) + index * 0.86
                    val y = yBase + sin(angle) * height * amplitude
                    path.lineTo(x, y.toFloat())
                }
                path.lineTo(width, height)
                path.close()
                drawPath(path, color)

                val line = Path()
                for (point in 0..segments) {
                    val x = width * point / segments
                    val angle = (point.toFloat() / segments) * (PI * 2.0) +
                        phase * (PI * 2.0) + index * 0.86
                    val y = yBase + sin(angle) * height * amplitude
                    if (point == 0) line.moveTo(x, y.toFloat()) else line.lineTo(x, y.toFloat())
                }
                drawPath(
                    path = line,
                    color = color.copy(alpha = color.alpha * 1.45f),
                    style = Stroke(width = 1.2f)
                )
            }

            val horizonY = height * (0.49f + sin(phase * (PI * 2.0)).toFloat() * 0.02f)
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = androidx.compose.ui.geometry.Offset(width * 0.08f, horizonY),
                end = androidx.compose.ui.geometry.Offset(width * 0.92f, horizonY),
                strokeWidth = 1f
            )
        }
        content()
    }
}
