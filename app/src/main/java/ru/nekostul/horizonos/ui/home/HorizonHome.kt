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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.hardware.input.InputManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
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
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import ru.nekostul.horizonos.ui.games.GameLauncher
import ru.nekostul.horizonos.ui.games.GameLibrary
import ru.nekostul.horizonos.ui.games.GamesScreen
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
import kotlinx.coroutines.flow.first
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


@Composable
private fun BatteryLevel(): Int {

    val context = LocalContext.current

    var batteryLevel by remember {
        mutableIntStateOf(100)
    }

    LaunchedEffect(Unit) {

        while (true) {

            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            if (intent != null) {

                val level = intent.getIntExtra(
                    BatteryManager.EXTRA_LEVEL,
                    -1
                )

                val scale = intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1
                )

                if (level >= 0 && scale > 0) {
                    batteryLevel =
                        (level * 100 / scale)
                }
            }

            delay(5000)
        }
    }

    return batteryLevel
}

@Composable
private fun CurrentTime(): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    var currentTime by remember {
        mutableStateOf(
            SimpleDateFormat(
                pattern,
                Locale.getDefault()
            ).format(Date())
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat(
                pattern,
                Locale.getDefault()
            ).format(Date())

            delay(1000)
        }
    }

    return currentTime
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
    onRequestPermissions: (Array<String>) -> Unit = {},
    onOpenGameFolder: (((android.net.Uri?) -> Unit) -> Unit) = {}
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val homeFocusRequester = remember { FocusRequester() }

    val currentTime = CurrentTime()
    val battery = BatteryLevel()
    val externalGamepadConnected = ExternalGamepadConnected()
    var homeEntryTarget by remember { mutableFloatStateOf(0f) }
    val homeEntryProgress by animateFloatAsState(
        targetValue = homeEntryTarget,
        animationSpec = tween(920, delayMillis = 35, easing = FastOutSlowInEasing),
        label = "homeEntryProgress"
    )

    LaunchedEffect("home-entry") {
        // Keep the first home frame hidden, then animate every layer from its
        // actual starting position instead of composing directly at the end.
        withFrameNanos { }
        homeEntryTarget = 1f
    }

    val gameLibrary = remember { GameLibrary(context) }
    val games by gameLibrary.games.collectAsState(initial = emptyList())
    val launcherSettingsRepository = remember { LauncherSettingsRepository(context) }
    val launcherSettings by launcherSettingsRepository.settings.collectAsState(initial = LauncherSettings())
    val visibleGames = games.filterNot { it.hidden }
    val gameLauncher = remember { GameLauncher() }

    // Keep twelve empty slots as the minimum and extend the strip only when
    // the saved library grows beyond that initial Home layout.
    val slotCount = maxOf(HomeCardSlotCount, visibleGames.size)

    var selectedGame by remember {
        mutableIntStateOf(0)
    }

    // Keep the physical carousel position outside the carousel composable.
    // The carousel is temporarily removed while Settings is open, but its
    // position and the page indicator must remain synchronized.
    var homeScrollPositionPx by remember {
        mutableFloatStateOf(0f)
    }

    var tappedGameIndex by remember {
        // The first slot is the initial Home selection, matching the reference.
        mutableIntStateOf(0)
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

    var showPowerMenu by remember {
        mutableStateOf(false)
    }

    var showLauncherSettings by remember {
        mutableStateOf(false)
    }

    var showGames by remember {
        mutableStateOf(false)
    }

    var showFiles by remember {
        mutableStateOf(false)
    }

    // Ghost launch animation layer (null when inactive).
    var launchGhost by remember {
        mutableStateOf<LaunchGhostData?>(null)
    }

    // Automatic metadata scan state comes from the app-wide coordinator so the
    // Home indicator reflects both automatic and manually started scans.
    val scanning by ScanCoordinator.scanning.collectAsState()
    val scanProgress by ScanCoordinator.progress.collectAsState()
    val scanHint by ScanCoordinator.hint.collectAsState()

    // While the launch sequence runs, the Home content zooms toward the
    // player and dims, mirroring the console's fade into the loading screen.
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
    }

    fun moveMenuSelection(direction: Int) {
        cancelMenuOpening()
        selectedMenu = (selectedMenu + direction + 5) % 5
        menuSelectionArmed = true
        tappedGameIndex = -1
    }

    fun moveDownToMenu() {
        cancelMenuOpening()
        selectedMenu = 0
        menuSelectionArmed = true
        tappedGameIndex = -1
    }

    fun moveUpToGames() {
        cancelMenuOpening()
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = selectedGame
    }

    fun activateMenu(index: Int) {

        if (menuOpeningIndex >= 0) return

        if (selectedMenu != index || !menuSelectionArmed) {
            selectedMenu = index
            menuSelectionArmed = true
            tappedGameIndex = -1
            return
        }

        // Let the selected icon finish its entrance animation before opening
        // the destination screen or external application.
        menuOpeningIndex = index
        menuOpeningJob = coroutineScope.launch {
            delay(640)
            if (menuOpeningIndex != index) return@launch
            menuSelectionArmed = false
            menuOpeningIndex = -1
            menuOpeningJob = null

            when (index) {

                // Игры
                0 -> {
                    showGames = true
                }

                // Файлы
                1 -> {
                    showFiles = true
                }

                // GameSir
                2 -> {
                    val packages = listOf(
                        "com.xiaoji.gamemiracle",
                        "com.gamesir"
                    )

                    for (packageName in packages) {
                        val launchIntent =
                            context.packageManager.getLaunchIntentForPackage(packageName)

                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                            break
                        }
                    }
                }

                // Настройки лаунчера
                3 -> {
                    showLauncherSettings = true
                }

                // Питание
                4 -> {
                    showPowerMenu = true
                }
            }
        }
    }

