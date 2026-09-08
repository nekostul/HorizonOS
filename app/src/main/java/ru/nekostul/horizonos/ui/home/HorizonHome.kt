package ru.nekostul.horizonos.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.mutableStateListOf
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.hardware.input.InputManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.InputDevice
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import ru.nekostul.horizonos.R
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.animation.core.animateFloat
import ru.nekostul.horizonos.ui.settings.LauncherSettingsScreen
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.exp
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Job

private val HorizonBackground = Color(0xFF2B2B2B)
private val HorizonBlue = Color(0xFF00C8FF)
private val HorizonWhite = Color(0xFFF2F2F2)
private val HorizonGray = Color(0xFF686868)
private const val HomeCardSlotCount = 12

private data class HorizonGame(
    val title: String,
    val color: Color
)


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
fun HorizonHome(onRequestPermissions: (Array<String>) -> Unit = {}) {

    val context = LocalContext.current

    val currentTime = CurrentTime()
    val battery = BatteryLevel()
    val externalGamepadConnected = ExternalGamepadConnected()

    // The Home library is intentionally a fixed strip of 12 empty slots for
    // now. Game discovery and card contents will be added separately later.
    val games = remember {
        mutableStateListOf<HorizonGame>()
    }

    val slotCount = HomeCardSlotCount

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

    var selectedMenu by remember {
        mutableIntStateOf(0)
    }

    var menuSelectionArmed by remember {
        mutableStateOf(false)
    }

    var showPowerMenu by remember {
        mutableStateOf(false)
    }

    var showLauncherSettings by remember {
        mutableStateOf(false)
    }

    fun clearHomeSelection() {
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = -1
    }

    fun moveGameSelection(direction: Int) {
        selectedGame = (selectedGame + direction + slotCount) % slotCount
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = selectedGame
        gamepadNavigationRequest++
    }

    fun moveMenuSelection(direction: Int) {
        selectedMenu = (selectedMenu + direction + 5) % 5
        menuSelectionArmed = true
        tappedGameIndex = -1
    }

    fun moveDownToMenu() {
        selectedMenu = 0
        menuSelectionArmed = true
        tappedGameIndex = -1
    }

    fun moveUpToGames() {
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = selectedGame
    }

    fun activateMenu(index: Int) {

        if (selectedMenu != index || !menuSelectionArmed) {
            selectedMenu = index
            menuSelectionArmed = true
            tappedGameIndex = -1
            return
        }

        // A completed action starts a fresh selection cycle when the user
        // returns to Home.
        menuSelectionArmed = false

        when (index) {

            // Игры
            0 -> {
                // Уже главный экран
            }

            // Файлы
            1 -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                context.startActivity(intent)
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

    fun selectGame(index: Int) {
        val wasSelected = selectedGame == index && tappedGameIndex == index
        selectedGame = index
        selectedMenu = -1
        menuSelectionArmed = false
        tappedGameIndex = index

        if (wasSelected) {
            // The second tap is reserved for opening the game. There are no
            // game entries yet, so empty slots intentionally remain inert.
            games.getOrNull(index)?.let { /* Game launch will be wired to the library. */ }
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
            .onPreviewKeyEvent { event ->

                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
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

                    Key.Enter, Key.NumPadEnter -> {
                        if (menuSelectionArmed && selectedMenu >= 0) {
                            activateMenu(selectedMenu)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(h * 0.042f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(h * 0.083f),
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

            Spacer(Modifier.height(h * 0.141f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardSize),
                contentAlignment = Alignment.CenterStart
            ) {
                HorizonGameCarousel(
                    games = games,
                    slotCount = slotCount,
                    selectedIndex = selectedGame,
                    selectedTitle = games.getOrNull(selectedGame)?.title,
                    selectionActive = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
                    showSelectedTitle = tappedGameIndex == selectedGame && tappedGameIndex >= 0,
                    cardSize = cardSize,
                    cardGap = cardGap,
                    startOffset = cardStartOffset,
                    onCardTap = ::selectGame,
                    onSelectedIndexChange = {
                        selectedGame = it
                    },
                    scrollPositionPx = homeScrollPositionPx,
                    onScrollPositionChange = { homeScrollPositionPx = it },
                    gamepadNavigationRequest = gamepadNavigationRequest,
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
                HorizonMenuButton("▣", Color(0xFFFF0033), h * 0.105f, menuSelectionArmed && selectedMenu == 0, menuSelectionArmed && selectedMenu == 0, stringResource(R.string.home_menu_games)) { activateMenu(0) }
                HorizonMenuButton("▤", Color(0xFF35D060), h * 0.105f, menuSelectionArmed && selectedMenu == 1, menuSelectionArmed && selectedMenu == 1, stringResource(R.string.home_menu_files)) { activateMenu(1) }
                HorizonMenuButton("▥", Color(0xFF20BFFF), h * 0.105f, menuSelectionArmed && selectedMenu == 2, menuSelectionArmed && selectedMenu == 2, stringResource(R.string.home_menu_gamesir)) { activateMenu(2) }
                HorizonMenuButton("☼", HorizonWhite, h * 0.105f, menuSelectionArmed && selectedMenu == 3, menuSelectionArmed && selectedMenu == 3, stringResource(R.string.home_menu_settings)) { activateMenu(3) }
                HorizonMenuButton("⏻", HorizonWhite, h * 0.105f, menuSelectionArmed && selectedMenu == 4, menuSelectionArmed && selectedMenu == 4, stringResource(R.string.home_menu_power)) { activateMenu(4) }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFAAAAAA))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.096f)
                    .padding(horizontal = 24.dp),
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
                        fill = Color(0xFF777777),
                        contentColor = HorizonBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_ok),
                        color = Color(0xFF777777),
                        fontSize = 18.sp
                    )
                }
            }
        }
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
        }
    }
}


