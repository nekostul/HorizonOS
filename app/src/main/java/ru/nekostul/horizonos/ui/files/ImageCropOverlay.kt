package ru.nekostul.horizonos.ui.files

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.HorizonXboxGlyph
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.hideDialogSystemBars
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Square image cropper used for covers and avatars. The user can zoom and pan
 * the image with the gamepad or touch, then confirm with A or cancel with B.
 * The resulting 1:1 PNG is written to the cache and returned through [onConfirm].
 */
@Composable
internal fun ImageCropOverlay(
    sourcePath: String,
    title: String,
    onConfirm: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var source by remember(sourcePath) { mutableStateOf<Bitmap?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableFloatStateOf(0f) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(sourcePath) {
        source = withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(sourcePath) }.getOrNull()
        }
        zoom = 1f
        panX = 0f
        panY = 0f
    }

    LaunchedEffect(Unit) {
        delay(80)
        focusRequester.requestFocus()
    }

    fun doConfirm() {
        val bmp = source ?: return
        if (saving || viewport <= 0f) return
        saving = true
        val out = cropToFile(bmp, viewport, zoom, panX, panY, context.cacheDir)
        saving = false
        if (out != null) onConfirm(out) else onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window != null) hideDialogSystemBars(window)
            onDispose { }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    if (HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode)) {
                        HorizonNavigation.requestHome()
                        return@onPreviewKeyEvent true
                    }
                    if (isHorizonConfirmKey(event)) {
                        doConfirm()
                        return@onPreviewKeyEvent true
                    }
                    val bmp = source
                    val scale = scaleFor(bmp, viewport, zoom)
                    when (event.key) {
                        Key.DirectionLeft -> {
                            panX = clampPan(viewport, (bmp?.width ?: 0) * scale, panX - 24f); true
                        }
                        Key.DirectionRight -> {
                            panX = clampPan(viewport, (bmp?.width ?: 0) * scale, panX + 24f); true
                        }
                        Key.DirectionUp -> {
                            panY = clampPan(viewport, (bmp?.height ?: 0) * scale, panY - 24f); true
                        }
                        Key.DirectionDown -> {
                            panY = clampPan(viewport, (bmp?.height ?: 0) * scale, panY + 24f); true
                        }
                        Key.ButtonR1, Key.ButtonR2 -> { zoom = (zoom + 0.1f).coerceAtMost(5f); true }
                        Key.ButtonL1, Key.ButtonL2 -> { zoom = (zoom - 0.1f).coerceAtLeast(1f); true }
                        Key.ButtonB, Key.Back -> { onDismiss(); true }
                        else -> false
                    }
                }
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, color = SettingsWhite, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    BoxWithConstraints(contentAlignment = Alignment.Center) {
                        val side = minOf(maxWidth, maxHeight)
                        Box(
                            Modifier
                                .size(side)
                                .clipToBounds()
                                .background(Color.Black)
                                .onSizeChanged { viewport = it.width.toFloat() }
                                .pointerInput(sourcePath) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        val bmp = source ?: return@detectTransformGestures
                                        zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                        val scale = scaleFor(bmp, viewport, zoom)
                                        panX = clampPan(viewport, bmp.width * scale, panX + pan.x)
                                        panY = clampPan(viewport, bmp.height * scale, panY + pan.y)
                                    }
                                }
                        ) {
                            val bmp = source
                            if (bmp != null) {
                                val image = remember(bmp) { bmp.asImageBitmap() }
                                Canvas(Modifier.fillMaxSize()) {
                                    val scale = scaleFor(bmp, size.width, zoom)
                                    val dw = bmp.width * scale
                                    val dh = bmp.height * scale
                                    val px = clampPan(size.width, dw, panX)
                                    val py = clampPan(size.height, dh, panY)
                                    drawImage(
                                        image = image,
                                        srcOffset = IntOffset.Zero,
                                        srcSize = IntSize(bmp.width, bmp.height),
                                        dstOffset = IntOffset(
                                            ((size.width - dw) / 2f + px).roundToInt(),
                                            ((size.height - dh) / 2f + py).roundToInt()
                                        ),
                                        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                                        filterQuality = FilterQuality.High
                                    )
                                }
                            } else {
                                Text(
                                    stringResource(R.string.games_media_apply_failed),
                                    color = SettingsWhite,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CropHint(
                        glyph = { HorizonButtonGlyph("B", size = 26.dp) },
                        label = stringResource(R.string.settings_action_back),
                        onClick = onDismiss
                    )
                    Spacer(Modifier.width(24.dp))
                    CropHint(
                        glyph = { HorizonButtonGlyph("A", size = 26.dp) },
                        label = stringResource(R.string.action_ok),
                        onClick = { doConfirm() }
                    )
                    Spacer(Modifier.width(24.dp))
                    CropHint(
                        glyph = { HorizonXboxGlyph(size = 26.dp) },
                        label = stringResource(R.string.files_home_hint),
                        onClick = { HorizonNavigation.requestHome() }
                    )
                }
            }
        }
    }
}

private fun clampPan(viewport: Float, displayed: Float, value: Float): Float {
    val limit = ((displayed - viewport) / 2f).coerceAtLeast(0f)
    return value.coerceIn(-limit, limit)
}

private fun scaleFor(bitmap: Bitmap?, viewport: Float, zoom: Float): Float {
    if (bitmap == null || viewport <= 0f) return 1f
    val base = max(viewport / bitmap.width, viewport / bitmap.height)
    return base * zoom
}

private fun cropToFile(
    bitmap: Bitmap,
    viewport: Float,
    zoom: Float,
    panX: Float,
    panY: Float,
    cacheDir: File
): File? {
    return runCatching {
        val scale = scaleFor(bitmap, viewport, zoom)
        val dw = bitmap.width * scale
        val dh = bitmap.height * scale
        val side = (viewport / scale).roundToInt().coerceAtLeast(1)
        val left = (((dw - viewport) / 2f - panX) / scale).roundToInt()
            .coerceIn(0, (bitmap.width - side).coerceAtLeast(0))
        val top = (((dh - viewport) / 2f - panY) / scale).roundToInt()
            .coerceIn(0, (bitmap.height - side).coerceAtLeast(0))
        val cropW = side.coerceAtMost(bitmap.width - left)
        val cropH = side.coerceAtMost(bitmap.height - top)
        val cropped = Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
        val target = File(cacheDir, "crop_${System.currentTimeMillis()}.png")
        FileOutputStream(target).use { stream ->
            cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (cropped !== bitmap) cropped.recycle()
        target
    }.getOrNull()
}

@Composable
private fun CropHint(
    glyph: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .height(44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        glyph()
        Spacer(Modifier.width(7.dp))
        Text(label, color = SettingsWhite, fontSize = 16.sp)
    }
}
