package ru.nekostul.horizonos.ui.keyboard

import android.content.Context
import android.content.ClipboardManager
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.nekostul.horizonos.ui.horizonLongPress
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonStartGlyph
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.isExternalGamepadConnected
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

enum class KeyboardLanguage { EN, RU }

private enum class KeyboardMode { LETTERS, SYMBOLS }

private enum class KeyboardAction { CHARACTER, BACKSPACE, SPACE, LANGUAGE, SHIFT, MODE }

private data class KeyboardItem(
    val label: String,
    val value: String = label,
    val action: KeyboardAction = KeyboardAction.CHARACTER,
    val weight: Float = 1f
)

private data class KeyboardRow(val items: List<KeyboardItem>)

private data class KeyboardPalette(
    val surface: Color,
    val key: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    val accent: Color
)

private fun palette(colors: ru.nekostul.horizonos.ui.theme.HorizonColorPalette): KeyboardPalette {
    val dark = colors.background.red < 0.5f
    return if (dark) {
        KeyboardPalette(
            surface = Color(0xFF4B494C),
            key = Color(0xFF59575A),
            divider = Color(0xFF464447),
            text = Color.White,
            muted = Color(0xFFD0CED1),
            accent = colors.accent
        )
    } else {
        KeyboardPalette(
            surface = Color(0xFFF0F0F0),
            key = Color(0xFFE0E0E0),
            divider = Color(0xFFC4C4C4),
            text = Color(0xFF222222),
            muted = Color(0xFF555555),
            accent = colors.accent
        )
    }
}

private fun label(language: KeyboardLanguage, english: String, russian: String): String =
    if (language == KeyboardLanguage.RU) russian else english

private fun letterRows(language: KeyboardLanguage): List<KeyboardRow> {
    val rows = if (language == KeyboardLanguage.RU) {
        listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю,.")
    } else {
        listOf("qwertyuiop/", "asdfghjkl:'", "zxcvbnm,.?!")
    }
    return rows.map { row -> KeyboardRow(row.map { KeyboardItem(it.toString()) }) }
}

private fun symbolRows(): List<KeyboardRow> = listOf(
    "@#$%&-+()=/",
    "*\"':;!?.,<>",
    "~_\\|[]{}^`"
).map { row -> KeyboardRow(row.map { KeyboardItem(it.toString()) }) }

private fun keyboardRows(language: KeyboardLanguage, mode: KeyboardMode): List<KeyboardRow> {
    val numberRow = KeyboardRow("1234567890-".map { KeyboardItem(it.toString()) })
    val controls = KeyboardRow(
        listOf(
            KeyboardItem("", action = KeyboardAction.LANGUAGE),
            KeyboardItem("", action = KeyboardAction.SHIFT),
            KeyboardItem("", action = KeyboardAction.MODE),
            KeyboardItem("", action = KeyboardAction.SPACE, weight = 8f)
        )
    )
    return listOf(numberRow) +
        (if (mode == KeyboardMode.LETTERS) letterRows(language) else symbolRows()) +
        controls
}

private fun moveColumn(row: Int, column: Int, rows: List<KeyboardRow>, delta: Int): Pair<Int, Int> {
    val safeRow = row.coerceIn(0, rows.lastIndex)
    val safeColumn = column.coerceIn(0, rows[safeRow].items.lastIndex)
    return safeRow to (safeColumn + delta).coerceIn(0, rows[safeRow].items.lastIndex)
}

private fun moveRow(row: Int, column: Int, rows: List<KeyboardRow>, delta: Int): Pair<Int, Int> {
    val newRow = (row + delta).coerceIn(0, rows.lastIndex)
    val newColumn = column.coerceIn(0, rows[newRow].items.lastIndex)
    return newRow to newColumn
}

private fun shiftText(value: String, shift: Int): String {
    if (shift == 0) return value
    return value.map { character ->
        if (character.isLetter()) character.uppercaseChar() else character
    }.joinToString("")
}

