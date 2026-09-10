package ru.nekostul.horizonos

import android.os.Bundle
import android.os.Build
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
