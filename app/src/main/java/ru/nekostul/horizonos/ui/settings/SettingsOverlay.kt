package ru.nekostul.horizonos.ui.settings

import android.graphics.drawable.ColorDrawable
import android.content.Context
import android.content.ClipboardManager
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.horizonLongPress
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound

internal val LocalSettingsOverlayVisible =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.MutableState<Boolean>?> { null }

internal val LocalSettingsOverlayBackHandlers =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateList<() -> Unit>?> { null }

@Suppress("DEPRECATION")
internal fun hideDialogSystemBars(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setStatusBarColor(android.graphics.Color.TRANSPARENT)
    window.setNavigationBarColor(android.graphics.Color.TRANSPARENT)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
}

@Composable
internal fun HorizonOverlay(
    title: String,
    onDismiss: () -> Unit,
    onControllerBack: (() -> Boolean)? = null,
    onFooterBack: (() -> Unit)? = null,
    scrollState: androidx.compose.foundation.ScrollState? = null,
    onDirectionalKey: ((Key) -> Boolean)? = null,
    footerAction: (@Composable () -> Unit)? = null,
    centered: Boolean = false,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    val overlayFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val overlayVisibility = LocalSettingsOverlayVisible.current
    val overlayBackHandlers = LocalSettingsOverlayBackHandlers.current
    val latestControllerBack = rememberUpdatedState(onControllerBack)
    val latestDirectionalKey = rememberUpdatedState(onDirectionalKey)
    val inputMode = LocalSettingsInputMode.current
    val localizedContext = LocalContext.current

    fun dismissAnimated() {
        if (dismissing) return
        dismissing = true
    }

    val dismissAction = remember { { dismissAnimated() } }

    LaunchedEffect(Unit) {
        overlayVisibility?.value = true
        visible = true
    }
    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(150)
            overlayVisibility?.value = false
            onDismiss()
        }
    }
    DisposableEffect(Unit) {
        onDispose { overlayVisibility?.value = false }
    }

    DisposableEffect(overlayBackHandlers, dismissAction) {
        overlayBackHandlers?.add(dismissAction)
        onDispose { overlayBackHandlers?.remove(dismissAction) }
    }

    Dialog(
        onDismissRequest = {
            if (onControllerBack?.invoke() != true) {
                dismissAnimated()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalContext provides localizedContext) {
            BackHandler(enabled = true) {
                if (latestControllerBack.value?.invoke() != true) {
                    dismissAnimated()
                }
            }

            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            val overlayView = LocalView.current
            DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                val decorView = window.decorView
                val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) hideDialogSystemBars(window)
                }
                decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
                val previousWindowCallback = window.callback
                var lastStickHorizontal = 0
                var lastStickVertical = 0
                fun axisDirection(value: Float): Int = when {
                    value <= -0.55f -> -1
                    value >= 0.55f -> 1
                    else -> 0
                }
                fun dispatchAxisKey(previous: Int, current: Int, negativeKey: Int, positiveKey: Int) {
                    if (previous == current) return
                    val now = SystemClock.uptimeMillis()
                    fun send(action: Int, keyCode: Int) {
                        previousWindowCallback?.dispatchKeyEvent(
                            android.view.KeyEvent(now, now, action, keyCode, 0)
                        )
                    }
                    if (previous < 0) send(android.view.KeyEvent.ACTION_UP, negativeKey)
                    if (previous > 0) send(android.view.KeyEvent.ACTION_UP, positiveKey)
                    if (current < 0) send(android.view.KeyEvent.ACTION_DOWN, negativeKey)
                    if (current > 0) send(android.view.KeyEvent.ACTION_DOWN, positiveKey)
                }
                val backKeyCallback = previousWindowCallback?.let { previous ->
                    object : Window.Callback by previous {
                                override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                                    val source = event.source
                                    val isGamepad = source and InputDevice.SOURCE_GAMEPAD != 0 ||
                                        source and InputDevice.SOURCE_JOYSTICK != 0
                                    if (isGamepad && event.action == android.view.KeyEvent.ACTION_DOWN &&
                                        event.repeatCount == 0
                                    ) {
                                        inputMode?.value = SettingsInputMode.GAMEPAD
                                        when (event.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP,
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        LauncherAudioManager.play(
                                            LauncherSound.CLICK,
                                            LauncherInputSource.GAMEPAD
                                        )
                                        LauncherAudioManager.performHapticFeedback(overlayView)
                                        if (latestControllerBack.value == null) {
                                            val direction = when (event.keyCode) {
                                                android.view.KeyEvent.KEYCODE_DPAD_UP -> Key.DirectionUp
                                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> Key.DirectionDown
                                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> Key.DirectionLeft
                                                else -> Key.DirectionRight
                                            }
                                            if (latestDirectionalKey.value?.invoke(direction) == true) {
                                                return true
                                            }
                                        }
                                    }
                                    android.view.KeyEvent.KEYCODE_BUTTON_B -> {
                                        LauncherAudioManager.play(
                                            LauncherSound.BACK,
                                            LauncherInputSource.GAMEPAD
                                        )
                                        LauncherAudioManager.performHapticFeedback(overlayView)
                                    }
                                }
                            }
                            if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                if (event.action == android.view.KeyEvent.ACTION_UP) {
                                    if (latestControllerBack.value?.invoke() != true) dismissAnimated()
                                }
                                return true
                            }
                            return previous.dispatchKeyEvent(event)
                        }

                        override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
                            val source = event.source
                            val isController =
                                (source and InputDevice.SOURCE_JOYSTICK) != 0 ||
                                    (source and InputDevice.SOURCE_GAMEPAD) != 0
                            if (!isController || event.action != MotionEvent.ACTION_MOVE) {
                                return previous.dispatchGenericMotionEvent(event)
                            }
                            val horizontal = axisDirection(
                                event.getAxisValue(MotionEvent.AXIS_HAT_X)
                                    .takeUnless { kotlin.math.abs(it) < 0.01f }
                                    ?: event.getAxisValue(MotionEvent.AXIS_X)
                            )
                            val vertical = axisDirection(
                                event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                                    .takeUnless { kotlin.math.abs(it) < 0.01f }
                                    ?: event.getAxisValue(MotionEvent.AXIS_Y)
                            )
                            val wasActive = lastStickHorizontal != 0 || lastStickVertical != 0
                            dispatchAxisKey(lastStickHorizontal, horizontal, android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT)
                            dispatchAxisKey(lastStickVertical, vertical, android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN)
                            lastStickHorizontal = horizontal
                            lastStickVertical = vertical
                            return horizontal != 0 || vertical != 0 || wasActive
                        }
                    }
                }
                if (backKeyCallback != null) window.callback = backKeyCallback
                var nativeBackCallback: android.window.OnBackInvokedCallback? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    nativeBackCallback = android.window.OnBackInvokedCallback {
                        if (onControllerBack?.invoke() != true) dismissAnimated()
                    }
                    window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                        nativeBackCallback!!
                    )
                }
                window.apply {
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(WindowManager::class.java)
                    if (manager?.isCrossWindowBlurEnabled == true) {
                        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        setBackgroundBlurRadius(12)
                        attributes = attributes.apply { blurBehindRadius = 22 }
                    } else {
                        clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    }
                }
                hideDialogSystemBars(this)
                }
                onDispose {
                    decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                    if (backKeyCallback != null && window.callback === backKeyCallback) {
                        window.callback = previousWindowCallback
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        nativeBackCallback?.let {
                            window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                        }
                    }
                }
            }
            }

            LaunchedEffect(window, onControllerBack) {
            overlayFocusRequester.requestFocus()
            if (onControllerBack == null) {
                delay(40)
                focusManager.moveFocus(FocusDirection.Enter)
            }
            }

            Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        inputMode?.value = SettingsInputMode.TOUCH
                        if (onControllerBack?.invoke() != true) dismissAnimated()
                    })
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        false
                    } else {
                        inputMode?.value = SettingsInputMode.GAMEPAD
                        if (HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode)) {
                            HorizonNavigation.requestHome()
                            true
                        } else if (event.key == Key.ButtonB || event.key == Key.Back) {
                        LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.GAMEPAD)
                        LauncherAudioManager.performHapticFeedback(overlayView)
                        if (onControllerBack?.invoke() != true) {
                            dismissAnimated()
                        }
                        true
                        } else if (onControllerBack == null) {
                        if (event.key == Key.DirectionUp || event.key == Key.DirectionDown ||
                            event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                        ) {
                            LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
                            LauncherAudioManager.performHapticFeedback(overlayView)
                        }
                        onDirectionalKey?.invoke(event.key) == true || when (event.key) {
                            Key.DirectionDown, Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Next)
                            Key.DirectionUp, Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Previous)
                            else -> false
                        }
                        } else {
                        false
                        }
                    }
                }
                .focusRequester(overlayFocusRequester)
                .focusable(),
            contentAlignment = Alignment.BottomCenter
            ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.825f)
            ) {
                AnimatedVisibility(
                    visible = visible && !dismissing,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(tween(180)) + slideInVertically(
                        animationSpec = tween(180),
                        initialOffsetY = { it }
                    ),
                    exit = fadeOut(tween(150)) + slideOutVertically(
                        animationSpec = tween(150),
                        targetOffsetY = { it }
                    )
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(SettingsOverlayPanel)
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    inputMode?.value = SettingsInputMode.TOUCH
                                })
                            }
                            .focusGroup()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.ButtonB || event.key == Key.Back)
                                ) {
                                    if (onControllerBack?.invoke() != true) dismissAnimated()
                                    true
                                } else false
                            },
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(35.dp)
                                .padding(horizontal = 62.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(title, color = SettingsWhite, fontSize = 22.sp)
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(1.dp)
                                .background(SettingsDivider)
                        )
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth(0.58f)
                                    .align(if (centered) Alignment.Center else Alignment.TopCenter)
                                    .then(
                                        if (centered) Modifier
                                        else Modifier.verticalScroll(scrollState ?: rememberScrollState())
                                    )
                                    .padding(top = 8.dp, bottom = 16.dp)
                            ) {
                                content()
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(1.dp)
                                .background(SettingsDivider)
                        )
                        HorizonOverlayFooter(
                            onBack = onFooterBack ?: { dismissAnimated() },
                            leading = footerAction
                        )
                    }
                }
            }
            }
        }
    }
}

