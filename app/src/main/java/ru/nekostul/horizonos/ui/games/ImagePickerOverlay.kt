package ru.nekostul.horizonos.ui.games

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.files.FileEntry
import ru.nekostul.horizonos.ui.files.FileKind
import ru.nekostul.horizonos.ui.files.FileOperations
import ru.nekostul.horizonos.ui.files.FileSorting
import ru.nekostul.horizonos.ui.files.FileTheme
import ru.nekostul.horizonos.ui.files.ImageCropOverlay
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import java.io.File

@Composable
internal fun ImagePickerOverlay(
    title: String,
    onPick: (FileEntry) -> Unit,
    onDismiss: () -> Unit,
    cropSquare: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val inputMode = remember { mutableStateOf(SettingsInputMode.TOUCH) }
    val focusRequester = remember { FocusRequester() }

    var currentPath by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var cropSource by remember { mutableStateOf<String?>(null) }

    fun isImage(entry: FileEntry) = entry.kind == FileKind.IMAGE

    fun loadEntries() {
        val path = currentPath ?: return
        scope.launch {
            val raw = withContext(Dispatchers.IO) { FileOperations.listRoot(path) }
            entries = raw
                .filter { it.isDirectory || isImage(it) }
                .filter { !it.hidden }
                .sortedWith(FileSorting.byName)
            focusedIndex = 0
        }
    }

    fun open(entry: FileEntry) {
        if (entry.isDirectory) {
            currentPath = entry.path
            loadEntries()
        } else if (cropSquare) {
            cropSource = entry.path
        } else {
            onPick(entry)
        }
    }

    fun activateFocused() {
        if (entries.isNotEmpty() && focusedIndex in entries.indices) {
            inputMode.value = SettingsInputMode.GAMEPAD
            open(entries[focusedIndex])
        }
    }

    fun goUp() {
        val path = currentPath
        if (path == null || path == "/" || path == "/storage/emulated/0") {
            onDismiss()
        } else {
            val parent = File(path).parent
            if (parent != null) {
                currentPath = parent
                loadEntries()
            } else onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        val home = File("/storage/emulated/0")
        currentPath = if (home.exists()) "/storage/emulated/0" else "/"
        loadEntries()
        delay(80)
        focusRequester.requestFocus()
    }

    BackHandler(enabled = true) { goUp() }

    CompositionLocalProvider(LocalSettingsInputMode provides inputMode) {
        HorizonOverlay(
            title = title,
            onDismiss = onDismiss,
            onControllerBack = { goUp(); true },
            onFooterBack = { goUp() }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        inputMode.value = SettingsInputMode.GAMEPAD
                        if (isHorizonConfirmKey(event)) {
                            activateFocused()
                            true
                        } else when (event.key) {
                            Key.DirectionDown, Key.DirectionRight -> {
                                if (entries.isNotEmpty()) {
                                    focusedIndex = (focusedIndex + 1).coerceAtMost(entries.lastIndex)
                                }
                                true
                            }
                            Key.DirectionUp, Key.DirectionLeft -> {
                                focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                                true
                            }
                            else -> false
                        }
                    }
            ) {
                Text(
                    text = currentPath ?: "/",
                    color = FileTheme.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
                entries.forEachIndexed { index, entry ->
                    PickerRow(
                        entry = entry,
                        focused = focusedIndex == index,
                        inputMode = inputMode.value,
                        onClick = {
                            inputMode.value = SettingsInputMode.TOUCH
                            open(entry)
                        },
                        onFocus = { focusedIndex = index }
                    )
                }
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.picker_empty),
                        color = FileTheme.muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    cropSource?.let { path ->
        ImageCropOverlay(
            sourcePath = path,
            title = title,
            onConfirm = { file ->
                cropSource = null
                onPick(FileEntry(name = file.name, path = file.absolutePath, isDirectory = false))
            },
            onDismiss = { cropSource = null }
        )
    }
}

@Composable
private fun PickerRow(
    entry: FileEntry,
    focused: Boolean,
    inputMode: SettingsInputMode,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val active = inputMode == SettingsInputMode.GAMEPAD && focused
    var preview by remember(entry.path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.path) {
        if (entry.kind == FileKind.IMAGE) {
            preview = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(entry.path)?.asImageBitmap() }
        }
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) FileTheme.divider.copy(alpha = 0.5f) else Color.Transparent)
            .then(if (active) Modifier.border(2.dp, FileTheme.accent, RoundedCornerShape(6.dp)) else Modifier)
            .clickable {
                onFocus()
                onClick()
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)).background(FileTheme.pathBar),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = preview
            when {
                bitmap != null -> Image(bitmap, null, Modifier.size(40.dp), contentScale = ContentScale.Crop)
                entry.isDirectory -> Text("📁", color = FileTheme.accent, fontSize = 16.sp)
                else -> Text("IMG", color = FileTheme.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(entry.name, color = FileTheme.text, fontSize = 14.sp, maxLines = 1)
    }
}
