package ru.nekostul.horizonos.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.view.InputDevice
import android.view.KeyEvent
import kotlin.math.roundToInt
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.isExternalGamepadConnected
import ru.nekostul.horizonos.ui.keyboard.KeyboardLanguage
import kotlin.math.max

internal class HorizonKeyboardImeView(
    context: Context,
    private val onAction: (Action) -> Unit
) : View(context) {
    enum class Action { BACKSPACE, RETURN, CONFIRM, CANCEL, SHIFT, LANGUAGE, MODE, SPACE, MOVE_LEFT, MOVE_RIGHT, TEXT }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val keyRect = RectF()
    private val hitRects = mutableListOf<Pair<RectF, ActionData>>()
    private var language = KeyboardLanguage.EN
    private var darkTheme = true
    private var symbols = false
    private var shift = 0
    private var selectedRow = 0
    private var selectedColumn = 0
    private var lastStickHorizontal = 0
    private var lastStickVertical = 0

    private data class ActionData(
        val action: Action,
        val value: String = "",
        val row: Int = -1,
        val column: Int = -1
    )

    private val darkSurface = Color.rgb(75, 73, 76)
    private val darkKey = Color.rgb(89, 87, 90)
    private val darkDivider = Color.rgb(70, 68, 71)
    private val lightSurface = Color.rgb(240, 240, 240)
    private val lightKey = Color.rgb(224, 224, 224)
    private val lightDivider = Color.rgb(196, 196, 196)
    private val accent = Color.rgb(0, 220, 220)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isFocusable) requestFocus()
    }

    fun acquireGamepadFocus() {
        isFocusable = true
        isFocusableInTouchMode = true
        if (!hasFocus()) requestFocus()
    }

    fun releaseGamepadFocus() {
        clearFocus()
        isFocusable = false
        isFocusableInTouchMode = false
        lastStickHorizontal = 0
        lastStickVertical = 0
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (!isFocusable) return super.onKeyDown(keyCode, event)
        if (event.repeatCount > 0 && keyCode !in dpadKeyCodes) return true
        when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> moveSelection(-1, 0)
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> moveSelection(1, 0)
            android.view.KeyEvent.KEYCODE_DPAD_UP -> moveSelection(0, -1)
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> moveSelection(0, 1)
            android.view.KeyEvent.KEYCODE_BUTTON_A,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER -> activateSelected()
            android.view.KeyEvent.KEYCODE_BUTTON_B,
            android.view.KeyEvent.KEYCODE_BACK -> onAction(Action.CANCEL)
            android.view.KeyEvent.KEYCODE_BUTTON_X -> onAction(Action.BACKSPACE)
            android.view.KeyEvent.KEYCODE_BUTTON_Y -> activate(ActionData(Action.SPACE))
            android.view.KeyEvent.KEYCODE_BUTTON_START -> onAction(Action.CONFIRM)
            android.view.KeyEvent.KEYCODE_BUTTON_L1 -> onAction(Action.MOVE_LEFT)
            android.view.KeyEvent.KEYCODE_BUTTON_R1 -> onAction(Action.MOVE_RIGHT)
            android.view.KeyEvent.KEYCODE_BUTTON_THUMBL -> activate(ActionData(Action.SHIFT))
            android.view.KeyEvent.KEYCODE_BUTTON_THUMBR -> {
                activate(ActionData(Action.LANGUAGE))
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        invalidate()
        return true
    }

    override fun onKeyUp(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (!isFocusable) return super.onKeyUp(keyCode, event)
        return if (keyCode in gamepadKeyCodes) true else super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!isFocusable) return super.onGenericMotionEvent(event)
        val source = event.source
        val isController =
            (source and InputDevice.SOURCE_JOYSTICK) != 0 ||
                (source and InputDevice.SOURCE_GAMEPAD) != 0
        if (!isController || event.action != MotionEvent.ACTION_MOVE) {
            return super.onGenericMotionEvent(event)
        }
        val horizontal = axisDirection(
            event.getAxisValue(MotionEvent.AXIS_HAT_X)
                .takeUnless { kotlin.math.abs(it) < 0.01f }
                ?: event.getAxisValue(MotionEvent.AXIS_X)
        )
        val vertical = axisDirection(
            event.getAxisValue(MotionEvent.AXIS_HAT_Y)
                .takeUnless { kotlin.math.abs(it) < 0.01f }
                ?: event.getAxisValue(MotionEvent.AXIS_Y)
        )
        val consumed = horizontal != 0 || vertical != 0 || lastStickHorizontal != 0 || lastStickVertical != 0
        if (horizontal != lastStickHorizontal && horizontal != 0) moveSelection(horizontal, 0)
        if (vertical != lastStickVertical && vertical != 0) moveSelection(0, vertical)
        lastStickHorizontal = horizontal
        lastStickVertical = vertical
        return consumed
    }

    private fun axisDirection(value: Float): Int = when {
        value <= -0.55f -> -1
        value >= 0.55f -> 1
        else -> 0
    }

    private val dpadKeyCodes = setOf(
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN
    )

    private val gamepadKeyCodes = dpadKeyCodes + setOf(
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_THUMBL,
        KeyEvent.KEYCODE_BUTTON_THUMBR
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec).takeIf { it > 0 }
            ?: resources.displayMetrics.widthPixels
        val desiredHeight = (measuredWidth * 0.282f).roundToInt()
            .coerceAtLeast((96f * density).roundToInt())
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        val measuredHeight = if (availableHeight > 0) desiredHeight.coerceAtMost(availableHeight) else desiredHeight
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun setLanguage(value: KeyboardLanguage) {
        language = value
        invalidate()
    }

    fun setDarkTheme(value: Boolean) {
        darkTheme = value
        invalidate()
    }

    private fun keyRows(): List<String> = listOf(
        "1234567890-",
        if (symbols) "@#$%&-+()=/" else if (language == KeyboardLanguage.RU) "йцукенгшщзх" else "qwertyuiop/",
        if (symbols) "*\"':;!?.,<>" else if (language == KeyboardLanguage.RU) "фывапролджэ" else "asdfghjkl:'",
        if (symbols) "~_\\|[]{}^`" else if (language == KeyboardLanguage.RU) "ячсмитьбю,." else "zxcvbnm,.?!"
    )

    private fun rowLength(row: Int): Int = if (row == 4) 4 else 11

    private fun moveSelection(horizontal: Int, vertical: Int) {
        selectedRow = (selectedRow + vertical).coerceIn(0, 4)
        selectedColumn = if (horizontal != 0) {
            (selectedColumn + horizontal).coerceIn(0, rowLength(selectedRow) - 1)
        } else {
            selectedColumn.coerceIn(0, rowLength(selectedRow) - 1)
        }
        invalidate()
    }

    private fun selectedAction(): ActionData {
        if (selectedRow == 4) {
            return when (selectedColumn) {
                0 -> ActionData(Action.LANGUAGE, row = 4, column = 0)
                1 -> ActionData(Action.SHIFT, row = 4, column = 1)
                2 -> ActionData(Action.MODE, row = 4, column = 2)
                else -> ActionData(Action.SPACE, row = 4, column = 3)
            }
        }
        val raw = keyRows()[selectedRow][selectedColumn].toString()
        val value = if (shift > 0 && raw.first().isLetter()) raw.uppercase() else raw
        return ActionData(Action.TEXT, value, selectedRow, selectedColumn)
    }

    private fun activateSelected() = activate(selectedAction())

    private fun activate(data: ActionData) {
        when (data.action) {
            Action.TEXT -> {
                textAction = data.value
                onAction(Action.TEXT)
                if (shift == 1) shift = 0
            }
            Action.SPACE -> onAction(Action.SPACE)
            Action.SHIFT -> shift = (shift + 1) % 3
            Action.LANGUAGE -> {
                language = if (language == KeyboardLanguage.EN) KeyboardLanguage.RU else KeyboardLanguage.EN
                onAction(Action.LANGUAGE)
            }
            Action.MODE -> {
                symbols = !symbols
                onAction(Action.MODE)
            }
            else -> onAction(data.action)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val surface = if (darkTheme) darkSurface else lightSurface
        val key = if (darkTheme) darkKey else lightKey
        val divider = if (darkTheme) darkDivider else lightDivider
        val text = if (darkTheme) Color.WHITE else Color.rgb(34, 34, 34)
        val muted = if (darkTheme) Color.rgb(208, 206, 209) else Color.rgb(85, 85, 85)

        canvas.drawColor(surface)
        hitRects.clear()

        val w = width.toFloat()
        val h = height.toFloat()
        val horizontal = w * 0.041f
        val footerTop = h * 0.83f
        val top = max(8f * density, footerTop * 0.035f)
        val bottom = max(8f * density, footerTop * 0.035f)
        val gap = max(2f * density, w * 0.0015f)
        val sideWidth = w * 0.10f
        val mainWidth = w - horizontal * 2f - sideWidth - gap
        val rowGap = gap
        val rowHeight = (footerTop - top - bottom - rowGap * 4f) / 5f

        val mainRows = keyRows()

        mainRows.forEachIndexed { row, labels ->
            val y = top + row * (rowHeight + rowGap)
            drawEqualRow(canvas, labels, row, horizontal, y, mainWidth, rowHeight, gap, key, divider, text)
        }

        val controlY = top + 4f * (rowHeight + rowGap)
        val controlWeights = listOf(1f, 1f, 1f, 8f)
        val totalWeight = controlWeights.sum()
        var x = horizontal
        controlWeights.forEachIndexed { index, weight ->
            val cellWidth = (mainWidth - gap * 3f) * weight / totalWeight
            val rect = RectF(x, controlY, x + cellWidth, controlY + rowHeight)
            drawKey(canvas, rect, key, divider, text, when (index) {
                0 -> ActionData(Action.LANGUAGE)
                1 -> ActionData(Action.SHIFT)
                2 -> ActionData(Action.MODE)
                else -> ActionData(Action.SPACE)
            }.copy(row = 4, column = index), muted)
            x += cellWidth + gap
        }

        val sideX = horizontal + mainWidth + gap
        val sideRects = listOf(
            RectF(sideX, top, sideX + sideWidth, top + rowHeight * 1f + rowGap * 0f),
            RectF(sideX, top + rowHeight + rowGap, sideX + sideWidth, top + rowHeight * 3f + rowGap * 2f),
            RectF(sideX, top + rowHeight * 3f + rowGap * 3f, sideX + sideWidth, footerTop - bottom)
        )
        drawSideKey(canvas, sideRects[0], Color.WHITE, darkSurface, ActionData(Action.BACKSPACE), "X", muted)
        drawSideKey(canvas, sideRects[1], key, text, ActionData(Action.RETURN), if (language == KeyboardLanguage.RU) "Ввод" else "Return", muted)
        drawSideKey(canvas, sideRects[2], accent, Color.rgb(34, 34, 34), ActionData(Action.CONFIRM), if (language == KeyboardLanguage.RU) "ОК" else "OK", muted)
        drawFooter(canvas, footerTop, h, text, muted, surface)
    }

    private fun drawFooter(canvas: Canvas, top: Float, bottom: Float, text: Int, muted: Int, surface: Int) {
        val footerHeight = bottom - top
        val cy = top + footerHeight / 2f
        val controlHeight = footerHeight * 0.62f
        var x = width * 0.44f
        val gap = width * 0.012f

        fun addControl(widthFraction: Float, action: Action, draw: (Float, Float) -> Unit) {
            val controlWidth = width * widthFraction
            val rect = RectF(x, top, x + controlWidth, bottom)
            draw(cy, controlHeight)
            hitRects += rect to ActionData(action)
            x += controlWidth + gap
        }

        if (isExternalGamepadConnected()) {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.game)
            val iconSize = footerHeight * 0.62f
            paint.colorFilter = PorterDuffColorFilter(text, PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(18f * density, cy - iconSize / 2f, 18f * density + iconSize, cy + iconSize / 2f),
                paint
            )
            paint.colorFilter = null
        }

        addControl(0.075f, Action.MOVE_LEFT) { center, size ->
            drawBumper(canvas, x + width * 0.018f, center, size * 0.58f, "LB", text)
            drawCentered(canvas, "←", RectF(x + width * 0.043f, center - size / 2f, x + width * 0.075f, center + size / 2f), size * 0.58f, text)
        }
        addControl(0.075f, Action.MOVE_RIGHT) { center, size ->
            drawBumper(canvas, x + width * 0.018f, center, size * 0.58f, "RB", text)
            drawCentered(canvas, "→", RectF(x + width * 0.043f, center - size / 2f, x + width * 0.075f, center + size / 2f), size * 0.58f, text)
        }
        addControl(0.12f, Action.SHIFT) { center, size ->
            drawStick(canvas, x + width * 0.018f, center, size * 0.58f, text)
            drawCentered(canvas, if (language == KeyboardLanguage.RU) "Регистр" else "Shift", RectF(x + width * 0.038f, center - size / 2f, x + width * 0.12f, center + size / 2f), size * 0.52f, text)
        }
        addControl(0.12f, Action.CANCEL) { center, size ->
            drawButton(canvas, x + width * 0.018f, center, size * 0.58f, "B", text, surface)
            drawCentered(canvas, if (language == KeyboardLanguage.RU) "Отмена" else "Cancel", RectF(x + width * 0.038f, center - size / 2f, x + width * 0.12f, center + size / 2f), size * 0.52f, text)
        }
        addControl(0.12f, Action.CONFIRM) { center, size ->
            drawButton(canvas, x + width * 0.018f, center, size * 0.58f, "A", text, surface)
            drawCentered(canvas, if (language == KeyboardLanguage.RU) "Ввод" else "Enter", RectF(x + width * 0.038f, center - size / 2f, x + width * 0.12f, center + size / 2f), size * 0.52f, text)
        }
    }

    private fun drawEqualRow(
        canvas: Canvas,
        labels: String,
        row: Int,
        startX: Float,
        y: Float,
        rowWidth: Float,
        rowHeight: Float,
        gap: Float,
        key: Int,
        divider: Int,
        text: Int
    ) {
        val cellWidth = (rowWidth - gap * (labels.length - 1)) / labels.length
        labels.forEachIndexed { index, raw ->
            val x = startX + index * (cellWidth + gap)
            val value = if (shift > 0 && raw.isLetter()) raw.uppercase() else raw.toString()
            drawKey(
                canvas,
                RectF(x, y, x + cellWidth, y + rowHeight),
                key,
                divider,
                text,
                ActionData(Action.TEXT, value, row, index),
                text
            )
        }
    }

    private fun drawKey(
        canvas: Canvas,
        rect: RectF,
        fill: Int,
        divider: Int,
        text: Int,
        data: ActionData,
        muted: Int
    ) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRect(rect, paint)
        paint.style = Paint.Style.STROKE
        val selected = data.row == selectedRow && data.column == selectedColumn
        paint.strokeWidth = if (selected) max(3f, density * 1.25f) else max(1f, density)
        paint.color = if (selected) accent else divider
        canvas.drawRect(rect, paint)
        paint.style = Paint.Style.FILL
        paint.color = text
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        when (data.action) {
            Action.TEXT -> drawCentered(canvas, data.value, rect, max(16f * density, rect.height() * 0.31f), text)
            Action.SPACE -> {
                drawCentered(canvas, if (language == KeyboardLanguage.RU) "Пробел" else "Space", rect, 18f * density, muted)
                drawBadge(canvas, "Y", rect.right - 8f * density, rect.top + 8f * density, muted, fill)
            }
            Action.LANGUAGE -> drawGlobe(canvas, rect.centerX(), rect.centerY(), rect.height() * 0.28f, text)
            Action.SHIFT -> {
                drawShift(canvas, rect.centerX(), rect.centerY(), rect.height() * 0.27f, if (shift == 0) text else accent)
                paint.color = muted
                canvas.drawCircle(rect.left + 8f * density, rect.top + 8f * density, 3f * density, paint)
            }
            Action.MODE -> drawCentered(canvas, "#+=", rect, 16f * density, text)
            else -> Unit
        }
        hitRects += RectF(rect) to data
    }

    private fun drawSideKey(canvas: Canvas, rect: RectF, fill: Int, text: Int, data: ActionData, label: String, muted: Int) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawRect(rect, paint)
        paint.color = text
        when (data.action) {
            Action.BACKSPACE -> {
                drawBackspace(canvas, rect.centerX(), rect.centerY(), rect.height() * 0.22f, text)
                drawBadge(canvas, label, rect.right - 7f * density, rect.top + 7f * density, text, fill)
            }
            Action.CONFIRM -> {
                drawStartGlyph(canvas, rect.right - 7f * density, rect.top + 7f * density, 6f * density, Color.rgb(34, 34, 34), accent)
                drawCentered(canvas, label, rect, 20f * density, Color.rgb(34, 34, 34))
            }
            else -> drawCentered(canvas, label, rect, 20f * density, muted)
        }
        hitRects += RectF(rect) to data
    }

    private fun drawCentered(canvas: Canvas, value: String, rect: RectF, size: Float, color: Int) {
        paint.color = color
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(value, rect.centerX(), baseline, paint)
    }

    private fun drawBadge(canvas: Canvas, label: String, x: Float, y: Float, fill: Int, text: Int) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        val radius = 6f * density
        canvas.drawCircle(x, y, radius, paint)
        drawCentered(canvas, label, RectF(x - radius, y - radius, x + radius, y + radius), 8f * density, text)
    }

    private fun drawStartGlyph(canvas: Canvas, cx: Float, cy: Float, radius: Float, fill: Int, content: Int) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = content
        val barWidth = radius * 1.05f
        val barHeight = max(1f, radius * 0.16f)
        val gap = radius * 0.22f
        val left = cx - barWidth / 2f
        val top = cy - (barHeight * 3f + gap * 2f) / 2f
        repeat(3) { index ->
            canvas.drawRoundRect(
                RectF(left, top + index * (barHeight + gap), left + barWidth, top + index * (barHeight + gap) + barHeight),
                barHeight / 2f,
                barHeight / 2f,
                paint
            )
        }
    }

    private fun drawBumper(canvas: Canvas, cx: Float, cy: Float, height: Float, label: String, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, height * 0.10f)
        paint.color = color
        val width = height * 1.62f
        canvas.drawRoundRect(
            RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f),
            height * 0.16f,
            height * 0.16f,
            paint
        )
        drawCentered(canvas, label, RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f), height * 0.43f, color)
        paint.style = Paint.Style.FILL
    }

    private fun drawStick(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, size * 0.10f)
        paint.color = color
        canvas.drawCircle(cx, cy + size * 0.12f, size * 0.30f, paint)
        canvas.drawLine(cx, cy - size * 0.36f, cx, cy - size * 0.02f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy - size * 0.43f, size * 0.14f, paint)
    }

    private fun drawButton(canvas: Canvas, cx: Float, cy: Float, radius: Float, label: String, fill: Int, text: Int) {
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawCircle(cx, cy, radius, paint)
        drawCentered(canvas, label, RectF(cx - radius, cy - radius, cx + radius, cy + radius), radius * 1.05f, text)
    }

    private fun drawGlobe(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f * density, r * 0.10f)
        paint.color = color
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawOval(RectF(cx - r * 0.42f, cy - r, cx + r * 0.42f, cy + r), paint)
        canvas.drawLine(cx - r, cy, cx + r, cy, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawShift(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f * density, size * 0.10f)
        paint.color = color
        val path = android.graphics.Path().apply {
            moveTo(cx, cy - size)
            lineTo(cx - size * 0.82f, cy - size * 0.08f)
            lineTo(cx - size * 0.35f, cy - size * 0.08f)
            lineTo(cx - size * 0.35f, cy + size * 0.78f)
            lineTo(cx + size * 0.35f, cy + size * 0.78f)
            lineTo(cx + size * 0.35f, cy - size * 0.08f)
            lineTo(cx + size * 0.82f, cy - size * 0.08f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawBackspace(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f * density, size * 0.11f)
        paint.color = color
        val path = android.graphics.Path().apply {
            moveTo(cx - size, cy)
            lineTo(cx - size * 0.45f, cy - size * 0.75f)
            lineTo(cx + size, cy - size * 0.75f)
            lineTo(cx + size, cy + size * 0.75f)
            lineTo(cx - size * 0.45f, cy + size * 0.75f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawLine(cx - size * 0.15f, cy - size * 0.35f, cx + size * 0.55f, cy + size * 0.35f, paint)
        canvas.drawLine(cx + size * 0.55f, cy - size * 0.35f, cx - size * 0.15f, cy + size * 0.35f, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val hit = hitRects.asReversed().firstOrNull { it.first.contains(event.x, event.y) } ?: return true
        if (hit.second.row >= 0 && hit.second.column >= 0) {
            selectedRow = hit.second.row
            selectedColumn = hit.second.column
        }
        when (hit.second.action) {
            Action.TEXT, Action.SPACE, Action.SHIFT, Action.LANGUAGE, Action.MODE -> activate(hit.second)
            else -> onAction(hit.second.action)
        }
        return true
    }

    private var textAction: String = ""

    fun consumeTextAction(): String = textAction.also { textAction = "" }
}
