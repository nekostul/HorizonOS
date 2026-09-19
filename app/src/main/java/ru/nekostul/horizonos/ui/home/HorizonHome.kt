package ru.nekostul.horizonos.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.graphics.BitmapFactory
import android.view.InputDevice
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import ru.nekostul.horizonos.R
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.animation.core.animateFloat
import ru.nekostul.horizonos.ui.settings.LauncherSettingsScreen
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.user.UserPageScreen
import ru.nekostul.horizonos.ui.user.UserProfileRepository
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import ru.nekostul.horizonos.ui.games.GameLauncher
import ru.nekostul.horizonos.ui.games.GameLibrary
import ru.nekostul.horizonos.ui.games.DownloadTracker
import ru.nekostul.horizonos.ui.games.GamesScreen
import ru.nekostul.horizonos.ui.games.FolderRescan
import ru.nekostul.horizonos.ui.games.NewGamesNotifier
import ru.nekostul.horizonos.ui.games.NewGamesAddedOverlay
import ru.nekostul.horizonos.ui.lockscreen.HorizonLock
import ru.nekostul.horizonos.ui.home.status.StatusAirplaneIcon
import ru.nekostul.horizonos.ui.home.status.StatusBatteryIcon
import ru.nekostul.horizonos.ui.home.status.StatusClock
import ru.nekostul.horizonos.ui.home.status.StatusWifiIcon
import ru.nekostul.horizonos.ui.home.status.batteryBarColor
import ru.nekostul.horizonos.ui.home.status.rememberAirplaneModeEnabled
import ru.nekostul.horizonos.ui.home.status.rememberBatteryState
import ru.nekostul.horizonos.ui.home.status.rememberWifiEnabled
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val HorizonBackground: Color
    @Composable get() = LocalHorizonColors.current.background
private val HorizonBlue: Color
    @Composable get() = LocalHorizonColors.current.accent
private val HorizonWhite: Color
    @Composable get() = LocalHorizonColors.current.text
private val HorizonGray: Color
    @Composable get() = LocalHorizonColors.current.mutedText
private const val HomeCardSlotCount = 12

private val GameSirPackages = listOf(
    "com.xiaoji.xtouch.google",
    "com.xiaoji.gamemiracle",
    "com.gamesir.virtualtouchutil",
    "com.gamesir"
)
private const val GameSirPlayPackage = "com.xiaoji.xtouch.google"

private object BackdropCache {
    // The backdrop is intentionally prepared at a smaller size: it is blurred and
    // shown behind the UI, so extra source pixels only increase upload and GPU cost.
    private const val MAX_DIMENSION = 720
    // The source is later enlarged to the display, so a small source-space
    // radius matches the former 7.dp runtime blur instead of over-softening it.
    private const val BLUR_RADIUS = 3

    private val cache = android.util.LruCache<String, ImageBitmap>(3)

    fun get(path: String): ImageBitmap? = cache.get(path)

    fun load(path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        val bitmap = decodeSampled(path) ?: return null
        cache.put(path, bitmap)
        return bitmap
    }

    private fun decodeSampled(path: String): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        val blurred = blur(bitmap, BLUR_RADIUS)
        if (blurred !== bitmap) bitmap.recycle()
        return blurred.asImageBitmap()
    }

    /** Two-pass box blur; it runs once on Dispatchers.IO and keeps every frame cheap. */
    private fun blur(source: android.graphics.Bitmap, radius: Int): android.graphics.Bitmap {
        if (radius <= 0) return source

        val width = source.width
        val height = source.height
        val input = IntArray(width * height)
        val horizontal = IntArray(width * height)
        val output = IntArray(width * height)
        source.getPixels(input, 0, width, 0, 0, width, height)

        val diameter = radius * 2 + 1
        for (y in 0 until height) {
            var red = 0
            var green = 0
            var blue = 0
            var alpha = 0
            for (offset in -radius..radius) {
                val x = offset.coerceIn(0, width - 1)
                val pixel = input[y * width + x]
                red += pixel shr 16 and 0xFF
                green += pixel shr 8 and 0xFF
                blue += pixel and 0xFF
                alpha += pixel ushr 24
            }
            for (x in 0 until width) {
                horizontal[y * width + x] =
                    (alpha / diameter shl 24) or
                        (red / diameter shl 16) or
                        (green / diameter shl 8) or
                        (blue / diameter)

                val removeX = (x - radius).coerceAtLeast(0)
                val addX = (x + radius + 1).coerceAtMost(width - 1)
                val remove = input[y * width + removeX]
                val add = input[y * width + addX]
                red += (add shr 16 and 0xFF) - (remove shr 16 and 0xFF)
                green += (add shr 8 and 0xFF) - (remove shr 8 and 0xFF)
                blue += (add and 0xFF) - (remove and 0xFF)
                alpha += (add ushr 24) - (remove ushr 24)
            }
        }

        for (x in 0 until width) {
            var red = 0
            var green = 0
            var blue = 0
            var alpha = 0
            for (offset in -radius..radius) {
                val y = offset.coerceIn(0, height - 1)
                val pixel = horizontal[y * width + x]
                red += pixel shr 16 and 0xFF
                green += pixel shr 8 and 0xFF
                blue += pixel and 0xFF
                alpha += pixel ushr 24
            }
            for (y in 0 until height) {
                output[y * width + x] =
                    (alpha / diameter shl 24) or
                        (red / diameter shl 16) or
                        (green / diameter shl 8) or
                        (blue / diameter)

                val removeY = (y - radius).coerceAtLeast(0)
                val addY = (y + radius + 1).coerceAtMost(height - 1)
                val remove = horizontal[removeY * width + x]
                val add = horizontal[addY * width + x]
                red += (add shr 16 and 0xFF) - (remove shr 16 and 0xFF)
                green += (add shr 8 and 0xFF) - (remove shr 8 and 0xFF)
                blue += (add and 0xFF) - (remove and 0xFF)
                alpha += (add ushr 24) - (remove ushr 24)
            }
        }

        return android.graphics.Bitmap.createBitmap(
            width,
            height,
            android.graphics.Bitmap.Config.ARGB_8888
        ).apply {
            setPixels(output, 0, width, 0, 0, width, height)
        }
    }
}

private fun launchStaggerProgress(
    totalProgress: Float,
    index: Int,
    firstDelay: Float,
    stagger: Float,
    duration: Float
): Float {
    return ((totalProgress - firstDelay - index * stagger) / duration)
        .coerceIn(0f, 1f)
}

private fun easeOutBack(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = x - 1f
    return 1f + c3 * u * u * u + c1 * u * u
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
private fun ExternalGamepadConnected(): Boolean {
    val context = LocalContext.current
    val homeView = androidx.compose.ui.platform.LocalView.current
    var connected by remember { mutableStateOf(isExternalGamepadConnected()) }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            private fun refresh() {
                connected = isExternalGamepadConnected()
            }

            override fun onInputDeviceAdded(deviceId: Int) = refresh()

            override fun onInputDeviceRemoved(deviceId: Int) = refresh()

            override fun onInputDeviceChanged(deviceId: Int) = refresh()
        }

        inputManager.registerInputDeviceListener(
            listener,
            Handler(Looper.getMainLooper())
        )
        connected = isExternalGamepadConnected()

        onDispose {
            inputManager.unregisterInputDeviceListener(listener)
        }
    }

    return connected
}