// ═══════════════════════════════════════════════════════════════════
// EMPTY GAME CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun HorizonGameCarousel(
    games: List<HorizonGame>,
    slotCount: Int,
    selectedIndex: Int,
    selectedTitle: String?,
    selectionActive: Boolean,
    showSelectedTitle: Boolean,
    cardSize: Dp,
    cardGap: Dp,
    startOffset: Dp,
    onCardTap: (Int) -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
    scrollPositionPx: Float,
    onScrollPositionChange: (Float) -> Unit,
    gamepadNavigationRequest: Int,
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
        if (showSelectedTitle && selectedTitle != null) {
            Text(
                text = selectedTitle,
                color = HorizonBlue,
                fontSize = 23.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-34).dp)
            )
        }

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
                    modifier = Modifier.requiredSize(cardSize),
                    contentAlignment = Alignment.Center
                ) {
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
private fun HorizonEmptyGameCard(
    selected: Boolean,
    size: Dp,
    selectionPulse: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val frameColor = if (selected) {
        lerp(HorizonBlue, Color(0xFF9EEFFF), selectionPulse)
    } else {
        Color(0xFF333333)
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
                drawRect(color = Color(0xFF303030))
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
    game: HorizonGame,
    selected: Boolean,
    size: Dp,
    selectionPulse: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val frameColor = if (selected) {
        lerp(HorizonBlue, Color(0xFF9EEFFF), selectionPulse)
    } else {
        Color(0xFF333333)
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
                drawRect(color = game.color)
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

        Text(
            text = game.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
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
            .background(Color(0xFF444444))
            .border(
                width = 2.dp,
                color = Color(0xFF686868),
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

@Composable
private fun HorizonMenuButton(
    symbol: String,
    symbolColor: Color,
    size: Dp,
    selected: Boolean = false,
    showLabel: Boolean = false,
    label: String = "",
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
    Box(
        modifier = Modifier
            .width(size)
            .height(if (showLabel) size + 30.dp else size)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredSize(size)
                .clip(CircleShape)
                .background(Color(0xFF444444))
                .border(
                    width = 2.dp,
                    color = if (selected) {
                        HorizonBlue.copy(alpha = selectionAlpha)
                    } else {
                        Color(0xFF686868)
                    },

                    shape = CircleShape
                )
                .clickable {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = symbolColor,
                fontSize = (size.value * 0.32f).sp
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
