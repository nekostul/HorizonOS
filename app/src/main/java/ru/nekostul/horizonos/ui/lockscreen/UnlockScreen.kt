package ru.nekostul.horizonos.ui.lockscreen

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

private const val ScrimAlpha = 0.68f

private const val PRESS_RESET_TIMEOUT_MILLIS = 1500L

/**
 * Keep the launcher lock screen in its own window so it stays above any
 * keyboard or launcher dialog that was open when the phone was locked.
 */
@Composable
fun HorizonLockDialog(
    unlockProgress: Float,
    onUnlockStart: () -> Unit,
    pressRequired: Int = 3
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val blurRadius = with(LocalDensity.current) { 20.dp.roundToPx() }
        androidx.compose.runtime.DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                window.setDimAmount(0f)
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = window.context.getSystemService(WindowManager::class.java)
                    if (manager?.isCrossWindowBlurEnabled == true) {
                        // Dialogs such as the launcher keyboard are separate windows;
                        // blur-behind makes the lock screen match the home-screen lock
                        // even when one of those windows is still open underneath.
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.setBackgroundBlurRadius(blurRadius)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = blurRadius
                        }
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    }
                }
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                onDispose { }
            }
        }
        UnlockScreen(
            unlockProgress = unlockProgress,
            onUnlockStart = onUnlockStart,
            pressRequired = pressRequired
        )
    }
}

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
        repeat(4) {
            focusRequester.requestFocus()
            delay(60)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = ScrimAlpha * remaining))
            .pointerInput(Unit) { detectTapGestures { } }
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    isHorizonConfirmKey(event) -> { press(); true }
                    event.key == Key.ButtonB || event.key == Key.Back ||
                        HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode) -> true
                    else -> false
                }
            }
    ) {
        val h = maxHeight

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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(96.dp)
                .graphicsLayer {
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHouse(
    left: Float,
    top: Float,
    box: Float,
    color: Color
) {
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(left + box * 0.50f, top + box * 0.06f)
        lineTo(left + box * 0.95f, top + box * 0.46f)
        lineTo(left + box * 0.82f, top + box * 0.46f)
        lineTo(left + box * 0.82f, top + box * 0.94f)
        lineTo(left + box * 0.18f, top + box * 0.94f)
        lineTo(left + box * 0.18f, top + box * 0.46f)
        lineTo(left + box * 0.05f, top + box * 0.46f)
        close()
        moveTo(left + box * 0.42f, top + box * 0.64f)
        lineTo(left + box * 0.58f, top + box * 0.64f)
        lineTo(left + box * 0.58f, top + box * 0.94f)
        lineTo(left + box * 0.42f, top + box * 0.94f)
        close()
    }
    drawPath(path = path, color = color)
}