@Composable
fun HorizonHome(
    onRequestPermissions: (Array<String>) -> Unit = {}
) {

    val context = LocalContext.current
    val homeView = androidx.compose.ui.platform.LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val homeFocusRequester = remember { FocusRequester() }

    val battery = rememberBatteryState()
    val wifiEnabled = rememberWifiEnabled()
    val airplaneEnabled = rememberAirplaneModeEnabled()
    val externalGamepadConnected = ExternalGamepadConnected()
    var homeEntryTarget by remember { mutableFloatStateOf(0f) }
    val homeEntryProgress by animateFloatAsState(
        targetValue = homeEntryTarget,
        animationSpec = tween(920, delayMillis = 35, easing = FastOutSlowInEasing),
        label = "homeEntryProgress"
    )

    LaunchedEffect("home-entry") {
        withFrameNanos { }
        homeEntryTarget = 1f
    }

    val gameLibrary = remember { GameLibrary(context) }
    val games by gameLibrary.games.collectAsState(initial = emptyList())
    val launcherSettingsRepository = remember { LauncherSettingsRepository(context) }
    val launcherSettings by launcherSettingsRepository.settings.collectAsState(initial = LauncherSettings())
    val visibleGames = games.filterNot { it.hidden }
    val downloadingApps by DownloadTracker.downloading.collectAsState()
    val downloadingGames = downloadingApps.map { app ->
        Game.fromAndroidApp(app.label, app.packageName, null, app.iconPath)
    }
    val displayGames = visibleGames + downloadingGames
    val downloadingProgress = downloadingApps.associate { it.packageName to it.progress }
    val gameLauncher = remember { GameLauncher() }

    val slotCount = maxOf(HomeCardSlotCount, displayGames.size)

    var selectedGame by remember {
        mutableIntStateOf(0)
    }

    var homeScrollPositionPx by remember {
        mutableFloatStateOf(0f)
    }

    var tappedGameIndex by remember {
        mutableIntStateOf(0)
    }

    var backgroundGameIndex by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(tappedGameIndex) {
        if (tappedGameIndex >= 0) backgroundGameIndex = tappedGameIndex
    }

    var gamepadNavigationRequest by remember {
        mutableIntStateOf(0)
    }

    var gameSelectionRevision by remember {
        mutableIntStateOf(0)
    }

    var selectedMenu by remember {
        mutableIntStateOf(0)
    }

    var menuSelectionArmed by remember {
        mutableStateOf(false)
    }

    var menuOpeningIndex by remember {
        mutableIntStateOf(-1)
    }

    var menuOpeningJob by remember {
        mutableStateOf<Job?>(null)
    }

    var gamesirMissing by remember { mutableStateOf(false) }
    var gamesirChoice by remember { mutableIntStateOf(0) }

    var showLauncherSettings by remember {
        mutableStateOf(false)
    }

    var showGames by remember {
        mutableStateOf(false)
    }

    var showFiles by remember {
        mutableStateOf(false)
    }

    var showApps by remember {
        mutableStateOf(false)
    }

    var showUserPage by remember {
        mutableStateOf(false)
    }

    var profileFocused by remember {
        mutableStateOf(false)
    }

    val userProfileRepository = remember { UserProfileRepository(context) }
    val userProfile by userProfileRepository.profile.collectAsState()

    val homeRequests by HorizonNavigation.requests.collectAsState()
    LaunchedEffect(homeRequests) {
        if (homeRequests > 0) {
            showGames = false
            showFiles = false
            showApps = false
            showLauncherSettings = false
            showUserPage = false
            profileFocused = false
        }
    }

    var launchGhost by remember {
        mutableStateOf<LaunchGhostData?>(null)
    }

    var poweringOff by remember { mutableStateOf(false) }
    val powerOffProgress = remember { Animatable(0f) }
    LaunchedEffect(poweringOff) {
        if (!poweringOff) return@LaunchedEffect
        powerOffProgress.snapTo(0f)
        powerOffProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        withContext(Dispatchers.IO) { PowerController.turnOffScreen(context) }
        delay(500)
        powerOffProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )
        poweringOff = false
    }

    val scanning by ScanCoordinator.scanning.collectAsState()
    val scanProgress by ScanCoordinator.progress.collectAsState()
    val scanHint by ScanCoordinator.hint.collectAsState()

    val locked by HorizonLock.locked.collectAsState()

    val homeLaunchZoom by animateFloatAsState(
        targetValue = if (launchGhost == null) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "homeLaunchZoom"
    )

    fun cancelMenuOpening() {
        menuOpeningJob?.cancel()
        menuOpeningJob = null
        menuOpeningIndex = -1
    }

    fun clearHomeSelection() {
        cancelMenuOpening()
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = -1
    }

    fun moveGameSelection(direction: Int) {
        cancelMenuOpening()
        selectedGame = (selectedGame + direction + slotCount) % slotCount
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = selectedGame
        gamepadNavigationRequest++
        gameSelectionRevision++
        LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        LauncherAudioManager.performHapticFeedback(homeView)
    }

    fun moveMenuSelection(direction: Int) {
        cancelMenuOpening()
        selectedMenu = (selectedMenu + direction + 7) % 7
        menuSelectionArmed = true
        tappedGameIndex = -1
        LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        LauncherAudioManager.performHapticFeedback(homeView)
    }

    fun moveDownToMenu() {
        cancelMenuOpening()
        selectedMenu = 0
        menuSelectionArmed = true
        tappedGameIndex = -1
        LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        LauncherAudioManager.performHapticFeedback(homeView)
    }

    fun moveUpToGames() {
        cancelMenuOpening()
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = selectedGame
        LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        LauncherAudioManager.performHapticFeedback(homeView)
    }

    fun openGamesir(source: LauncherInputSource = LauncherInputSource.TOUCH) {
        val launchIntent = GameSirPackages.firstNotNullOfOrNull { packageName ->
            runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
        }
        if (launchIntent == null) {
            gamesirChoice = 0
            gamesirMissing = true
            LauncherAudioManager.playHint(source)
            return
        }
        runCatching { context.startActivity(launchIntent) }
            .onFailure {
                gamesirChoice = 0
                gamesirMissing = true
                LauncherAudioManager.playHint(source)
            }
    }

    fun openGooglePlay(source: LauncherInputSource = LauncherInputSource.TOUCH) {
        LauncherAudioManager.performHapticFeedback(homeView)
        val launchIntent = runCatching {
            context.packageManager.getLaunchIntentForPackage("com.android.vending")
        }.getOrNull()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                context.startActivity(web)
            }
        }.recoverCatching { context.startActivity(web) }
            .onFailure {
                LauncherAudioManager.playHint(source)
            }
    }

    fun openGamesirStore() {
        gamesirMissing = false
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$GameSirPlayPackage")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$GameSirPlayPackage")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }
            .recoverCatching { context.startActivity(web) }
    }

    fun activateMenu(
        index: Int,
        source: LauncherInputSource = LauncherInputSource.TOUCH
    ) {

        if (menuOpeningIndex >= 0) return

        if (selectedMenu != index || !menuSelectionArmed) {
            LauncherAudioManager.play(LauncherSound.CLICK, source)
            LauncherAudioManager.performHapticFeedback(homeView)
            selectedMenu = index
            menuSelectionArmed = true
            tappedGameIndex = -1
            return
        }

        menuOpeningIndex = index
        menuOpeningJob = coroutineScope.launch {
            delay(660)
            if (menuOpeningIndex != index) return@launch
            menuSelectionArmed = false
            menuOpeningIndex = -1
            menuOpeningJob = null

            when (index) {

                0 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    openGooglePlay(source)
                }

                1 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    showGames = true
                }

                2 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    showFiles = true
                }

                3 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    showApps = true
                }

                4 -> {
                    LauncherAudioManager.performHapticFeedback(homeView)
                    openGamesir(source)
                }

                5 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    showLauncherSettings = true
                }

                6 -> {
                    LauncherAudioManager.playConfirm(source)
                    LauncherAudioManager.performHapticFeedback(homeView)
                    poweringOff = true
                }
            }
        }
    }

