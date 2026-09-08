package ru.nekostul.horizonos.ui.settings

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

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
    val scale by animateFloatAsState(
        targetValue = if (visible && !dismissing) 1f else 0.96f,
        animationSpec = tween(150),
        label = "horizon_overlay_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible && !dismissing) 1f else 0f,
        animationSpec = tween(150),
        label = "horizon_overlay_alpha"
    )
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

    // Dialog is a separate window. Consume Android Back here so it closes the
    // current HorizonOS overlay instead of finishing the Activity.
    BackHandler(enabled = true) {
        dismissAnimated()
    }

    Dialog(
        onDismissRequest = { dismissAnimated() },
        properties = DialogProperties(
            dismissOnBackPress = true,
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
                }
            }
        }

        LaunchedEffect(window) {
            overlayFocusRequester.requestFocus()
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Back ||
                            event.key == Key.Escape ||
                            event.key == Key.ButtonB)
                    ) {
                        dismissAnimated()
                        true
                    } else false
                }
                .focusRequester(overlayFocusRequester)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = alpha > 0f,
                enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.96f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.96f)
            ) {
                Column(
                    Modifier
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .fillMaxWidth(0.72f)
                        .widthIn(min = 360.dp, max = 760.dp)
                        .heightIn(max = 760.dp)
                        .verticalScroll(rememberScrollState())
                        .background(SettingsPanel, RoundedCornerShape(12.dp))
                        .border(1.dp, SettingsBlue, RoundedCornerShape(12.dp))
                        .padding(24.dp)
                        .focusGroup(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, color = SettingsWhite, fontSize = 24.sp)
                    Spacer(Modifier.height(18.dp))
                    content()
                }
            }
        }
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
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) SettingsSelected else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .onKeyEvent { event ->
                if (enabled && event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .focusable(enabled)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = if (enabled) SettingsWhite else SettingsGray, fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, color = if (selected) SettingsBlue else SettingsGray, fontSize = 16.sp)
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
