package ru.nekostul.horizonos

import android.os.Bundle
import android.os.Build
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

    private lateinit var runtimePermissionLauncher: ActivityResultLauncher<Array<String>>

    private var nativeBackCallback: android.window.OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // HorizonOS uses the controller B button for navigation. Consume the
        // platform Back gesture/button so it can never finish the launcher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nativeBackCallback = android.window.OnBackInvokedCallback { }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                nativeBackCallback!!
            )
        }

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
        // Intentionally disabled. Navigation is controller-first.
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            nativeBackCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
        }
        super.onDestroy()
    }
}