fun launchGame(game: Game) {
        when (val result = gameLauncher.launch(context, game)) {
            GameLaunchResult.Launched -> Unit
            is GameLaunchResult.Failed -> Toast.makeText(
                context,
                result.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Plays the Switch-style launch transition before the real launch. Games
     * with artwork show their tile on the loading screen; the rest fall back
     * to a plain title tile so every launch feels the same.
     */
    fun launchGameWithAnimation(game: Game) {
        if (launchGhost != null) return
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
                game = game
            )
        }
    }

    /**
     * Queues newly added games for automatic metadata/covers scanning. The scan
     * runs in an app-wide coordinator, so it keeps going after leaving Home or
     * the Games window, and survives the app being backgrounded.
     */
    fun enqueueAutoScan(added: List<Game>) {
        ScanCoordinator.init(context)
        ScanCoordinator.consumeHint()
        ScanCoordinator.enqueue(added)
    }

    fun selectGame(index: Int) {
        val wasSelected = selectedGame == index && tappedGameIndex == index
        selectedGame = index
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = index
        if (!wasSelected) {
            gameSelectionRevision++
        }

        if (wasSelected) {            visibleGames.getOrNull(index)?.let { launchGameWithAnimation(it) }
        }
    }

    // Every Home visit starts at slot zero. Keep the selected index and the
    // physical strip position in sync so a restored/recreated Activity cannot
    // reopen on a later card while the indicator still points at the first one.
    LaunchedEffect(Unit) {
        selectedGame = 0
        homeScrollPositionPx = 0f
        tappedGameIndex = 0
    }

    if (showGames) {
        GamesScreen(
            onOpenFolder = onOpenGameFolder,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HorizonBackground)
            .graphicsLayer {
                val zoom = FastOutSlowInEasing.transform(homeLaunchZoom)
                scaleX = 1f + 0.085f * zoom
                scaleY = 1f + 0.085f * zoom
                alpha = 1f - 0.45f * zoom
            }
            .focusRequester(homeFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->

                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

if (isHorizonConfirmKey(event)) {
                    if (menuSelectionArmed && selectedMenu >= 0) {
                        activateMenu(selectedMenu)
                    } else {
                        visibleGames.getOrNull(selectedGame)?.let { launchGameWithAnimation(it) }
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
        // Match the reference strip: slot 0 begins inside the left margin and
        // the remaining slots continue beyond the viewport to the right.
        val cardStartOffset = h * 0.114f

        // Blurred screenshot of the selected game as a living backdrop.
        if (launcherSettings.screenshotBackgroundEnabled) {
            GameScreenshotBackground(
                screenshotPath = visibleGames.getOrNull(selectedGame)?.screenshotPath
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
                ProfileIcon(size = h * 0.082f)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTime,
                        color = HorizonWhite,
                        fontSize = (h.value * 0.042f).sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(h * 0.018f))
                    Text(
                        text = "✈",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.039f).sp
                    )
                    Spacer(Modifier.width(h * 0.012f))
                    Text(
                        text = "⌁",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.040f).sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(h * 0.020f))
                    Text(
                        text = "$battery%",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.039f).sp
                    )
                    Spacer(Modifier.width(h * 0.010f))
                    BatteryIcon(
                        width = h * 0.052f,
                        height = h * 0.032f,
                        level = battery
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
                    games = visibleGames,
                    slotCount = slotCount,
                    selectedIndex = selectedGame,
                    selectedTitle = visibleGames.getOrNull(selectedGame)?.displayTitle,
                    selectionActive = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
                    showSelectedTitle = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
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
                // The existing button meanings and actions are intentionally unchanged.
                HorizonMenuButton(
                    icon = HorizonMenuIconType.GAMES,
                    iconColor = Color(0xFFFF0033),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 0,
                    selected = menuSelectionArmed && selectedMenu == 0 || menuOpeningIndex == 0,
                    showLabel = menuSelectionArmed && selectedMenu == 0 || menuOpeningIndex == 0,
                    label = stringResource(R.string.home_menu_games),
                    opening = menuOpeningIndex == 0
                ) { activateMenu(0) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.FILES,
                    iconColor = Color(0xFF35D060),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 1,
                    selected = menuSelectionArmed && selectedMenu == 1 || menuOpeningIndex == 1,
                    showLabel = menuSelectionArmed && selectedMenu == 1 || menuOpeningIndex == 1,
                    label = stringResource(R.string.home_menu_files),
                    opening = menuOpeningIndex == 1
                ) { activateMenu(1) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.GAMESIR,
                    iconColor = Color(0xFF20BFFF),
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 2,
                    selected = menuSelectionArmed && selectedMenu == 2 || menuOpeningIndex == 2,
                    showLabel = menuSelectionArmed && selectedMenu == 2 || menuOpeningIndex == 2,
                    label = stringResource(R.string.home_menu_gamesir),
                    opening = menuOpeningIndex == 2
                ) { activateMenu(2) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.SETTINGS,
                    iconColor = HorizonWhite,
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 3,
                    selected = menuSelectionArmed && selectedMenu == 3 || menuOpeningIndex == 3,
                    showLabel = menuSelectionArmed && selectedMenu == 3 || menuOpeningIndex == 3,
                    label = stringResource(R.string.home_menu_settings),
                    opening = menuOpeningIndex == 3
                ) { activateMenu(3) }
                HorizonMenuButton(
                    icon = HorizonMenuIconType.POWER,
                    iconColor = HorizonWhite,
                    size = h * 0.105f,
                    entryProgress = homeEntryProgress,
                    entryIndex = 4,
                    selected = menuSelectionArmed && selectedMenu == 4 || menuOpeningIndex == 4,
                    showLabel = menuSelectionArmed && selectedMenu == 4 || menuOpeningIndex == 4,
                    label = stringResource(R.string.home_menu_power),
                    opening = menuOpeningIndex == 4
                ) { activateMenu(4) }
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

    // Ghost launch animation overlays the whole Home when a game is launched.
    launchGhost?.let { ghost ->
        LaunchGhostOverlay(
            ghost = ghost,
            onLaunch = { launchGame(ghost.game) },
            onFinished = { if (launchGhost?.id == ghost.id) launchGhost = null }
        )
    }

    if (scanHint.isNotEmpty()) {
        ScanHintOverlay(
            games = scanHint,
            onDismiss = { ScanCoordinator.consumeHint() }
        )
    }

    LaunchedEffect(slotCount) {
        selectedGame = selectedGame.coerceIn(0, slotCount - 1)
        if (tappedGameIndex >= slotCount) tappedGameIndex = -1
    }

    // Settings temporarily removes the Home content from composition. Start
    // a fresh Home session at the first card so the physical strip and the
    // indicator can never reopen on different positions.
    LaunchedEffect(showLauncherSettings) {
        if (!showLauncherSettings) {
            selectedGame = 0
            homeScrollPositionPx = 0f
            tappedGameIndex = 0
            withFrameNanos { }
            homeFocusRequester.requestFocus()
        }
    }

LaunchedEffect(showGames) {
        if (!showGames) {
            selectedGame = 0
            homeScrollPositionPx = 0f
            tappedGameIndex = 0
            withFrameNanos { }
            homeFocusRequester.requestFocus()
        }
    }
}

private data class LaunchGhostData(
    val id: Long,
    val image: ImageBitmap?,
    val game: Game
)

/**
 * Shown after an automatic scan when no cover could be found for the newly
 * added games. Explains how to scan manually and how to fix the title.
 */
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

/**
 * The console-style launch sequence: Home zooms away under a black fade, the
 * game tile then holds centre screen above a spinning loader until the
 * emulator/application window takes over.
 */
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

    // Hand control to the emulator once the cover has fully approached the
    // screen, while the layer is still mostly opaque.
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
    // Loading holds for ~0.3s; the cover then zooms in and blurs.
    val loadEnd = 0.30f / 1.4f
    val zoomEnd = 1.00f / 1.4f
    val zoomRaw = ((t - loadEnd) / (zoomEnd - loadEnd)).coerceIn(0f, 1f)
    // Non-linear approach: smoothstep accelerates, then settles at full size.
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
        // Uniform scale that makes the tile cover the whole screen.
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
                // Games without artwork still receive the full transition on
                // a plain tile carrying their title.
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

/** The console loader: a white ring of dashes rotating at a steady pace. */
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


// ═══════════════════════════════════════════════════════════════════
// EMPTY GAME CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun HorizonGameCarousel(
    games: List<Game>,
    slotCount: Int,
    selectedIndex: Int,
    selectedTitle: String?,
    selectionActive: Boolean,
    showSelectedTitle: Boolean,
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
    val selectionTransition = rememberInfiniteTransition(label = "cardSelectionPulse")
    val selectionPulse by selectionTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardSelectionPulseValue"
    )
    val cardSizePx = with(density) { cardSize.toPx() }
    val gapPx = with(density) { cardGap.toPx() }
    val stepPx = cardSizePx + gapPx
    val startOffsetPx = with(density) { startOffset.toPx() }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val latestScrollPositionPx by rememberUpdatedState(scrollPositionPx)
    var flingVelocityPx by remember { mutableFloatStateOf(0f) }
    var bringIntoViewJob by remember { mutableStateOf<Job?>(null) }

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

    // A gamepad changes the logical selection without producing a drag event.
    // Bring the newly selected card into the viewport when it reaches either
    // edge of the visible strip.
    LaunchedEffect(gamepadNavigationRequest, viewportWidthPx, maximumScrollPx) {
        if (gamepadNavigationRequest > 0 && viewportWidthPx > 0) {
            bringCardIntoView(selectedIndex)
        }
    }

    LaunchedEffect(maximumScrollPx) {
        onScrollPositionChange(scrollPositionPx.coerceIn(0f, maximumScrollPx))
    }

    // A fling keeps moving freely after the finger leaves the screen. Its
    // distance is proportional to the swipe velocity, and strong swipes can
    // reach the end of the fixed 12-card strip.
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
                        // Finger velocity is opposite to the content offset:
                        // a left swipe increases scrollPositionPx.
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
                        // Box centers oversized children by default. Cancel
                        // that centering so slot 0 starts at startOffsetPx.
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
                                stagger = 0.035f,
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
    // requiredWidth reports an oversized child as centered when its parent
    // cannot accommodate it. Move it by half the overflow so the initial
    // frame starts at the real beginning of the measured line.
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
                    // Keep the full measured line wider than the viewport;
                    // only the outer card-width window clips it during marquee.
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


// ═══════════════════════════════════════════════════════════════════
// GAME CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun HorizonGameCard(
    game: Game,
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

        // The card shows the scraped cover image, or the app icon for Android
        // applications. The game title is displayed as a separate heading
        // above the card, not inside it.
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
    }
}