private fun isExternalGamepadConnected(): Boolean {
    return InputDevice.getDeviceIds().any { deviceId ->
        val device = InputDevice.getDevice(deviceId) ?: return@any false
        val sources = device.sources
        device.isExternal &&
            !device.isVirtual &&
            (sources and InputDevice.SOURCE_GAMEPAD != 0 ||
                sources and InputDevice.SOURCE_JOYSTICK != 0)
    }
}

@Composable
private fun HorizonOverlayFooter(
    onBack: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    var gamepadConnected by remember { mutableStateOf(isExternalGamepadConnected()) }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            private fun refresh() {
                gamepadConnected = isExternalGamepadConnected()
            }

            override fun onInputDeviceAdded(deviceId: Int) = refresh()
            override fun onInputDeviceRemoved(deviceId: Int) = refresh()
            override fun onInputDeviceChanged(deviceId: Int) = refresh()
        }
        inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        gamepadConnected = isExternalGamepadConnected()
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .padding(start = 28.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (gamepadConnected) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(4) { index ->
                            Box(
                                Modifier
                                    .size(width = 7.dp, height = 6.dp)
                                    .background(if (index == 0) Color(0xFFB6E800) else Color(0xFF777777))
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Image(
                        painter = painterResource(R.drawable.game),
                        contentDescription = null,
                        modifier = Modifier.size(46.dp).padding(2.dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(SettingsWhite)
                    )
                }
            }
            if (leading != null) {
                Spacer(Modifier.width(20.dp))
                leading()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizonOverlayFooterButton(
                glyph = "B",
                label = stringResource(R.string.settings_action_back),
                onClick = onBack
            )
            Spacer(Modifier.width(25.dp))
            HorizonOverlayFooterButton(
                glyph = "A",
                label = stringResource(R.string.action_ok),
                onClick = {}
            )
        }
    }
}