@Composable
private fun KeyboardGamepadConnected(): Boolean {
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
        inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        connected = isExternalGamepadConnected()
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    return connected
}

@Composable
fun HorizonKeyboardContent(
    initialValue: String,
    initialLanguage: KeyboardLanguage? = null,
    initialCursor: Int = initialValue.length,
    maxLength: Int? = null,
    onValueChange: ((String, Int) -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    showFooter: Boolean = true,
    title: String = ""
) {
    val colors = LocalHorizonColors.current
    val keyboardPalette = palette(colors)
    val context = LocalContext.current
    val systemLanguage = if (
        context.resources.configuration.locales[0].language == "ru" ||
        LocalConfiguration.current.locales[0].language == "ru"
    ) KeyboardLanguage.RU else KeyboardLanguage.EN
    val resolvedLanguage = initialLanguage ?: systemLanguage
    val limitedInitial = maxLength?.let { initialValue.take(it) } ?: initialValue
    var text by remember(initialValue, maxLength) { mutableStateOf(limitedInitial) }
    var cursor by remember(initialValue, initialCursor, maxLength) {
        mutableIntStateOf(initialCursor.coerceIn(0, limitedInitial.length))
    }
    var language by remember(resolvedLanguage) { mutableStateOf(resolvedLanguage) }
    var mode by remember { mutableStateOf(KeyboardMode.LETTERS) }
    var shift by remember { mutableIntStateOf(0) }
    var selectedRow by remember { mutableIntStateOf(0) }
    var selectedColumn by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val rows = keyboardRows(language, mode)
    val gamepadConnected = KeyboardGamepadConnected()

    fun publish() {
        onValueChange?.invoke(text, cursor)
    }

    fun insert(value: String) {
        val remaining = maxLength?.let { (it - text.length).coerceAtLeast(0) }
        if (remaining == 0) return
        val transformed = shiftText(value, shift).let { remaining?.let(it::take) ?: it }
        text = text.substring(0, cursor) + transformed + text.substring(cursor)
        cursor += transformed.length
        if (shift == 1) shift = 0
        publish()
    }

    fun backspace() {
        if (cursor <= 0) return
        text = text.removeRange(cursor - 1, cursor)
        cursor--
        publish()
    }

    fun cycleShift() {
        shift = (shift + 1) % 3
    }

    fun activate(item: KeyboardItem) {
        when (item.action) {
            KeyboardAction.CHARACTER -> insert(item.value)
            KeyboardAction.BACKSPACE -> backspace()
            KeyboardAction.SPACE -> insert(" ")
            KeyboardAction.LANGUAGE -> language = if (language == KeyboardLanguage.EN) KeyboardLanguage.RU else KeyboardLanguage.EN
            KeyboardAction.SHIFT -> cycleShift()
            KeyboardAction.MODE -> mode = if (mode == KeyboardMode.LETTERS) KeyboardMode.SYMBOLS else KeyboardMode.LETTERS
        }
    }

    fun pasteClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val pasted = clipboard?.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.replace('\r', ' ')
            ?.replace('\n', ' ')
            ?: return
        if (pasted.isNotEmpty()) insert(pasted)
    }

    fun resetKeyboard() {
        text = limitedInitial
        cursor = limitedInitial.length
        language = resolvedLanguage
        mode = KeyboardMode.LETTERS
        shift = 0
        selectedRow = 0
        selectedColumn = 0
        publish()
    }

    fun handleControllerKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val native = event.nativeKeyEvent.keyCode
        when {
            event.key == Key.DirectionLeft || event.key == Key.DirectionRight -> {
                val next = moveColumn(selectedRow, selectedColumn, rows, if (event.key == Key.DirectionLeft) -1 else 1)
                selectedRow = next.first
                selectedColumn = next.second
            }
            event.key == Key.DirectionUp || event.key == Key.DirectionDown -> {
                val next = moveRow(selectedRow, selectedColumn, rows, if (event.key == Key.DirectionUp) -1 else 1)
                selectedRow = next.first
                selectedColumn = next.second
            }
            event.key == Key.ButtonA || native == AndroidKeyEvent.KEYCODE_BUTTON_A -> {
                activate(rows[selectedRow].items[selectedColumn])
            }
            event.key == Key.ButtonB || event.key == Key.Back || native == AndroidKeyEvent.KEYCODE_BUTTON_B -> onCancel()
            event.key == Key.ButtonX || native == AndroidKeyEvent.KEYCODE_BUTTON_X -> backspace()
            event.key == Key.ButtonY || native == AndroidKeyEvent.KEYCODE_BUTTON_Y -> activate(rows.last().items.last())
            event.key == Key.ButtonL1 || native == AndroidKeyEvent.KEYCODE_BUTTON_L1 -> {
                cursor = (cursor - 1).coerceAtLeast(0)
                publish()
            }
            event.key == Key.ButtonR1 || native == AndroidKeyEvent.KEYCODE_BUTTON_R1 -> {
                cursor = (cursor + 1).coerceAtMost(text.length)
                publish()
            }
            native == AndroidKeyEvent.KEYCODE_BUTTON_THUMBL -> cycleShift()
            native == AndroidKeyEvent.KEYCODE_BUTTON_THUMBR -> {
                language = if (language == KeyboardLanguage.EN) KeyboardLanguage.RU else KeyboardLanguage.EN
            }
            event.key == Key.ButtonStart || native == AndroidKeyEvent.KEYCODE_BUTTON_START -> onConfirm(text)
            else -> return false
        }
        return true
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent(::handleControllerKey),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showHeader) {
            KeyboardHeader(
                modifier = Modifier.weight(0.444f),
                text = text,
                cursor = cursor,
                title = title,
                maxLength = maxLength,
                palette = keyboardPalette,
                onPaste = ::pasteClipboard
            )
        }
        KeyboardSurface(
            modifier = Modifier.weight(if (showHeader) 0.556f else 1f),
            language = language,
            rows = rows,
            selectedRow = selectedRow,
            selectedColumn = selectedColumn,
            shift = shift,
            palette = keyboardPalette,
            showFooter = showFooter,
            gamepadConnected = gamepadConnected,
            onKeyClick = { row, column ->
                selectedRow = row
                selectedColumn = column
                activate(rows[row].items[column])
            },
            onBackspace = ::backspace,
            onConfirm = { onConfirm(text) },
            onCancel = onCancel,
            onMoveLeft = {
                cursor = (cursor - 1).coerceAtLeast(0)
                publish()
            },
            onMoveRight = {
                cursor = (cursor + 1).coerceAtMost(text.length)
                publish()
            },
            onShift = ::cycleShift
        )
    }
}