/** Loads the square cover image from the local cache path, if present. */
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

/**
 * Blurred screenshot of the selected game used as the Home backdrop:
 * very light blur, a very slow Ken-Burns drift, a crossfade between games and
 * a soft vignette. In the dark theme it is darkened; in the light theme it is
 * shown plain. When a game has no screenshot the grey Home background remains.
 */
@Composable
private fun GameScreenshotBackground(screenshotPath: String?) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(screenshotPath) {
        bitmap = if (screenshotPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val file = File(screenshotPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } else null
            }
        }
    }

    val darkTheme = LocalHorizonColors.current.background.luminance() < 0.5f

    // Extremely slow, smooth drift so the backdrop feels alive.
    val drift = rememberInfiniteTransition(label = "homeBackdropDrift")
    val scale by drift.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.075f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
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
                        }
                        .blur(7.dp, BlurredEdgeTreatment.Unbounded),
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


// ═══════════════════════════════════════════════════════════════════
// PROFILE
// ═══════════════════════════════════════════════════════════════════

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


// ═══════════════════════════════════════════════════════════════════
// BATTERY
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun BatteryIcon(
    width: Dp,
    height: Dp,
    level: Int
) {

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .border(2.dp, HorizonWhite)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0, 100) / 100f)
                    .background(HorizonWhite)
            )
        }
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(height * 0.45f)
                .background(HorizonWhite)
        )
    }
}