fun launchGame(game: Game, source: LauncherInputSource) {
        when (val result = gameLauncher.launch(context, game)) {
            GameLaunchResult.Launched -> LauncherAudioManager.playOpenGame(source)
            is GameLaunchResult.Failed -> {
                LauncherAudioManager.playGameError(source)
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun launchGameWithAnimation(
        game: Game,
        source: LauncherInputSource = LauncherInputSource.TOUCH
    ) {
        if (launchGhost != null) return
        LauncherAudioManager.playLaunchVibration()
        coroutineScope.launch {
            val image = withContext(Dispatchers.IO) {
                val path = game.coverPath ?: game.iconPath
                if (path.isNullOrBlank()) {
                    null
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    } else null
                }
            }
            launchGhost = LaunchGhostData(
                id = System.nanoTime(),
                image = image,
                game = game,
                source = source
            )
        }
    }

    fun enqueueAutoScan(added: List<Game>) {
        ScanCoordinator.init(context)
        ScanCoordinator.consumeHint()
        ScanCoordinator.enqueue(added)
    }

    fun selectGame(index: Int) {
        val wasSelected = selectedGame == index && tappedGameIndex == index
        if (!wasSelected) {
            LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.TOUCH)
        }
        LauncherAudioManager.performHapticFeedback(homeView)
        selectedGame = index
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = index
        if (!wasSelected) {
            gameSelectionRevision++
        }

        if (wasSelected) {
            visibleGames.getOrNull(index)?.let {
                launchGameWithAnimation(it, LauncherInputSource.TOUCH)
            }
        }
    }

    LaunchedEffect(Unit) {
        selectedGame = 0
        homeScrollPositionPx = 0f
        tappedGameIndex = 0
    }

    LaunchedEffect(Unit) {
        if (!FolderRescan.claim()) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) { FolderRescan.rescan(context) }
        if (result.added.isNotEmpty()) {
            ScanCoordinator.init(context)
            ScanCoordinator.enqueue(result.added)
            NewGamesNotifier.publish(result.platforms)
        }
    }

    LaunchedEffect(selectedGame, visibleGames, launcherSettings.screenshotBackgroundEnabled) {
        if (!launcherSettings.screenshotBackgroundEnabled) return@LaunchedEffect
        val paths = listOf(-1, 0, 1, 2)
            .mapNotNull { offset -> visibleGames.getOrNull(selectedGame + offset)?.screenshotPath }
        withContext(Dispatchers.IO) { paths.forEach { BackdropCache.load(it) } }
    }

    val newGamePlatforms by NewGamesNotifier.platforms.collectAsState()
    LaunchedEffect(newGamePlatforms) {
        if (newGamePlatforms.isNotEmpty()) {
            LauncherAudioManager.playHint(LauncherInputSource.TOUCH)
        }
    }
    if (newGamePlatforms.isNotEmpty()) {
        NewGamesAddedOverlay(
            platforms = newGamePlatforms,
            onDismiss = { NewGamesNotifier.consume() }
        )
    }

    if (gamesirMissing) {
        val gamesirInputMode = remember { mutableStateOf(SettingsInputMode.GAMEPAD) }
        CompositionLocalProvider(LocalSettingsInputMode provides gamesirInputMode) {
            HorizonOverlay(
                title = stringResource(R.string.home_gamesir_missing_title),
                onDismiss = { gamesirMissing = false },
                onDirectionalKey = { key ->
                    when (key) {
                        Key.DirectionDown, Key.DirectionRight -> {
                            gamesirChoice = (gamesirChoice + 1).coerceAtMost(1)
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            gamesirChoice = (gamesirChoice - 1).coerceAtLeast(0)
                            true
                        }
                        else -> false
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.home_gamesir_missing_message),
                    color = SettingsWhite,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
                HorizonOverlayChoice(
                    title = stringResource(R.string.home_gamesir_download),
                    selected = gamesirChoice == 0,
                    onClick = { openGamesirStore() }
                )
                HorizonOverlayChoice(
                    title = stringResource(R.string.home_gamesir_close),
                    selected = gamesirChoice == 1,
                    onClick = { gamesirMissing = false }
                )
            }
        }
    }

    if (showUserPage) {
        UserPageScreen(
            repository = userProfileRepository,
            onBack = {
                showUserPage = false
                clearHomeSelection()
            }
        )
        return
    }

    if (showGames) {
        GamesScreen(
            onDismiss = {
                showGames = false
                clearHomeSelection()
            },
            onGamesAdded = { added -> enqueueAutoScan(added) }
        )
        return
    }

    if (showFiles) {
        ru.nekostul.horizonos.ui.files.FilesScreen(
            onDismiss = {
                showFiles = false
                clearHomeSelection()
            }
        )
        return
    }

    if (showLauncherSettings) {
        LauncherSettingsScreen(
            onBack = {
                showLauncherSettings = false
                clearHomeSelection()
            },
            onRequestPermissions = onRequestPermissions
        )
        return
    }

    if (showApps) {
        ru.nekostul.horizonos.ui.apps.AndroidAppsScreen(
            onDismiss = {
                showApps = false
                clearHomeSelection()
            }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HorizonBackground)
            .graphicsLayer {
                val zoom = FastOutSlowInEasing.transform(homeLaunchZoom)
                val powerZoom = powerOffProgress.value
                val base = 1f + 0.085f * zoom
                scaleX = base * (1f - 0.06f * powerZoom)
                scaleY = base * (1f - 0.06f * powerZoom)
                alpha = (1f - 0.45f * zoom) * (1f - 0.15f * powerZoom)
            }
            .focusRequester(homeFocusRequester)
            .focusProperties { canFocus = !locked }
            .focusable()
            .onPreviewKeyEvent { event ->

                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                if (profileFocused) {
                    when {
                        isHorizonConfirmKey(event) -> {
                            LauncherAudioManager.playConfirm(LauncherInputSource.GAMEPAD)
                            LauncherAudioManager.performHapticFeedback(homeView)
                            profileFocused = false
                            showUserPage = true
                        }
                        event.key == Key.DirectionDown ||
                            event.key == Key.ButtonB ||
                            event.key == Key.Back -> {
                            LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.GAMEPAD)
                            LauncherAudioManager.performHapticFeedback(homeView)
                            profileFocused = false
                            tappedGameIndex = selectedGame
                        }
                    }
                    return@onPreviewKeyEvent true
                }

                if (isHorizonConfirmKey(event)) {
                    if (menuSelectionArmed && selectedMenu >= 0) {
                        activateMenu(selectedMenu, LauncherInputSource.GAMEPAD)
                    } else {
                        LauncherAudioManager.performHapticFeedback(homeView)
                        visibleGames.getOrNull(selectedGame)?.let {
                            launchGameWithAnimation(it, LauncherInputSource.GAMEPAD)
                        }
                    }
                    return@onPreviewKeyEvent true
                }

                when (event.key) {

                    Key.DirectionLeft -> {
                        if (menuSelectionArmed) {
                            moveMenuSelection(-1)
                        } else {
                            moveGameSelection(-1)
                        }
                        true
                    }

                    Key.DirectionRight -> {
                        if (menuSelectionArmed) {
                            moveMenuSelection(1)
                        } else {
                            moveGameSelection(1)
                        }
                        true
                    }

                    Key.DirectionDown -> {
                        moveDownToMenu()
                        true
                    }

                    Key.DirectionUp -> {
                        if (menuSelectionArmed) {
                            moveUpToGames()
                        } else {
                            clearHomeSelection()
                            profileFocused = true
                            LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
                            LauncherAudioManager.performHapticFeedback(homeView)
                        }
                        true
                    }

                    else -> false
                }
            }
    ) {

        val h = maxHeight
        val cardSize = h * 0.364f
        val cardGap = h * 0.018f
        val cardStartOffset = h * 0.114f

        if (launcherSettings.screenshotBackgroundEnabled) {
            GameScreenshotBackground(
                screenshotPath = visibleGames.getOrNull(backgroundGameIndex)?.screenshotPath
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(h * 0.042f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(h * 0.083f),
                contentAlignment = Alignment.Center
            ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val entry = launchStaggerProgress(
                            homeEntryProgress,
                            index = 0,
                            firstDelay = 0.02f,
                            stagger = 0f,
                            duration = 0.20f
                        )
                        alpha = entry
                        translationY = -18.dp.toPx() * (1f - entry)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    HomeProfileButton(
                        avatarPath = userProfile.avatarPath,
                        size = h * 0.082f,
                        focused = profileFocused,
                        onClick = {
                            profileFocused = false
                            LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                            showUserPage = true
                        }
                    )
                    if (profileFocused) {
                        Text(
                            text = stringResource(R.string.user_page_title),
                            color = HorizonBlue,
                            fontSize = 18.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = h * 0.082f + 4.dp)
                                .wrapContentWidth(unbounded = true)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(h * 0.014f)
                ) {
                    StatusClock(
                        color = HorizonWhite,
                        fontSize = (h.value * 0.042f).sp
                    )
                    if (airplaneEnabled) {
                        StatusAirplaneIcon(color = HorizonWhite, size = h * 0.030f)
                    }
                    if (wifiEnabled) {
                        StatusWifiIcon(color = HorizonWhite, size = h * 0.032f)
                    }
                    StatusBatteryIcon(
                        level = battery.level,
                        fillColor = batteryBarColor(battery),
                        height = h * 0.030f - 1.dp,
                        percentageFontSize = (h.value * 0.039f).sp,
                        modifier = Modifier.offset(y = (0).dp)
                    )
                }
            }
            if (scanning) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwitchLoaderSpinner(diameter = 20.dp, alpha = 1f)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = scanProgress?.let {
                            stringResource(R.string.scanning_covers_progress, it.index, it.total)
                        } ?: stringResource(R.string.scanning_covers),
                        color = HorizonWhite,
                        fontSize = (h.value * 0.026f).sp
                    )
                }
            }
            }

            Spacer(Modifier.height(h * 0.141f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardSize),
                contentAlignment = Alignment.CenterStart
            ) {
                HorizonGameCarousel(
                    games = displayGames,
                    slotCount = slotCount,
                    selectedIndex = selectedGame,
                    selectedTitle = displayGames.getOrNull(selectedGame)?.displayTitle,
                    selectionActive = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
                    showSelectedTitle = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
                    downloadingProgress = downloadingProgress,
                    cardSize = cardSize,
                    cardGap = cardGap,
                    startOffset = cardStartOffset,
                    entryProgress = homeEntryProgress,
                    onCardTap = ::selectGame,
                    onSelectedIndexChange = {
                        selectedGame = it
                    },
                    scrollPositionPx = homeScrollPositionPx,
                    onScrollPositionChange = { homeScrollPositionPx = it },
                    gamepadNavigationRequest = gamepadNavigationRequest,
                    gameSelectionRevision = gameSelectionRevision,
                    onSwipe = {
                        clearHomeSelection()
                    }
                )
            }

            Spacer(Modifier.height(h * 0.079f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.17f),
                horizontalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                HorizonMenuButton(
                    icon = HorizonMenuIconType.STORE,
                    iconColor = Color(0xFF00E8C8),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 0,
                    selected = menuSelectionArmed && selectedMenu == 0 || menuOpeningIndex == 0,
                    showLabel = menuSelectionArmed && selectedMenu == 0 || menuOpeningIndex == 0,
                    label = stringResource(R.string.home_menu_google_play),
                    opening = menuOpeningIndex == 0
                ) { activateMenu(0, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.GAMES,
                    iconColor = Color(0xFFFF0033),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 1,
                    selected = menuSelectionArmed && selectedMenu == 1 || menuOpeningIndex == 1,
                    showLabel = menuSelectionArmed && selectedMenu == 1 || menuOpeningIndex == 1,
                    label = stringResource(R.string.home_menu_games),
                    opening = menuOpeningIndex == 1
                ) { activateMenu(1, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.FILES,
                    iconColor = Color(0xFF35D060),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 2,
                    selected = menuSelectionArmed && selectedMenu == 2 || menuOpeningIndex == 2,
                    showLabel = menuSelectionArmed && selectedMenu == 2 || menuOpeningIndex == 2,
                    label = stringResource(R.string.home_menu_files),
                    opening = menuOpeningIndex == 2
                ) { activateMenu(2, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.APPS,
                    iconColor = Color(0xFFB26BFF),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 3,
                    selected = menuSelectionArmed && selectedMenu == 3 || menuOpeningIndex == 3,
                    showLabel = menuSelectionArmed && selectedMenu == 3 || menuOpeningIndex == 3,
                    label = stringResource(R.string.home_menu_apps),
                    opening = menuOpeningIndex == 3
                ) { activateMenu(3, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.GAMESIR,
                    iconColor = Color(0xFF20BFFF),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 4,
                    selected = menuSelectionArmed && selectedMenu == 4 || menuOpeningIndex == 4,
                    showLabel = menuSelectionArmed && selectedMenu == 4 || menuOpeningIndex == 4,
                    label = stringResource(R.string.home_menu_gamesir),
                    opening = menuOpeningIndex == 4
                ) { activateMenu(4, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.SETTINGS,
                    iconColor = HorizonWhite,
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 5,
                    selected = menuSelectionArmed && selectedMenu == 5 || menuOpeningIndex == 5,
                    showLabel = menuSelectionArmed && selectedMenu == 5 || menuOpeningIndex == 5,
                    label = stringResource(R.string.home_menu_settings),
                    opening = menuOpeningIndex == 5
                ) { activateMenu(5, LauncherInputSource.TOUCH) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.POWER,
                    iconColor = HorizonWhite,
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 6,
                    selected = menuSelectionArmed && selectedMenu == 6 || menuOpeningIndex == 6,
                    showLabel = menuSelectionArmed && selectedMenu == 6 || menuOpeningIndex == 6,
                    label = stringResource(R.string.home_menu_power),
                    opening = menuOpeningIndex == 6
                ) { activateMenu(6, LauncherInputSource.TOUCH) }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LocalHorizonColors.current.divider)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.096f)
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val entry = launchStaggerProgress(
                            homeEntryProgress,
                            index = 0,
                            firstDelay = 0.48f,
                            stagger = 0f,
                            duration = 0.22f
                        )
                        alpha = entry
                        translationY = 22.dp.toPx() * (1f - entry)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (externalGamepadConnected) {
                        GamepadIndicator()
                        Spacer(Modifier.height(3.dp))
                        ControllerIcon(size = h * 0.062f)
                    }
                }

Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizonButtonGlyph(
                        label = "A",
                        size = 20.dp,
                        fill = HorizonGray,
                        contentColor = HorizonBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_ok),
                        color = HorizonGray,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

    launchGhost?.let { ghost ->
        LaunchGhostOverlay(
            ghost = ghost,
            onLaunch = { launchGame(ghost.game, ghost.source) },
            onFinished = { if (launchGhost?.id == ghost.id) launchGhost = null }
        )
    }

    if (scanHint.isNotEmpty()) {
        LaunchedEffect(scanHint) {
            LauncherAudioManager.playHint(LauncherInputSource.TOUCH)
        }
        ScanHintOverlay(
            games = scanHint,
            onDismiss = { ScanCoordinator.consumeHint() }
        )
    }

    if (poweringOff || powerOffProgress.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = powerOffProgress.value))
        )
    }

    LaunchedEffect(slotCount) {
        selectedGame = selectedGame.coerceIn(0, slotCount - 1)
        backgroundGameIndex = backgroundGameIndex.coerceIn(0, slotCount - 1)
        if (tappedGameIndex >= slotCount) tappedGameIndex = -1
    }

    LaunchedEffect(showLauncherSettings) {
        if (!showLauncherSettings) {
            selectedGame = 0
            homeScrollPositionPx = 0f
            tappedGameIndex = 0
            if (locked) return@LaunchedEffect
            withFrameNanos { }
            homeFocusRequester.requestFocus()
        }
    }

