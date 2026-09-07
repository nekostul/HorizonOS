package ru.nekostul.horizonos.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import ru.nekostul.horizonos.ui.settings.LauncherSettingsScreen

private val HorizonBackground = Color(0xFF2B2B2B)
private val HorizonBlue = Color(0xFF00C8FF)
private val HorizonWhite = Color(0xFFF2F2F2)
private val HorizonGray = Color(0xFF686868)

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
    var currentTime by remember {
        mutableStateOf(
            SimpleDateFormat(
                "h:mm a",
                Locale.US
            ).format(Date())
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat(
                "h:mm a",
                Locale.US
            ).format(Date())

            delay(1000)
        }
    }

    return currentTime
}

@Composable
fun HorizonHome() {

    val context = LocalContext.current

    val currentTime = CurrentTime()
    val battery = BatteryLevel()

    // Пока игр нет — карусель всё равно показывает 12 пустых слотов.
    // Позже сюда можно добавлять реальные игры, и слоты будут
    // автоматически заполняться, а после 12 игр карусель расширится.
    val games = remember {
        mutableStateListOf<HorizonGame>()
    }

    val slotCount = maxOf(12, games.size)

    var selectedGame by remember {
        mutableIntStateOf(0)
    }

    var selectedMenu by remember {
        mutableIntStateOf(0)
    }

    var showPowerMenu by remember {
        mutableStateOf(false)
    }

    var showLauncherSettings by remember {
        mutableStateOf(false)
    }

    fun activateMenu(index: Int) {

        selectedMenu = index

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

    if (showLauncherSettings) {
        LauncherSettingsScreen(
            onBack = {
                showLauncherSettings = false
            }
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
                        selectedGame =
                            (selectedGame - 1 + slotCount) % slotCount
                        true
                    }

                    Key.DirectionRight -> {
                        selectedGame =
                            (selectedGame + 1) % slotCount
                        true
                    }

                    else -> false
                }
            }
    ) {

        val h = maxHeight

        /*
         * Основные отступы интерфейса.
         *
         * Сейчас специально оставляем много воздуха
         * по краям, как в референсе Switch.
         */

        val sidePadding = h * 0.14f
        val topPadding = h * 0.09f
        val bottomPadding = h * 0.055f

        /*
         * Размер квадратной обложки.
         */

        val cardSize = h * 0.36f
        val cardGap = h * 0.022f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = topPadding,
                    bottom = bottomPadding
                )
        ) {

            // =========================================================
            // TOP BAR
            // =========================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    horizontalAlignment = Alignment.Start
                ) {

                    ProfileIcon(
                        size = h * 0.075f
                    )

                    Spacer(
                        modifier = Modifier.height(
                            h * 0.012f
                        )
                    )

                    Text(
                        text = "Eden's Page",
                        color = HorizonBlue,
                        fontSize = (h.value * 0.036f).sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = currentTime,
                        color = HorizonWhite,
                        fontSize = (h.value * 0.054f).sp
                    )

                    Spacer(
                        modifier = Modifier.width(
                            h * 0.020f
                        )
                    )

                    Text(
                        text = "Ω",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.070f).sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.width(
                            h * 0.020f
                        )
                    )

                    Text(
                        text = "$battery%",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.050f).sp
                    )

                    Spacer(
                        modifier = Modifier.width(
                            h * 0.02f
                        )
                    )

                    BatteryIcon(
                        width = h * 0.050f,
                        height = h * 0.032f
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    h * 0.075f
                )
            )

            // =========================================================
            // GAME CAROUSEL
            // =========================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {

                /*
                 * Вычисляем положение всей карусели.
                 *
                 * При смене selectedGame лента смещается,
                 * а выбранная игра становится активной.
                 */

                val selectedOffset =
                    (sidePadding.value * 0.30f -
                            selectedGame * (cardSize.value + cardGap.value)).dp

                Row(
                    modifier = Modifier.offset(
                        x = selectedOffset,
                        y = -(h * 0.030f)
                    ),
                    horizontalArrangement = Arrangement.spacedBy(
                        cardGap
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    repeat(slotCount) { index ->

                        val game = games.getOrNull(index)

                        if (game != null) {
                            HorizonGameCard(
                                game = game,
                                selected = index == selectedGame,
                                size = cardSize,
                                onClick = {
                                    selectedGame = index
                                }
                            )
                        } else {
                            HorizonEmptyGameCard(
                                selected = index == selectedGame,
                                size = cardSize,
                                onClick = {
                                    selectedGame = index
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================
            // SYSTEM MENU
            // =========================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(h * 0.115f)
                    .offset(y = -(h * 0.025f)),
                horizontalArrangement = Arrangement.spacedBy(
                    13.dp,
                    Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Игры
                HorizonMenuButton(
                    symbol = "▣",
                    symbolColor = Color(0xFFFF0033),
                    selected = selectedMenu == 0,
                    size = h * 0.125f,
                    onClick = {
                        activateMenu(0)
                    }
                )

                HorizonMenuButton(
                    symbol = "▤",
                    symbolColor = Color(0xFF35D060),
                    selected = selectedMenu == 1,
                    size = h * 0.125f,
                    onClick = {
                        activateMenu(1)
                    }
                )

                HorizonMenuButton(
                    symbol = "▥",
                    symbolColor = Color(0xFF20BFFF),
                    selected = selectedMenu == 2,
                    size = h * 0.125f,
                    onClick = {
                        activateMenu(2)
                    }
                )

                HorizonMenuButton(
                    symbol = "☼",
                    symbolColor = HorizonWhite,
                    selected = selectedMenu == 3,
                    size = h * 0.125f,
                    onClick = {
                        activateMenu(3)
                    }
                )

                HorizonMenuButton(
                    symbol = "⏻",
                    symbolColor = HorizonWhite,
                    selected = selectedMenu == 4,
                    size = h * 0.125f,
                    onClick = {
                        activateMenu(4)
                    }
                )
            }

            // =========================================================
            // DIVIDER
            // =========================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Color(0xFFAAAAAA)
                    )
            )

            // =========================================================
            // BOTTOM AREA
            // =========================================================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        h * 0.075f
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // -----------------------------------------------------
                // PAGE INDICATOR + CONTROLLER
                // -----------------------------------------------------

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    PageIndicator(
                        selected = selectedGame,
                        total = slotCount
                    )

                    Spacer(
                        modifier = Modifier.width(
                            h * 0.010f
                        )
                    )

                    ControllerIcon(
                        size = h * 0.055f
                    )
                }

                // -----------------------------------------------------
                // A OK
                // -----------------------------------------------------

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(
                                h * 0.040f
                            )
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                HorizonWhite,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "A",
                            color = HorizonWhite,
                            fontSize = (h.value * 0.024f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(
                            h * 0.010f
                        )
                    )

                    Text(
                        text = "OK",
                        color = HorizonWhite,
                        fontSize = (h.value * 0.032f).sp
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════════════
// EMPTY GAME CARD
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun HorizonEmptyGameCard(
    selected: Boolean,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(1.dp))
            .background(HorizonBackground)
            .border(
                width = if (selected) 4.dp else 2.dp,
                color = if (selected) HorizonBlue else Color(0xFF333333),
                shape = RoundedCornerShape(1.dp)
            )
            .clickable {
                onClick()
            }
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
    onClick: () -> Unit
) {

    val animatedSize by animateDpAsState(
        targetValue = size,
        animationSpec = tween(120),
        label = "cardSize"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "selectionPulse")

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = if (selected) 0.35f else 1f,
        targetValue = if (selected) 1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val borderColor = if (selected) {
        HorizonBlue.copy(alpha = borderAlpha)
    } else {
        Color(0xFF333333)
    }

    Box(
        modifier = Modifier
            .size(animatedSize)
            .clip(
                RoundedCornerShape(1.dp)
            )
            .background(game.color)
            .border(
                width = if (selected) 4.dp else 2.dp,
                color = if (selected) {
                    HorizonBlue.copy(alpha = borderAlpha)
                } else {
                    Color(0xFF333333)
                },
                shape = RoundedCornerShape(1.dp)
            )
            .clickable {
                onClick()
            },
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
    height: Dp
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .border(
                    2.dp,
                    HorizonWhite
                )
        )

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(
                    height * 0.45f
                )
                .background(
                    HorizonWhite
                )
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .requiredSize(size)
            .clip(CircleShape)
            .background(
                if (selected) {
                    symbolColor
                } else {
                    Color(0xFF444444)
                }
            )
            .border(
                width = 2.dp,
                color = if (selected) {
                    symbolColor
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
            color = if (selected) {
                HorizonWhite
            } else {
                symbolColor
            },
            fontSize = (size.value * 0.32f).sp
        )
    }
}


// ═══════════════════════════════════════════════════════════════════
// PAGE INDICATOR
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun PageIndicator(
    selected: Int,
    total: Int
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(
            4.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(total.coerceAtMost(4)) { index ->

            Box(
                modifier = Modifier
                    .size(
                        width = 10.dp,
                        height = 7.dp
                    )
                    .background(
                        if (index == selected % 4) {
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

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "🎮",
            fontSize = 25.sp
        )
    }
}