// ═══════════════════════════════════════════════════════════════════
// SYSTEM MENU BUTTON
// ═══════════════════════════════════════════════════════════════════

private enum class HorizonMenuIconType {
    GAMES,
    FILES,
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
    val selectionTransition = rememberInfiniteTransition(label = "menuSelectionPulse")
    val selectionAlpha by selectionTransition.animateFloat(
        initialValue = if (selected) 0.45f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "menuSelectionAlpha"
    )
    val iconReveal by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = tween(620, easing = FastOutSlowInEasing),
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
                .background(Color(0xFF555555))
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
                val scaleFactor = 0.84f + 0.16f * progress
                scale(scaleFactor, pivot = center) {
                    val screenSize = Size(size.width * 0.54f, size.height * 0.61f)
                    val screenTopLeft = Offset(
                        center.x - screenSize.width / 2f,
                        size.height * 0.16f
                    )
                    drawRoundRect(
                        color = iconColor,
                        topLeft = screenTopLeft,
                        size = screenSize,
                        cornerRadius = CornerRadius(size.minDimension * 0.045f),
                        style = stroke
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(
                            center.x - screenSize.width * 0.28f,
                            screenTopLeft.y + screenSize.height
                        ),
                        end = Offset(
                            center.x,
                            screenTopLeft.y + screenSize.height + size.height * 0.10f
                        ),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(
                            center.x,
                            screenTopLeft.y + screenSize.height + size.height * 0.10f
                        ),
                        end = Offset(
                            center.x + screenSize.width * 0.28f,
                            screenTopLeft.y + screenSize.height
                        ),
                        strokeWidth = strokeWidth
                    )
                }
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
                translate(
                    left = -size.width * 0.10f * (1f - progress),
                    top = size.height * 0.07f * (1f - progress)
                ) {
                    drawPath(folderPath, iconColor, style = stroke)
                }
            }

            HorizonMenuIconType.GAMESIR -> {
                val scaleFactor = 0.82f + 0.18f * progress
                rotate(-18f * (1f - progress), pivot = center) {
                    scale(scaleFactor, pivot = center) {
                        val bodyTopLeft = Offset(
                            size.width * 0.13f,
                            size.height * 0.27f
                        )
                        val bodySize = Size(
                            size.width * 0.74f,
                            size.height * 0.46f
                        )
                        drawRoundRect(
                            color = iconColor,
                            topLeft = bodyTopLeft,
                            size = bodySize,
                            cornerRadius = CornerRadius(size.minDimension * 0.18f),
                            style = stroke
                        )
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
                        drawCircle(
                            color = iconColor,
                            radius = size.minDimension * 0.055f,
                            center = Offset(size.width * 0.68f, size.height * 0.44f),
                            style = stroke
                        )
                        drawCircle(
                            color = iconColor,
                            radius = size.minDimension * 0.055f,
                            center = Offset(size.width * 0.77f, size.height * 0.56f),
                            style = stroke
                        )
                    }
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
                scale(0.80f + 0.20f * progress, pivot = center) {
                    val ringRadius = size.minDimension * 0.34f
                    drawArc(
                        color = iconColor,
                        topLeft = Offset(
                            center.x - ringRadius,
                            center.y - ringRadius
                        ),
                        size = Size(ringRadius * 2f, ringRadius * 2f),
                        startAngle = -48f,
                        sweepAngle = 276f,
                        useCenter = false,
                        style = stroke
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(center.x, size.height * 0.14f),
                        end = Offset(center.x, center.y + size.height * 0.06f),
                        strokeWidth = strokeWidth
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════
// GAMEPAD INDICATOR
// ═══════════════════════════════════════════════════════════════════

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


// ═══════════════════════════════════════════════════════════════════
// CONTROLLER
// ═══════════════════════════════════════════════════════════════════

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