@Composable
private fun KeyboardHeader(
    modifier: Modifier,
    text: String,
    cursor: Int,
    title: String,
    maxLength: Int?,
    palette: KeyboardPalette,
    onPaste: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val titleStart = maxWidth * 0.10f
        val dividerPadding = maxWidth * 0.022f
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(0.27f)
                    .padding(start = titleStart),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(title, color = palette.text, fontSize = 24.sp, fontWeight = FontWeight.Normal)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dividerPadding)
                    .height(1.dp)
                    .background(palette.text.copy(alpha = 0.16f))
            )
            Box(Modifier.fillMaxWidth().weight(0.73f), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth(0.40f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val textStyle = remember(palette.text) {
                            androidx.compose.ui.text.TextStyle(
                                color = palette.text,
                                fontSize = 38.sp
                            )
                        }
                        val textMeasurer = rememberTextMeasurer()
                        val measuredText = remember(text, textStyle) {
                            textMeasurer.measure(
                                text = AnnotatedString(text),
                                style = textStyle,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        var textLayout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
                        val scrollState = rememberScrollState()
                        val blink by rememberInfiniteTransition(label = "keyboardCursor").animateFloat(
                            initialValue = 1f,
                            targetValue = 0.08f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(520),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "keyboardCursorAlpha"
                        )
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .clipToBounds()
                                .horizonLongPress(onLongPress = onPaste),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val viewportWidthPx = with(density) { maxWidth.toPx() }
                            val cursorRect = measuredText
                                .getCursorRect(cursor.coerceIn(0, text.length))
                            LaunchedEffect(text, cursor, viewportWidthPx) {
                                scrollState.scrollTo(
                                    (cursorRect.right - viewportWidthPx)
                                        .coerceAtLeast(0f)
                                        .roundToInt()
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = text,
                                    style = textStyle,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.drawBehind {
                                        textLayout?.getCursorRect(cursor.coerceIn(0, text.length))?.let { rect ->
                                            drawLine(
                                                color = palette.text.copy(alpha = blink),
                                                start = androidx.compose.ui.geometry.Offset(rect.left, rect.top),
                                                end = androidx.compose.ui.geometry.Offset(rect.left, rect.bottom),
                                                strokeWidth = 2.dp.toPx()
                                            )
                                        }
                                    },
                                    onTextLayout = { textLayout = it }
                                )
                            }
                        }
                        if (maxLength != null) {
                            Text("${text.length}/$maxLength", color = palette.text, fontSize = 30.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(palette.text.copy(alpha = 0.88f)))
                }
            }
        }
    }
}