@Composable
private fun HorizonOverlayFooterButton(
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizonButtonGlyph(
            label = glyph,
            size = 18.dp,
            fill = SettingsWhite,
            contentColor = SettingsOverlayPanel
        )
        Spacer(Modifier.width(7.dp))
        Text(label, color = SettingsWhite, fontSize = 16.sp)
    }
}

@Composable
internal fun HorizonOverlayFooterAction(
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    var hasFocus by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "overlayFooterActionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlayFooterActionPulseValue"
    )
    Row(
        Modifier
            .height(44.dp)
            .border(
                width = 2.dp,
                color = if (hasFocus) {
                    SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                } else {
                    SettingsDivider
                },
                shape = RoundedCornerShape(6.dp)
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .onFocusChanged { hasFocus = it.hasFocus }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SettingsWhite, fontSize = 16.sp)
    }
}

@Composable
internal fun HorizonOverlayChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    value: String = "",
    titleColor: Color? = null
) {
    var hasFocus by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val choiceFocusRequester = remember { FocusRequester() }
    val inputMode = LocalSettingsInputMode.current
    val localView = LocalView.current
    val pulse = rememberInfiniteTransition(label = "overlayChoiceSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlayChoiceSelectionPulseValue"
    )
    val active = inputMode?.value == SettingsInputMode.GAMEPAD && selected && enabled
    LaunchedEffect(hasFocus) {
        if (hasFocus) bringIntoViewRequester.bringIntoView()
    }
    LaunchedEffect(inputMode?.value, selected) {
        if (inputMode?.value == SettingsInputMode.GAMEPAD && selected && enabled) {
            choiceFocusRequester.requestFocus()
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (active) SettingsDivider.copy(alpha = 0.24f) else Color.Transparent)
            .then(
                if (active) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                    )
                } else Modifier
            )
            .clickable(enabled = enabled) {
                inputMode?.value = SettingsInputMode.TOUCH
                LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                LauncherAudioManager.performHapticFeedback(localView)
                onClick()
            }
            .onKeyEvent { event ->
                if (enabled && isHorizonConfirmKey(event)) {
                    inputMode?.value = SettingsInputMode.GAMEPAD
                    LauncherAudioManager.playConfirm(LauncherInputSource.GAMEPAD)
                    LauncherAudioManager.performHapticFeedback(localView)
                    onClick()
                    true
                } else false
            }
            .onFocusChanged { hasFocus = it.hasFocus }
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(choiceFocusRequester)
            .focusable(enabled)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = titleColor ?: if (enabled) SettingsWhite else SettingsGray, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (value.isNotEmpty()) Text(value, color = if (selected) SettingsBlue else SettingsGray, fontSize = 16.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
    }
}

