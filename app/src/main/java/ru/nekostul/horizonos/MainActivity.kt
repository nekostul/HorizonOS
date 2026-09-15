package ru.nekostul.horizonos

import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.nekostul.horizonos.ui.home.HorizonHome
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.lockscreen.HorizonLock
import ru.nekostul.horizonos.ui.lockscreen.UnlockScreen
import ru.nekostul.horizonos.ui.theme.HorizonOSTheme
import android.content.pm.ActivityInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.settings.LanguageManager
import ru.nekostul.horizonos.ui.home.HorizonStartupAnimation
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var lastStickHorizontal = 0
    private var lastStickVertical = 0

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val source = event.source
        val isController =
            (source and InputDevice.SOURCE_JOYSTICK) != 0 ||
                (source and InputDevice.SOURCE_GAMEPAD) != 0
        if (!isController || event.action != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event)
        }

        val horizontal = controllerAxisDirection(
            event.getAxisValue(MotionEvent.AXIS_HAT_X)
                .takeUnless { kotlin.math.abs(it) < 0.01f }
                ?: event.getAxisValue(MotionEvent.AXIS_X)
        )
        val vertical = controllerAxisDirection(
            event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                .takeUnless { kotlin.math.abs(it) < 0.01f }
                ?: event.getAxisValue(MotionEvent.AXIS_Y)
        )

        val wasActive = lastStickHorizontal != 0 || lastStickVertical != 0
        dispatchAxisKey(lastStickHorizontal, horizontal, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
        dispatchAxisKey(lastStickVertical, vertical, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)
        lastStickHorizontal = horizontal
        lastStickVertical = vertical
        return horizontal != 0 || vertical != 0 || wasActive
    }

    private fun dispatchAxisKey(previous: Int, current: Int, negativeKey: Int, positiveKey: Int) {
        if (previous == current) return
        val now = SystemClock.uptimeMillis()
        fun send(action: Int, keyCode: Int) {
            dispatchKeyEvent(KeyEvent(now, now, action, keyCode, 0))
        }
        if (previous < 0) send(KeyEvent.ACTION_UP, negativeKey)
        if (previous > 0) send(KeyEvent.ACTION_UP, positiveKey)
        if (current < 0) send(KeyEvent.ACTION_DOWN, negativeKey)
        if (current > 0) send(KeyEvent.ACTION_DOWN, positiveKey)
    }

    private fun controllerAxisDirection(value: Float): Int = when {
        value <= -0.55f -> -1
        value >= 0.55f -> 1
        else -> 0
    }

    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {

            hide(
                WindowInsetsCompat.Type.statusBars() or
                        WindowInsetsCompat.Type.navigationBars()
            )

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val repository = remember { LauncherSettingsRepository(this@MainActivity) }
            val settings by repository.settings.collectAsState(initial = LauncherSettings())
            val locked by HorizonLock.locked.collectAsState()
            var showStartupAnimation by remember { mutableStateOf(true) }
            var unlocking by remember { mutableStateOf(false) }
            val unlockProgress by animateFloatAsState(
                targetValue = if (unlocking) 1f else 0f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "unlockProgress"
            )
            LaunchedEffect(unlockProgress, unlocking) {
                if (unlocking && unlockProgress >= 1f) {
                    HorizonLock.unlock()
                    unlocking = false
                    showStartupAnimation = false
                }
            }
            val localizedContext = remember(settings.language) {
                LanguageManager.localizedContext(this@MainActivity, settings.language)
            }
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                            HorizonLock.lock()
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
                ContextCompat.registerReceiver(
                    this@MainActivity,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                onDispose { this@MainActivity.unregisterReceiver(receiver) }
            }
            LaunchedEffect(Unit) {
                ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
                    .init(this@MainActivity)
            }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    runtimePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }
            LaunchedEffect(Unit) {
                val legacy = ru.nekostul.horizonos.ui.files.StorageAccess
                    .missingLegacyPermissions(this@MainActivity)
                if (legacy.isNotEmpty()) {
                    runtimePermissionLauncher.launch(legacy)
                }
                if (!ru.nekostul.horizonos.ui.files.StorageAccess.hasAllFilesAccess()) {
                    ru.nekostul.horizonos.ui.files.StorageAccess
                        .requestAllFilesAccess(this@MainActivity)
                }
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
            ) {
                HorizonOSTheme(darkTheme = settings.theme != "light") {
                    LaunchedEffect(Unit) {
                        delay(1_320)
                        showStartupAnimation = false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LocalHorizonColors.current.background)
                    ) {
                        val blurRadius = if (locked) 20f * (1f - unlockProgress) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (blurRadius > 0.4f) Modifier.blur(blurRadius.dp)
                                    else Modifier
                                )
                        ) {
                            if (showStartupAnimation && !locked) {
                                HorizonStartupAnimation()
                            } else {
                                HorizonHome(
                                    onRequestPermissions = { permissions ->
                                        if (permissions.isNotEmpty()) {
                                            runtimePermissionLauncher.launch(permissions)
                                        }
                                    }
                                )
                            }
                        }

                        if (locked) {
                            UnlockScreen(
                                unlockProgress = unlockProgress,
                                onUnlockStart = { unlocking = true },
                                pressRequired = if (settings.lockScreenEnabled) 3 else 1
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            HorizonNavigation.isHomeKeyCode(event.keyCode)
        ) {
            if (!HorizonLock.locked.value) {
                HorizonNavigation.requestHome()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