LaunchedEffect(showGames) {
        if (!showGames) {
            selectedGame = 0
            homeScrollPositionPx = 0f
            tappedGameIndex = 0
            if (locked) return@LaunchedEffect
            withFrameNanos { }
            homeFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(locked) {
        if (!locked) {
            withFrameNanos { }
            homeFocusRequester.requestFocus()
        }
    }
}

private data class LaunchGhostData(
    val id: Long,
    val image: ImageBitmap?,
    val game: Game,
    val source: LauncherInputSource
)

@Composable
private fun ScanHintOverlay(
    games: List<String>,
    onDismiss: () -> Unit
) {
    val inputMode = remember { mutableStateOf(SettingsInputMode.GAMEPAD) }
    CompositionLocalProvider(LocalSettingsInputMode provides inputMode) {
        HorizonOverlay(
            title = stringResource(R.string.scan_hint_title),
            onDismiss = onDismiss
        ) {
            Text(
                text = stringResource(R.string.scan_hint_intro),
                color = SettingsWhite,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )
            games.take(8).forEach { name ->
                Text(
                    text = "•  $name",
                    color = SettingsGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.scan_hint_manual),
                color = SettingsGray,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.scan_hint_rename),
                color = SettingsGray,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(12.dp))
            HorizonOverlayChoice(
                title = stringResource(R.string.action_ok),
                selected = true,
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun LaunchGhostOverlay(
    ghost: LaunchGhostData,
    onLaunch: () -> Unit,
    onFinished: () -> Unit
) {
    val progress = remember(ghost.id) { Animatable(0f) }

    LaunchedEffect(ghost.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1400, easing = LinearEasing)
        )
    }

    LaunchedEffect(ghost.id) {
        delay(1000)
        onLaunch()
    }
    LaunchedEffect(ghost.id) {
        delay(1400)
        onFinished()
    }

    val t = progress.value
    val scrimAlpha = FastOutSlowInEasing.transform((t / 0.20f).coerceIn(0f, 1f))
    val iconProgress = FastOutSlowInEasing.transform(((t - 0.05f) / 0.16f).coerceIn(0f, 1f))
    val loadEnd = 0.30f / 1.4f
    val zoomEnd = 1.00f / 1.4f
    val zoomRaw = ((t - loadEnd) / (zoomEnd - loadEnd)).coerceIn(0f, 1f)
    val zoom = zoomRaw * zoomRaw * (3f - 2f * zoomRaw)
    val spinnerAlpha = ((t - 0.20f) / 0.14f).coerceIn(0f, 1f) * (1f - zoomRaw)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .pointerInput(ghost.id) {
                detectTapGestures(onTap = {})
            }
            .onPreviewKeyEvent { true }
    ) {
        val tile = minOf(maxWidth * 0.16f, maxHeight * 0.27f)
        val fillScale = maxOf(maxWidth.value, maxHeight.value) / tile.value
        val iconScale = 0.93f + 0.07f * iconProgress
        val coverScale = iconScale * (1f + zoom * (fillScale - 1f))
        val coverBlur = zoom * 46f
        val fadeOut = ((t - zoomEnd) / (1f - zoomEnd)).coerceIn(0f, 1f)
        val coverAlpha = iconProgress * (1f - fadeOut)

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = -maxHeight * 0.058f * (1f - zoom))
                .requiredSize(tile)
                .graphicsLayer {
                    scaleX = coverScale
                    scaleY = coverScale
                    alpha = coverAlpha
                }
                .blur(coverBlur.dp, BlurredEdgeTreatment.Unbounded)
        ) {
            val image = ghost.image
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF101E28))
                        .border(
                            width = 1.dp,
                            color = HorizonWhite.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ghost.game.displayTitle,
                        color = HorizonWhite,
                        fontSize = 15.sp,
                        maxLines = 3,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        SwitchLoaderSpinner(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.05f),
            diameter = maxOf(28.dp, maxHeight * 0.045f),
            alpha = spinnerAlpha
        )
    }
}

