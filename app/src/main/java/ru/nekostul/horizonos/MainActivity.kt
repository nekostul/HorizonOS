package ru.nekostul.horizonos

import android.os.Bundle
import android.view.KeyEvent
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
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.settings.LanguageManager

class MainActivity : ComponentActivity() {

    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Some gamepads expose B as BUTTON_B instead of forwarding Android
        // Back. Route it through the same callback stack used by overlays and
        // Settings, so it can never fall through to Activity.finish().
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runtimePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { }

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
            val localizedContext = remember(settings.language) {
                LanguageManager.localizedContext(this@MainActivity, settings.language)
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
            ) {
                HorizonOSTheme(darkTheme = settings.theme != "light") {
                    HorizonHome(
                        onRequestPermissions = { permissions ->
                            if (permissions.isNotEmpty()) {
                                runtimePermissionLauncher.launch(permissions)
                            }
                        }
                    )
                }
            }
        }
    }
}
