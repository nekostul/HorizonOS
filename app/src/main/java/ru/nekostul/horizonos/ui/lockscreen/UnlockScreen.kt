package ru.nekostul.horizonos.ui.lockscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.home.status.StatusAirplaneIcon
import ru.nekostul.horizonos.ui.home.status.StatusBatteryIcon
import ru.nekostul.horizonos.ui.home.status.StatusClock
import ru.nekostul.horizonos.ui.home.status.StatusWifiIcon
import ru.nekostul.horizonos.ui.home.status.batteryBarColor
import ru.nekostul.horizonos.ui.home.status.rememberAirplaneModeEnabled
import ru.nekostul.horizonos.ui.home.status.rememberBatteryState
import ru.nekostul.horizonos.ui.home.status.rememberWifiEnabled
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

/** Opacity of the dark scrim over the blurred Home. */
private const val ScrimAlpha = 0.68f

/** How long a press sequence stays valid before it resets. */
private const val PRESS_RESET_TIMEOUT_MILLIS = 1500L

/**
 * HorizonOS unlock screen. Shown on launch and after the device wakes from the
 * screen lock. The user must press the house button (or "Launch") once or three
 * times, depending on the "Screen lock" setting.
 *
 * [unlockProgress] is driven by the host and runs 0 -> 1 during the unlock
 * animation, so the scrim and the blurred Home fade away in sync.
 */
@Composable
fun UnlockScreen(
    unlockProgress: Float,
    onUnlockStart: () -> Unit,
    pressRequired: Int = 3
) {
    val palette = LocalHorizonColors.current
    val backgroundColor = palette.background
    val statusColor = palette.text
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var pressCount by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }
    var resetJob by remember { mutableStateOf<Job?>(null) }
    val pressScale = remember { Animatable(1f) }

    val remaining = (1f - unlockProgress).coerceIn(0f, 1f)

    fun press() {
        if (started) return
        scope.launch {
            pressScale.animateTo(0.88f, tween(90, easing = FastOutSlowInEasing))
            pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        val next = pressCount + 1
        pressCount = next
        // Restart the idle timer: too slow and the whole sequence resets.
        resetJob?.cancel()
        if (next >= pressRequired) {
            started = true
            scope.launch {
                delay(150)
                onUnlockStart()
            }
        } else {
            resetJob = scope.launch {
                delay(PRESS_RESET_TIMEOUT_MILLIS)
                pressCount = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        // Grab (and keep) focus so the gamepad controls the lock screen even
        // though the Home screen is composed behind it.
        repeat(4) {
            focusRequester.requestFocus()
            delay(60)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Slightly see-through so the blurred Home shows behind; fades out
            // together with the unlock animation.
            .background(backgroundColor.copy(alpha = ScrimAlpha * remaining))
            .pointerInput(Unit) { detectTapGestures { } }
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    isHorizonConfirmKey(event) -> { press(); true }
                    // B, Back and HOME must not bypass the lock.
                    event.key == Key.ButtonB || event.key == Key.Back ||
                        HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode) -> true
                    else -> false
                }
            }
    ) {
        val h = maxHeight

        // Same status icons as the Home screen, top-right.
        val wifiEnabled = rememberWifiEnabled()
        val airplaneEnabled = rememberAirplaneModeEnabled()
        val battery = rememberBatteryState()
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { alpha = remaining }
                .padding(top = h * 0.042f, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(h * 0.014f)
        ) {
            StatusClock(
                color = statusColor,
                fontSize = (h.value * 0.042f).sp
            )
            if (airplaneEnabled) {
                StatusAirplaneIcon(color = statusColor, size = h * 0.030f)
            }
            if (wifiEnabled) {
                StatusWifiIcon(color = statusColor, size = h * 0.032f)
            }
            StatusBatteryIcon(
                level = battery.level,
                fillColor = batteryBarColor(battery),
                height = h * 0.030f - 1.dp,
                percentageFontSize = (h.value * 0.039f).sp,
                modifier = Modifier.offset(y = (0).dp)
            )
        }

        // Central round unlock button with the house silhouette.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(96.dp)
                .graphicsLayer {
                    // Gentle "breathing" of the ring on each press, then a
                    // smooth grow-and-fade during unlock.
                    val scale = pressScale.value * (1f + unlockProgress * 0.6f)
                    scaleX = scale
                    scaleY = scale
                    alpha = remaining
                }
                .clip(CircleShape)
                .clickable { press() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f

                // Expanding soft halo during the unlock animation.
                if (unlockProgress > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f * (1f - unlockProgress)),
                        radius = radius + 46.dp.toPx() * unlockProgress,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = radius,
                    style = Stroke(width = stroke)
                )
                val box = size.minDimension * 0.48f
                val left = (size.minDimension - box) / 2f
                val top = (size.minDimension - box) / 2f
                drawHouse(
                    left = left,
                    top = top,
                    box = box,
                    color = Color.White
                )
            }
        }

        // Progress dots: how many of the required presses are done.
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 76.dp)
                .graphicsLayer { alpha = remaining },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pressRequired) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pressCount) Color.White
                            else Color.White.copy(alpha = 0.28f)
                        )
                )
            }
        }

        // "A  Launch" control at the bottom centre (no background plate).
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = remaining }
                .clickable { press() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizonButtonGlyph(label = "A", size = 24.dp)
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.unlock_launch),
                color = statusColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** A simple, symmetric white house silhouette with a cut-out door. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHouse(
    left: Float,
    top: Float,
    box: Float,
    color: Color
) {
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        // Outer silhouette: roof + walls.
        moveTo(left + box * 0.50f, top + box * 0.06f)
        lineTo(left + box * 0.95f, top + box * 0.46f)
        lineTo(left + box * 0.82f, top + box * 0.46f)
        lineTo(left + box * 0.82f, top + box * 0.94f)
        lineTo(left + box * 0.18f, top + box * 0.94f)
        lineTo(left + box * 0.18f, top + box * 0.46f)
        lineTo(left + box * 0.05f, top + box * 0.46f)
        close()
        // Door / window hole (shows the backdrop through).
        moveTo(left + box * 0.42f, top + box * 0.64f)
        lineTo(left + box * 0.58f, top + box * 0.64f)
        lineTo(left + box * 0.58f, top + box * 0.94f)
        lineTo(left + box * 0.42f, top + box * 0.94f)
        close()
    }
    drawPath(path = path, color = color)
}