@Composable
private fun SwitchLoaderSpinner(
    modifier: Modifier = Modifier,
    diameter: Dp,
    alpha: Float
) {
    val rotation = rememberInfiniteTransition(label = "launchLoaderRotation")
    val sweep by rotation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "launchLoaderSweep"
    )
    Canvas(modifier = modifier.size(diameter)) {
        val stroke = 2.5.dp.toPx()
        val radius = (size.minDimension - stroke) / 2f
        val ticks = 10
        val circumference = 2f * PI.toFloat() * radius
        val dash = circumference / (ticks * 2f)
        rotate(degrees = sweep * 360f) {
            drawArc(
                color = Color.White.copy(alpha = alpha),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                )
            )
        }
    }
}

@Composable
private fun HorizonGameCarousel(
    games: List<Game>,
    slotCount: Int,
    selectedIndex: Int,
    selectedTitle: String?,
    selectionActive: Boolean,
    showSelectedTitle: Boolean,
    downloadingProgress: Map<String, Float> = emptyMap(),
    cardSize: Dp,
    cardGap: Dp,
    startOffset: Dp,
    entryProgress: Float,
    onCardTap: (Int) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    scrollPositionPx: Float,
    onScrollPositionChange: (Float) -> Unit,
    gamepadNavigationRequest: Int,
    gameSelectionRevision: Int,
    onSwipe: () -> Unit
) {
    val density = LocalDensity.current
    // Keep the idle Home screen static. A continuously animated pulse on every card
    // invalidated the whole carousel at 60 fps even when the user was not interacting.
    val selectionPulse = 1f
    val cardSizePx = with(density) { cardSize.toPx() }
    val gapPx = with(density) { cardGap.toPx() }
    val stepPx = cardSizePx + gapPx
    val startOffsetPx = with(density) { startOffset.toPx() }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val latestScrollPositionPx by rememberUpdatedState(scrollPositionPx)
    var flingVelocityPx by remember { mutableFloatStateOf(0f) }
    var bringIntoViewJob by remember { mutableStateOf<Job?>(null) }

    val entryStagger = if (slotCount > 1) {
        (0.58f / (slotCount - 1)).coerceAtLeast(0.0001f)
    } else {
        0f
    }

    val contentWidthPx = slotCount * cardSizePx + (slotCount - 1).coerceAtLeast(0) * gapPx
    val contentWidth = with(density) { contentWidthPx.toDp() }
    val maximumScrollPx = (startOffsetPx + contentWidthPx - viewportWidthPx)
        .coerceAtLeast(0f)

    fun reportScrollPosition(positionPx: Float) {
        if (stepPx <= 0f) return
        val nearestIndex = ((positionPx - startOffsetPx) / stepPx)
            .roundToInt()
            .coerceIn(0, (slotCount - 1).coerceAtLeast(0))
        onSelectedIndexChange(nearestIndex)
    }

    fun bringCardIntoView(index: Int) {
        bringIntoViewJob?.cancel()
        bringIntoViewJob = null

        if (viewportWidthPx <= 0 || stepPx <= 0f) return

        val cardStart = startOffsetPx + index * stepPx - scrollPositionPx
        val cardEnd = cardStart + cardSizePx
        val target = when {
            cardStart < 0f -> scrollPositionPx + cardStart
            cardEnd > viewportWidthPx -> scrollPositionPx + cardEnd - viewportWidthPx
            else -> null
        } ?: return

        val targetPosition = target.coerceIn(0f, maximumScrollPx)
        bringIntoViewJob = coroutineScope.launch {
            val startPosition = scrollPositionPx
            val durationNanos = 220_000_000L
            var firstFrame = 0L
            while (true) {
                val frame = withFrameNanos { it }
                if (firstFrame == 0L) firstFrame = frame
                val progress = ((frame - firstFrame).toFloat() / durationNanos)
                    .coerceIn(0f, 1f)
                val eased = FastOutSlowInEasing.transform(progress)
                onScrollPositionChange(
                    startPosition + (targetPosition - startPosition) * eased
                )
                if (progress >= 1f) break
            }
            onSelectedIndexChange(index)
        }
    }

    LaunchedEffect(gamepadNavigationRequest, viewportWidthPx, maximumScrollPx) {
        if (gamepadNavigationRequest > 0 && viewportWidthPx > 0) {
            bringCardIntoView(selectedIndex)
        }
    }

    LaunchedEffect(maximumScrollPx) {
        onScrollPositionChange(scrollPositionPx.coerceIn(0f, maximumScrollPx))
    }

    LaunchedEffect(flingVelocityPx, maximumScrollPx) {
        var velocity = flingVelocityPx
        if (abs(velocity) < 12f || maximumScrollPx <= 0f) return@LaunchedEffect

        var position = scrollPositionPx
        var previousFrame = 0L
        while (abs(velocity) >= 12f) {
            val frameTime = withFrameNanos { it }
            if (previousFrame == 0L) {
                previousFrame = frameTime
                continue
            }

            val deltaSeconds = ((frameTime - previousFrame) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)
            previousFrame = frameTime

            val unboundedPosition = position + velocity * deltaSeconds
            val nextPosition = unboundedPosition.coerceIn(0f, maximumScrollPx)
            position = nextPosition
            onScrollPositionChange(nextPosition)
            reportScrollPosition(nextPosition)

            if (nextPosition != unboundedPosition) break
            velocity *= exp(-2.4f * deltaSeconds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportWidthPx = it.width }
            .pointerInput(slotCount, stepPx, maximumScrollPx) {
                var velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        onSwipe()
                        flingVelocityPx = 0f
                        velocityTracker = VelocityTracker()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val nextPosition = (latestScrollPositionPx - dragAmount)
                            .coerceIn(0f, maximumScrollPx)
                        onScrollPositionChange(nextPosition)
                        reportScrollPosition(nextPosition)
                    },
                    onDragEnd = {
                        flingVelocityPx =
                            -velocityTracker.calculateVelocity().x * 2.2f
                    },
                    onDragCancel = {
                        flingVelocityPx = 0f
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .requiredWidth(contentWidth)
                .requiredHeight(cardSize)
                .offset {
                    IntOffset(
                        x = (
                            (contentWidthPx - viewportWidthPx) / 2f +
                                    startOffsetPx -
                                    scrollPositionPx
                            ).roundToInt(),
                        y = 0
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(cardGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(slotCount) { index ->
                val cardSelected = selectionActive && index == selectedIndex
                Box(
                    modifier = Modifier
                        .requiredSize(cardSize)
                        .graphicsLayer {
                            val entry = launchStaggerProgress(
                                entryProgress,
                                index = index,
                                firstDelay = 0.08f,
                                stagger = entryStagger,
                                duration = 0.34f
                            )
                            alpha = entry
                            translationX = (1f - entry) * (150.dp.toPx() + index * 5.dp.toPx())
                            scaleX = 0.90f + entry * 0.10f
                            scaleY = 0.90f + entry * 0.10f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (showSelectedTitle && selectedTitle != null && index == selectedIndex) {
                        key(gameSelectionRevision, selectedIndex, selectedTitle) {
                            HorizonSelectedGameTitle(
                                title = selectedTitle,
                                cardWidth = cardSize,
                                selectionKey = gameSelectionRevision,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-36).dp)
                            )
                        }
                    }
                    val game = games.getOrNull(index)
                    if (game != null) {
                        HorizonGameCard(
                            game = game,
                            selected = cardSelected,
                            size = cardSize,
                            selectionPulse = selectionPulse,
                            downloadProgress = downloadingProgress[game.packageName],
                            onClick = {
                                flingVelocityPx = 0f
                                bringCardIntoView(index)
                                onCardTap(index)
                            }
                        )
                    } else {
                        HorizonEmptyGameCard(
                            selected = cardSelected,
                            size = cardSize,
                            selectionPulse = selectionPulse,
                            onClick = {
                                flingVelocityPx = 0f
                                bringCardIntoView(index)
                                onCardTap(index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizonSelectedGameTitle(
    title: String,
    cardWidth: Dp,
    selectionKey: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val titleColor = HorizonBlue
    val titleStyle = remember(titleColor) {
        TextStyle(
            color = titleColor,
            fontSize = 23.sp
        )
    }
    val textMeasurer = rememberTextMeasurer()
    val textWidthPx = remember(title, titleStyle, density) {
        textMeasurer.measure(
            text = AnnotatedString(title),
            style = titleStyle,
            maxLines = 1,
            softWrap = false
        ).size.width
    }
    val cardWidthPx = with(density) { cardWidth.toPx() }
    val overflowPx = (textWidthPx - cardWidthPx).coerceAtLeast(0f)
    val textWidth = with(density) { textWidthPx.toDp() }
    // The title is measured before layout, so the marquee only exists for
    // titles that actually exceed the card viewport.
    val initialTextOffsetPx = overflowPx / 2f
    val textOffset = remember(selectionKey, title, cardWidthPx) { Animatable(0f) }
    var marqueeStarted by remember(selectionKey, title, cardWidthPx) {
        mutableStateOf(false)
    }

    LaunchedEffect(selectionKey, title, cardWidthPx, overflowPx) {
        marqueeStarted = false
        textOffset.stop()
        textOffset.snapTo(0f)
        if (overflowPx <= 0f) return@LaunchedEffect

        val marqueePauseMillis = 2000L
        delay(marqueePauseMillis)
        marqueeStarted = true
        while (true) {
            textOffset.animateTo(
                targetValue = -overflowPx,
                animationSpec = tween(1900, easing = LinearEasing)
            )
            delay(marqueePauseMillis)
            textOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(1900, easing = LinearEasing)
            )
            delay(marqueePauseMillis)
        }
    }

    Box(
        modifier = modifier
            .requiredWidth(cardWidth)
            .height(30.dp)
            .clipToBounds(),
        contentAlignment = if (overflowPx <= 0f) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = title,
            style = titleStyle,
            maxLines = 1,
            softWrap = false,
            modifier = if (overflowPx > 0f) {
                Modifier
                    .align(Alignment.CenterStart)
                    .requiredWidth(textWidth)
                    .offset {
                        val visibleOffset = initialTextOffsetPx +
                            if (marqueeStarted) textOffset.value else 0f
                        IntOffset(visibleOffset.roundToInt(), 0)
                    }
            } else {
                Modifier
            }
        )
    }
}

@Composable
private fun HorizonEmptyGameCard(
    selected: Boolean,
    size: Dp,
    selectionPulse: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val palette = LocalHorizonColors.current
    val frameColor = if (selected) {
        lerp(HorizonBlue, Color(0xFF9EEFFF), selectionPulse)
    } else {
        LocalHorizonColors.current.card
    }

    Box(
        modifier = Modifier
            .requiredSize(size)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(1.dp),
                        clip = false,
                        ambientColor = frameColor.copy(alpha = 0.22f),
                        spotColor = frameColor.copy(alpha = 0.22f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(1.dp))
            .drawBehind {
                val strokeWidth = (if (selected) 4.dp else 2.dp).toPx()
                drawRect(color = palette.card)
                if (selected) {
                    val glowWidth = strokeWidth + 10.dp.toPx()
                    val glowOffset = (strokeWidth - glowWidth) / 2f
                    drawRect(
                        color = frameColor.copy(
                            alpha = 0.10f + selectionPulse * 0.08f
                        ),
                        topLeft = Offset(glowOffset, glowOffset),
                        size = Size(
                            width = this.size.width - 2f * glowOffset,
                            height = this.size.height - 2f * glowOffset
                        ),
                        style = Stroke(width = glowWidth)
                    )
                }
                drawRect(
                    color = frameColor,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(
                        width = this.size.width - strokeWidth,
                        height = this.size.height - strokeWidth
                    ),
                    style = Stroke(width = strokeWidth)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    )
}

@Composable
private fun HorizonGameCard(
    game: Game,
    selected: Boolean,
    size: Dp,
    selectionPulse: Float,
    downloadProgress: Float? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val palette = LocalHorizonColors.current
    val frameColor = if (selected) {
        lerp(HorizonBlue, Color(0xFF9EEFFF), selectionPulse)
    } else {
        LocalHorizonColors.current.card
    }

    Box(
        modifier = Modifier
            .requiredSize(size)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(1.dp),
                        clip = false,
                        ambientColor = frameColor.copy(alpha = 0.22f),
                        spotColor = frameColor.copy(alpha = 0.22f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(
                RoundedCornerShape(1.dp)
            )
            .drawBehind {
                val strokeWidth = (if (selected) 4.dp else 2.dp).toPx()
                drawRect(color = palette.card)
                if (selected) {
                    val glowWidth = strokeWidth + 10.dp.toPx()
                    val glowOffset = (strokeWidth - glowWidth) / 2f
                    drawRect(
                        color = frameColor.copy(
                            alpha = 0.10f + selectionPulse * 0.08f
                        ),
                        topLeft = Offset(glowOffset, glowOffset),
                        size = Size(
                            width = this.size.width - 2f * glowOffset,
                            height = this.size.height - 2f * glowOffset
                        ),
                        style = Stroke(width = glowWidth)
                    )
                }
                drawRect(
                    color = frameColor,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = Size(
                        width = this.size.width - strokeWidth,
                        height = this.size.height - strokeWidth
                    ),
                    style = Stroke(width = strokeWidth)
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        val cover = rememberCoverBitmap(game.coverPath ?: game.iconPath)
        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                contentScale = ContentScale.Crop
            )
        }

        if (downloadProgress != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(downloadProgress.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(HorizonBlue)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCoverBitmap(coverPath: String?): ImageBitmap? {
    var bitmap by remember(coverPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(coverPath) {
        bitmap = if (coverPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val file = File(coverPath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    bmp?.asImageBitmap()
                } else null
            }
        }
    }
    return bitmap
}

@Composable
private fun GameScreenshotBackground(screenshotPath: String?) {
    var bitmap by remember(screenshotPath) {
        mutableStateOf(
            screenshotPath
                ?.takeIf { it.isNotBlank() }
                ?.let { BackdropCache.get(it) }
        )
    }
    LaunchedEffect(screenshotPath) {
        bitmap = if (screenshotPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) { BackdropCache.load(screenshotPath) }
        }
    }

    val darkTheme = LocalHorizonColors.current.background.luminance() < 0.5f
    // Only transform the already-blurred texture. This preserves the slow, fluid
    // motion without running a blur shader over the full display every frame.
    val drift = rememberInfiniteTransition(label = "homeBackdropDrift")
    val scale by drift.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.075f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeBackdropScale"
    )

    Crossfade(
        targetState = bitmap,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "homeBackdropCrossfade"
    ) { frame ->
        if (frame != null) {
            Box(Modifier.fillMaxSize()) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    contentScale = ContentScale.Crop
                )
                if (darkTheme) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.46f))
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = if (darkTheme) 0.66f else 0.32f)
                                    ),
                                    center = center,
                                    radius = size.maxDimension * 0.72f
                                )
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun ProfileIcon(
    size: Dp
) {
    Box(
        modifier = Modifier
            .requiredSize(size)
            .clip(CircleShape)
            .background(LocalHorizonColors.current.panel)
            .border(
                width = 2.dp,
                color = LocalHorizonColors.current.divider,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "H0",
            color = HorizonWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private enum class HorizonMenuIconType {
    GAMES,
    FILES,
    APPS,
    STORE,
    GAMESIR,
    SETTINGS,
    POWER
}

@Composable
private fun HorizonMenuButton(
    icon: HorizonMenuIconType,
    iconColor: Color,
    size: Dp,
    entryProgress: Float = 1f,
    entryIndex: Int = 0,
    selected: Boolean = false,
    showLabel: Boolean = false,
    label: String = "",
    opening: Boolean = false,
    onClick: () -> Unit
) {
    // The selected frame is static while idle; transitions are reserved for actual actions.
    val palette = LocalHorizonColors.current
    val menuSurface = if (palette.background.luminance() > 0.5f) {
        Color(0xFFE8EAED)
    } else {
        Color(0xFF555555)
    }
    val selectionAlpha = 1f
    val iconReveal by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = tween(640, easing = FastOutSlowInEasing),
        label = "menuIconReveal"
    )
    val iconProgress = if (opening) iconReveal else 1f
    val entrance = launchStaggerProgress(
        entryProgress,
        index = entryIndex,
        firstDelay = 0.34f,
        stagger = 0.045f,
        duration = 0.28f
    )

    Box(
        modifier = Modifier
            .width(size)
            .height(if (showLabel) size + 30.dp else size)
            .graphicsLayer {
                alpha = entrance
                translationY = 36.dp.toPx() * (1f - entrance)
                scaleX = 0.76f + entrance * 0.24f
                scaleY = 0.76f + entrance * 0.24f
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredSize(size)
                .clip(CircleShape)
                .background(menuSurface)
                .then(
                    if (selected) {
                        Modifier.border(
                            width = 2.dp,
                            color = HorizonBlue.copy(alpha = selectionAlpha),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            HorizonMenuGlyph(
                type = icon,
                color = iconColor,
                progress = iconProgress,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.21f)
            )
        }
        if (showLabel) {
            Text(
                text = label,
                color = HorizonBlue,
                fontSize = 18.sp,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = size + 4.dp)
                    .wrapContentWidth(unbounded = true)
            )
        }
    }
}

@Composable
private fun HorizonMenuGlyph(
    type: HorizonMenuIconType,
    color: Color,
    progress: Float,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val iconColor = color.copy(alpha = 0.86f + 0.14f * progress)
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = size.minDimension * 0.075f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (type) {
            HorizonMenuIconType.GAMES -> {
                val screenSize = Size(size.width * 0.54f, size.height * 0.61f)
                val screenTopLeft = Offset(
                    center.x - screenSize.width / 2f,
                    size.height * 0.16f
                )
                val screenBottom = screenTopLeft.y + screenSize.height
                val legTipY = screenBottom + size.height * 0.10f

                val s0 = launchStaggerProgress(progress, 0, firstDelay = 0f, stagger = 0f, duration = 0.55f)
                scale(easeOutBack(s0), pivot = center) {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = screenTopLeft,
                        size = screenSize,
                        cornerRadius = CornerRadius(size.minDimension * 0.045f),
                        style = stroke
                    )
                }

                val s1 = launchStaggerProgress(progress, 1, firstDelay = 0.55f, stagger = 0f, duration = 0.25f)
                val leftStart = Offset(center.x - screenSize.width * 0.28f, screenBottom)
                val leftEnd = Offset(center.x, legTipY)
                val leftTip = Offset(
                    leftStart.x + (leftEnd.x - leftStart.x) * s1,
                    leftStart.y + (leftEnd.y - leftStart.y) * s1
                )
                drawLine(
                    color = iconColor,
                    start = leftStart,
                    end = leftTip,
                    strokeWidth = strokeWidth
                )

                val s2 = launchStaggerProgress(progress, 2, firstDelay = 0.78f, stagger = 0f, duration = 0.22f)
                val rightStart = Offset(center.x, legTipY)
                val rightEnd = Offset(center.x + screenSize.width * 0.28f, screenBottom)
                val rightTip = Offset(
                    rightStart.x + (rightEnd.x - rightStart.x) * s2,
                    rightStart.y + (rightEnd.y - rightStart.y) * s2
                )
                drawLine(
                    color = iconColor,
                    start = rightStart,
                    end = rightTip,
                    strokeWidth = strokeWidth
                )
            }

            HorizonMenuIconType.FILES -> {
                val folderPath = Path().apply {
                    moveTo(size.width * 0.14f, size.height * 0.30f)
                    lineTo(size.width * 0.36f, size.height * 0.30f)
                    lineTo(size.width * 0.46f, size.height * 0.20f)
                    lineTo(size.width * 0.84f, size.height * 0.20f)
                    lineTo(size.width * 0.91f, size.height * 0.29f)
                    lineTo(size.width * 0.91f, size.height * 0.78f)
                    lineTo(size.width * 0.14f, size.height * 0.78f)
                    close()
                }

                val s0 = launchStaggerProgress(progress, 0, firstDelay = 0f, stagger = 0f, duration = 0.55f)
                scale(easeOutBack(s0), pivot = center) {
                    drawPath(folderPath, iconColor, style = stroke)
                }

                val s1 = launchStaggerProgress(progress, 1, firstDelay = 0.55f, stagger = 0f, duration = 0.45f)
                val docWidth = size.width * 0.22f
                val docHeight = size.height * 0.26f
                val docBottom = size.height * 0.60f
                val docTop = size.height * 0.16f
                val docY = docBottom + (docTop - docBottom) * s1.coerceIn(0f, 1f)
                scale(0.6f + 0.4f * easeOutBack(s1), pivot = center) {
                    drawRoundRect(
                        color = iconColor,
                        topLeft = Offset(center.x - docWidth / 2f, docY),
                        size = Size(docWidth, docHeight),
                        cornerRadius = CornerRadius(size.minDimension * 0.03f),
                        style = stroke
                    )
                }
            }

            HorizonMenuIconType.APPS -> {
                val square = size.minDimension * 0.21f
                val gap = size.minDimension * 0.09f
                val left = center.x - square - gap / 2f
                val right = center.x + gap / 2f
                val top = center.y - square - gap / 2f
                val bottom = center.y + gap / 2f
                val positions = listOf(
                    Offset(left, top),
                    Offset(right, top),
                    Offset(left, bottom),
                    Offset(right, bottom)
                )
                positions.forEachIndexed { index, position ->
                    val s = launchStaggerProgress(
                        progress,
                        index = index,
                        firstDelay = 0f,
                        stagger = 0.15f,
                        duration = 0.55f
                    )
                    val pulse = if (s <= 0f || s >= 1f) 0f else sin(s * PI.toFloat())
                    val scaleFactor = 1f + 0.32f * pulse
                    val squareCenter = Offset(position.x + square / 2f, position.y + square / 2f)
                    scale(scaleFactor, pivot = squareCenter) {
                        drawRoundRect(
                            color = iconColor,
                            topLeft = position,
                            size = Size(square, square),
                            cornerRadius = CornerRadius(size.minDimension * 0.05f),
                            style = stroke
                        )
                    }
                }
            }

            HorizonMenuIconType.STORE -> {
                val tip = Offset(size.width * 0.94f, size.height * 0.50f)
                val topLeft = Offset(size.width * 0.22f, size.height * 0.06f)
                val bottomLeft = Offset(size.width * 0.22f, size.height * 0.94f)
                val meetPoint = Offset(size.width * 0.46f, size.height * 0.50f)

                fun partial(start: Offset, end: Offset, t: Float): Offset = Offset(
                    start.x + (end.x - start.x) * t,
                    start.y + (end.y - start.y) * t
                )

                val s0 = launchStaggerProgress(progress, 0, firstDelay = 0f, stagger = 0f, duration = 0.18f)
                drawLine(iconColor, topLeft, partial(topLeft, tip, s0), strokeWidth)

                val s1 = launchStaggerProgress(progress, 1, firstDelay = 0.16f, stagger = 0f, duration = 0.16f)
                drawLine(iconColor, tip, partial(tip, bottomLeft, s1), strokeWidth)

                val s2 = launchStaggerProgress(progress, 2, firstDelay = 0.30f, stagger = 0f, duration = 0.14f)
                drawLine(iconColor, bottomLeft, partial(bottomLeft, topLeft, s2), strokeWidth)

                val s3 = launchStaggerProgress(progress, 3, firstDelay = 0.44f, stagger = 0f, duration = 0.14f)
                drawLine(iconColor, meetPoint, partial(meetPoint, topLeft, s3), strokeWidth)

                val s4 = launchStaggerProgress(progress, 4, firstDelay = 0.54f, stagger = 0f, duration = 0.14f)
                drawLine(iconColor, meetPoint, partial(meetPoint, bottomLeft, s4), strokeWidth)

                val s5 = launchStaggerProgress(progress, 5, firstDelay = 0.64f, stagger = 0f, duration = 0.14f)
                drawLine(iconColor, meetPoint, partial(meetPoint, tip, s5), strokeWidth)
            }

            HorizonMenuIconType.GAMESIR -> {
                val bodyTopLeft = Offset(size.width * 0.13f, size.height * 0.27f)
                val bodySize = Size(size.width * 0.74f, size.height * 0.46f)

                val s0 = launchStaggerProgress(progress, 0, firstDelay = 0f, stagger = 0f, duration = 0.50f)
                rotate(-18f * (1f - s0.coerceIn(0f, 1f)), pivot = center) {
                    scale(easeOutBack(s0), pivot = center) {
                        drawRoundRect(
                            color = iconColor,
                            topLeft = bodyTopLeft,
                            size = bodySize,
                            cornerRadius = CornerRadius(size.minDimension * 0.18f),
                            style = stroke
                        )
                    }
                }

                val s1 = launchStaggerProgress(progress, 1, firstDelay = 0.48f, stagger = 0f, duration = 0.20f)
                val dPadCenter = Offset(size.width * 0.32f, size.height * 0.50f)
                scale(easeOutBack(s1), pivot = dPadCenter) {
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.25f, size.height * 0.50f),
                        end = Offset(size.width * 0.39f, size.height * 0.50f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width * 0.32f, size.height * 0.43f),
                        end = Offset(size.width * 0.32f, size.height * 0.57f),
                        strokeWidth = strokeWidth
                    )
                }

                val s2 = launchStaggerProgress(progress, 2, firstDelay = 0.66f, stagger = 0f, duration = 0.17f)
                scale(easeOutBack(s2), pivot = Offset(size.width * 0.68f, size.height * 0.44f)) {
                    drawCircle(
                        color = iconColor,
                        radius = size.minDimension * 0.055f,
                        center = Offset(size.width * 0.68f, size.height * 0.44f),
                        style = stroke
                    )
                }

                val s3 = launchStaggerProgress(progress, 3, firstDelay = 0.81f, stagger = 0f, duration = 0.19f)
                scale(easeOutBack(s3), pivot = Offset(size.width * 0.77f, size.height * 0.56f)) {
                    drawCircle(
                        color = iconColor,
                        radius = size.minDimension * 0.055f,
                        center = Offset(size.width * 0.77f, size.height * 0.56f),
                        style = stroke
                    )
                }
            }

            HorizonMenuIconType.SETTINGS -> {
                rotate(360f * progress, pivot = center) {
                    scale(0.80f + 0.20f * progress, pivot = center) {
                        val outerRadius = size.minDimension * 0.30f
                        val spokeStart = size.minDimension * 0.34f
                        val spokeEnd = size.minDimension * 0.46f
                        drawCircle(
                            color = iconColor,
                            radius = outerRadius,
                            center = center,
                            style = stroke
                        )
                        repeat(8) { index ->
                            val angle = index * PI / 4.0
                            drawLine(
                                color = iconColor,
                                start = Offset(
                                    center.x + cos(angle).toFloat() * spokeStart,
                                    center.y + sin(angle).toFloat() * spokeStart
                                ),
                                end = Offset(
                                    center.x + cos(angle).toFloat() * spokeEnd,
                                    center.y + sin(angle).toFloat() * spokeEnd
                                ),
                                strokeWidth = strokeWidth
                            )
                        }
                        drawCircle(
                            color = iconColor,
                            radius = size.minDimension * 0.105f,
                            center = center,
                            style = stroke
                        )
                    }
                }
            }

            HorizonMenuIconType.POWER -> {
                val ringRadius = size.minDimension * 0.34f

                val s0 = launchStaggerProgress(progress, 0, firstDelay = 0f, stagger = 0f, duration = 0.60f)
                drawArc(
                    color = iconColor,
                    topLeft = Offset(
                        center.x - ringRadius,
                        center.y - ringRadius
                    ),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    startAngle = -48f,
                    sweepAngle = 276f * easeOutBack(s0),
                    useCenter = false,
                    style = stroke
                )

                val s1 = launchStaggerProgress(progress, 1, firstDelay = 0.58f, stagger = 0f, duration = 0.42f)
                val lineTop = size.height * 0.14f
                val lineBottom = center.y + size.height * 0.06f
                val lineEnd = lineTop + (lineBottom - lineTop) * s1.coerceIn(0f, 1f)
                drawLine(
                    color = iconColor,
                    start = Offset(center.x, lineTop),
                    end = Offset(center.x, lineEnd),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

@Composable
private fun GamepadIndicator() {

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(4) { index ->

            Box(
                modifier = Modifier
                    .size(
                        width = 7.dp,
                        height = 6.dp
                    )
                    .background(
                        if (index == 0) {
                            Color(0xFFB6E800)
                        } else {
                            Color(0xFF777777)
                        }
                    )
            )
        }
    }
}

@Composable
private fun ControllerIcon(
    size: Dp
) {

    Image(
        painter = painterResource(R.drawable.game),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .padding(2.dp),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(HorizonWhite)
    )
}