@Composable
internal fun HorizonOverlayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
    placeholder: String = "",
    selected: Boolean = false,
    onEdit: (() -> Unit)? = null,
    autoEditOnSelection: Boolean = true,
    onPaste: (() -> Unit)? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val fieldFocusRequester = remember { FocusRequester() }
    val inputMode = LocalSettingsInputMode.current
    val context = LocalContext.current
    val pasteAction = rememberUpdatedState(
        onPaste ?: {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val pasted = clipboard?.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.replace('\r', ' ')
                ?.replace('\n', ' ')
                .orEmpty()
            if (pasted.isNotEmpty()) onValueChange(pasted)
        }
    )
    val pulse = rememberInfiniteTransition(label = "overlayFieldSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "overlayFieldSelectionPulseValue"
    )
    val active = selected
    LaunchedEffect(selected) {
        if (selected) {
            bringIntoViewRequester.bringIntoView()
            fieldFocusRequester.requestFocus()
            if (autoEditOnSelection) onEdit?.invoke()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) SettingsDivider.copy(alpha = 0.24f) else Color.Transparent)
            .then(
                if (active) Modifier.border(
                    width = 3.dp,
                    color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f),
                    shape = RoundedCornerShape(6.dp)
                ) else Modifier.border(1.dp, SettingsGray, RoundedCornerShape(6.dp))
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(fieldFocusRequester)
            .focusable()
            .horizonLongPress {
                pasteAction.value()
            }
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onEdit?.invoke()
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && isHorizonConfirmKey(event)) {
                    onEdit?.invoke()
                    true
                } else {
                    false
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        val shownValue = if (password) value.map { '•' }.joinToString("") else value
        Text(
            text = if (shownValue.isEmpty()) placeholder else shownValue,
            color = if (shownValue.isEmpty()) SettingsGray else SettingsWhite,
            fontSize = 17.sp,
            maxLines = 1
        )
    }
}