@Composable
private fun KeyboardSurface(
    modifier: Modifier,
    language: KeyboardLanguage,
    rows: List<KeyboardRow>,
    selectedRow: Int,
    selectedColumn: Int,
    shift: Int,
    palette: KeyboardPalette,
    showFooter: Boolean,
    gamepadConnected: Boolean,
    onKeyClick: (Int, Int) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onShift: () -> Unit
) {
    Column(modifier.fillMaxWidth().background(palette.surface)) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(if (showFooter) 0.83f else 1f)) {
            val horizontal = maxWidth * 0.041f
            val vertical = maxHeight * 0.041f
            Row(
                Modifier.fillMaxSize().padding(start = horizontal, end = horizontal, top = vertical),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Column(Modifier.weight(0.90f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    rows.forEachIndexed { rowIndex, row ->
                        KeyboardKeyRow(
                            row = row,
                            selected = rowIndex == selectedRow,
                            selectedColumn = selectedColumn,
                            shift = shift,
                            language = language,
                            palette = palette,
                            onClick = { onKeyClick(rowIndex, it) }
                        )
                    }
                }
                Column(Modifier.weight(0.10f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SideActionKey(1f, palette.text, palette.surface, onBackspace) {
                        HorizonBackspaceGlyph(color = palette.surface, size = 24.dp)
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            HorizonButtonGlyph("X", size = 12.dp, fill = palette.surface, contentColor = palette.text)
                        }
                    }
                    SideActionKey(2f, palette.key, palette.text, onConfirm) {
                        Text(label(language, "Return", "Ввод"), color = palette.muted, fontSize = 22.sp)
                    }
                    SideActionKey(2f, palette.accent, Color(0xFF222222), onConfirm) {
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            HorizonStartGlyph(size = 12.dp, fill = Color(0xFF222222), contentColor = palette.accent)
                        }
                        Text(label(language, "OK", "ОК"), color = Color(0xFF222222), fontSize = 24.sp)
                    }
                }
            }
        }
        if (showFooter) {
            KeyboardFooter(
                modifier = Modifier.fillMaxWidth().weight(0.17f),
                language = language,
                palette = palette,
                gamepadConnected = gamepadConnected,
                onCancel = onCancel,
                onMoveLeft = onMoveLeft,
                onMoveRight = onMoveRight,
                onShift = onShift,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun ColumnScope.KeyboardKeyRow(
    row: KeyboardRow,
    selected: Boolean,
    selectedColumn: Int,
    shift: Int,
    language: KeyboardLanguage,
    palette: KeyboardPalette,
    onClick: (Int) -> Unit
) {
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        row.items.forEachIndexed { index, item ->
            val active = selected && selectedColumn == index
            Box(
                Modifier
                    .weight(item.weight)
                    .fillMaxHeight()
                    .background(if (active) palette.surface else palette.key)
                    .border(if (active) 3.dp else 1.dp, if (active) palette.accent else palette.divider)
                    .clickable { onClick(index) },
                contentAlignment = Alignment.Center
            ) {
                when (item.action) {
                    KeyboardAction.LANGUAGE -> HorizonGlobeGlyph(28.dp, palette.text)
                    KeyboardAction.SHIFT -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(7.dp)
                                    .width(7.dp)
                                    .height(7.dp)
                                    .background(palette.muted, androidx.compose.foundation.shape.CircleShape)
                            )
                            HorizonShiftGlyph(30.dp, if (shift == 0) palette.text else palette.accent)
                        }
                    }
                    KeyboardAction.MODE -> Text("#+=", color = palette.text, fontSize = 18.sp)
                    KeyboardAction.SPACE -> {
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                label(language, "Space", "Пробел"),
                                color = palette.muted,
                                fontSize = 19.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                                HorizonButtonGlyph("Y", size = 12.dp, fill = palette.muted, contentColor = palette.key)
                            }
                        }
                    }
                    KeyboardAction.CHARACTER -> Text(shiftText(item.label, shift), color = palette.text, fontSize = 29.sp)
                    KeyboardAction.BACKSPACE -> Unit
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SideActionKey(
    weight: Float,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .weight(weight)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun KeyboardFooter(
    modifier: Modifier,
    language: KeyboardLanguage,
    palette: KeyboardPalette,
    gamepadConnected: Boolean,
    onCancel: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onShift: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (gamepadConnected) {
            Image(
                painter = painterResource(R.drawable.game),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                colorFilter = ColorFilter.tint(palette.text)
            )
        }
        Spacer(Modifier.weight(1f))
        FooterControl(Modifier.padding(horizontal = 7.dp), onClick = onMoveLeft) {
            HorizonBumperGlyph("LB", color = palette.text, size = 24.dp)
            Text("←", color = palette.text, fontSize = 19.sp)
        }
        FooterControl(Modifier.padding(horizontal = 7.dp), onClick = onMoveRight) {
            HorizonBumperGlyph("RB", color = palette.text, size = 24.dp)
            Text("→", color = palette.text, fontSize = 19.sp)
        }
        FooterControl(Modifier.padding(horizontal = 7.dp), onClick = onShift) {
            HorizonLeftStickGlyph(19.dp, palette.text)
            Text(label(language, "Shift", "Регистр"), color = palette.text, fontSize = 15.sp)
        }
        FooterControl(Modifier.padding(horizontal = 7.dp), onClick = onCancel) {
            HorizonButtonGlyph("B", size = 17.dp, fill = palette.text, contentColor = palette.surface)
            Text(label(language, "Cancel", "Отмена"), color = palette.text, fontSize = 15.sp)
        }
        FooterControl(Modifier.padding(horizontal = 7.dp), onClick = onConfirm) {
            HorizonButtonGlyph("A", size = 17.dp, fill = palette.text, contentColor = palette.surface)
            Text(label(language, "Enter", "Ввод"), color = palette.text, fontSize = 15.sp)
        }
    }
}

@Composable
private fun RowScope.FooterControl(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
fun HorizonKeyboardDialog(
    title: String = "",
    initialValue: String,
    initialLanguage: KeyboardLanguage? = null,
    maxLength: Int? = null,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler(onBack = onCancel)
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        androidx.compose.runtime.DisposableEffect(window) {
            window?.let {
                it.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                it.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT)
                WindowCompat.setDecorFitsSystemWindows(it, false)
                WindowInsetsControllerCompat(it, it.decorView).apply {
                    hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            onDispose { }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f))) {
            HorizonKeyboardContent(
                modifier = Modifier.fillMaxSize(),
                initialValue = initialValue,
                initialLanguage = initialLanguage,
                maxLength = maxLength,
                onConfirm = onConfirm,
                onCancel = onCancel,
                title = title
            )
        }
    }
}
