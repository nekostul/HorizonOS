package ru.nekostul.horizonos.ui.settings

import android.graphics.drawable.ColorDrawable
import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import ru.nekostul.horizonos.ui.isHorizonConfirmKey

internal val LocalSettingsOverlayVisible =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.MutableState<Boolean>?> { null }

internal val LocalSettingsOverlayBackHandlers =
    androidx.compose.runtime.compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateList<() -> Unit>?> { null }

@Suppress("DEPRECATION")
private fun hideDialogSystemBars(window: Window) {
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
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    val overlayFocusRequester = remember { FocusRequester() }
    val overlayVisibility = LocalSettingsOverlayVisible.current
    val overlayBackHandlers = LocalSettingsOverlayBackHandlers.current

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
        onDismissRequest = { /* Native Android Back is intentionally disabled. */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                val decorView = window.decorView
                val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) hideDialogSystemBars(window)
                }
                decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
                var nativeDialogBackCallback: android.window.OnBackInvokedCallback? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    nativeDialogBackCallback = android.window.OnBackInvokedCallback { }
                    window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                        nativeDialogBackCallback!!
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
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        nativeDialogBackCallback?.let { window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
                    }
                }
            }
        }

        LaunchedEffect(window) {
            overlayFocusRequester.requestFocus()
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(onClick = { dismissAnimated() })
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.ButtonB
                    ) {
                        dismissAnimated()
                        true
                    } else false
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
                            // Consume taps inside the panel so only the scrim
                            // closes the overlay.
                            .clickable(onClick = {})
                            .focusGroup(),
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
                                    .align(Alignment.TopCenter)
                                    .verticalScroll(rememberScrollState())
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
                        HorizonOverlayFooter(onBack = { dismissAnimated() })
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
private fun HorizonOverlayFooter(onBack: () -> Unit) {
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
        Box(
            Modifier
                .padding(start = 28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
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
internal fun HorizonOverlayChoice(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    value: String = ""
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .onKeyEvent { event ->
                if (enabled && isHorizonConfirmKey(event)) {
                    onClick()
                    true
                } else false
            }
            .focusable(enabled)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = if (enabled) SettingsWhite else SettingsGray, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            if (value.isNotEmpty()) Text(value, color = if (selected) SettingsBlue else SettingsGray, fontSize = 16.sp)
            if (selected) {
                Box(
                    Modifier
                        .size(26.dp)
                        .background(SettingsBlue, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = SettingsBackground, fontSize = 18.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
    }
}

@Composable
internal fun HorizonOverlayTextField(
    value: String,
    onValueChange: (String) -> Unit,
    password: Boolean = false,
    placeholder: String = ""
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = SettingsWhite, fontSize = 17.sp),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .background(SettingsBackground, RoundedCornerShape(6.dp))
            .border(1.dp, SettingsGray, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, color = SettingsGray, fontSize = 17.sp)
                }
                innerTextField()
            }
        }
    )
}
