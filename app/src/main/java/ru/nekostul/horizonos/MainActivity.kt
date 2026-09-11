package ru.nekostul.horizonos

import android.os.Bundle
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.nekostul.horizonos.ui.home.HorizonHome
import ru.nekostul.horizonos.ui.theme.HorizonOSTheme
import android.content.pm.ActivityInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.settings.LanguageManager
import ru.nekostul.horizonos.ui.home.HorizonStartupAnimation
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
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
    private lateinit var gameFolderLauncher: ActivityResultLauncher<Uri?>
    private var pendingGameFolderResult: ((Uri?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }
        gameFolderLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            pendingGameFolderResult?.invoke(uri)
            pendingGameFolderResult = null
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        WindowCompat.setDecorFitsSystemWindows(window, false)

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
            var showStartupAnimation by remember { mutableStateOf(true) }
            val localizedContext = remember(settings.language) {
                LanguageManager.localizedContext(this@MainActivity, settings.language)
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
                        if (showStartupAnimation) {
                            HorizonStartupAnimation()
                        } else {
                            HorizonHome(
                                onRequestPermissions = { permissions ->
                                    if (permissions.isNotEmpty()) {
                                        runtimePermissionLauncher.launch(permissions)
                                    }
                                },
                                onOpenGameFolder = { callback ->
                                    pendingGameFolderResult = callback
                                    gameFolderLauncher.launch(null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Launcher navigation is handled by the visible overlay or controller.
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
