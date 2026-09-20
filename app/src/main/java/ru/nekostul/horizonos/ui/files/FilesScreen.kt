package ru.nekostul.horizonos.ui.files

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.HorizonStartGlyph
import ru.nekostul.horizonos.ui.HorizonXboxGlyph
import ru.nekostul.horizonos.ui.isExternalGamepadConnected
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound
import ru.nekostul.horizonos.ui.settings.*
import ru.nekostul.horizonos.ui.keyboard.HorizonKeyboardDialog

enum class FilesMode { BROWSE, PICK_FOLDER, PICK_FILE }

@Composable
fun FilesScreen(
    onDismiss: () -> Unit,
    mode: FilesMode = FilesMode.BROWSE,
    onFolderPicked: ((String) -> Unit)? = null,
    onFilePicked: ((FileEntry) -> Unit)? = null,
    allowedExtensions: Set<String> = emptySet(),
    titleOverride: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val inputMode = remember {
        mutableStateOf(
            if (isExternalGamepadConnected()) SettingsInputMode.GAMEPAD
            else SettingsInputMode.TOUCH
        )
    }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val pickFolderMode = mode == FilesMode.PICK_FOLDER
    val pickFileMode = mode == FilesMode.PICK_FILE
    val pickerMode = pickFolderMode || pickFileMode

    var currentPath by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var sortMode by remember { mutableStateOf(FileSortMode.NAME) }
    var showHidden by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var busyText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var clipboard by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var clipboardMove by remember { mutableStateOf(false) }

    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmCall by remember { mutableStateOf<(() -> Unit)?>(null) }
    var propsEntry by remember { mutableStateOf<FileEntry?>(null) }
    var propsSize by remember { mutableStateOf(-1L) }
    var propsCount by remember { mutableStateOf(-1) }
    var showSortOverlay by remember { mutableStateOf(false) }
    var showNewMenu by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileEntry?>(null) }
    var createKind by remember { mutableStateOf<Int?>(null) }
    var rootWarningShow by remember { mutableStateOf(false) }
    var rootWarningPath by remember { mutableStateOf<String?>(null) }

    var searching by remember { mutableStateOf(false) }
    var showSearchKeyboard by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FileSearch.SearchHit>>(emptyList()) }

    var rootAvailable by remember { mutableStateOf(RootHelper.cachedRoot == true) }
    LaunchedEffect(Unit) {
        if (!rootAvailable) {
            rootAvailable = withContext(Dispatchers.IO) { RootHelper.isRootAvailable() }
        }
    }

    fun refresh() {
        val path = currentPath ?: return
        scope.launch {
            val raw = withContext(Dispatchers.IO) { FileOperations.listRoot(path) }
            var list = raw
            if (!showHidden) list = list.filter { !it.hidden }
            entries = when (sortMode) {
                FileSortMode.NAME -> list.sortedWith(FileSorting.byName)
                FileSortMode.SIZE -> list.sortedWith(FileSorting.bySize)
                FileSortMode.DATE -> list.sortedWith(FileSorting.byDate)
            }
        }
    }

    suspend fun loadEntries() {
        val path = currentPath ?: return
        val raw = withContext(Dispatchers.IO) { FileOperations.listRoot(path) }
        var list = raw
        if (!showHidden) list = list.filter { !it.hidden }
        entries = when (sortMode) {
            FileSortMode.NAME -> list.sortedWith(FileSorting.byName)
            FileSortMode.SIZE -> list.sortedWith(FileSorting.bySize)
            FileSortMode.DATE -> list.sortedWith(FileSorting.byDate)
        }
    }

    fun openEntry(entry: FileEntry) {
        if (!entry.isDirectory) return
        val path = entry.path
        if (rootAvailable && isSystemRoot(path) && rootWarningPath != path) {
            rootWarningShow = true
            rootWarningPath = path
            return
        }
        currentPath = path
        focusedIndex = 0
        scope.launch { loadEntries() }
    }

    fun openFile(entry: FileEntry) {
        scope.launch {
            when (val result = withContext(Dispatchers.IO) {
                FileOpenHandler.open(context, File(entry.path), entry.kind)
            }) {
                is FileOpenResult.Success -> Unit
                is FileOpenResult.Failed -> error = result.message
            }
        }
    }

    fun moveGridFocus(columns: Int, deltaRows: Int = 0, deltaColumns: Int = 0) {
        if (entries.isEmpty()) return
        val next = (focusedIndex + deltaRows * columns + deltaColumns)
            .coerceIn(0, entries.lastIndex)
        focusedIndex = next
        val info = gridState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == next }
        if (item == null) {
            scope.launch { gridState.animateScrollToItem(next) }
            return
        }
        val top = item.offset.y
        val height = item.size.height
        val delta = when {
            top < info.viewportStartOffset -> top - info.viewportStartOffset
            top + height > info.viewportEndOffset -> top + height - info.viewportEndOffset
            else -> 0
        }
        if (delta != 0) scope.launch { gridState.animateScrollBy(delta.toFloat()) }
    }

    fun pickCurrentFolder() {
        val path = currentPath ?: return
        onFolderPicked?.invoke(path)
        onDismiss()
    }

    fun goUp() {
        val path = currentPath
        if (path == null || path == "/" || path == "/storage/emulated/0") {
            onDismiss()
        } else {
            val parent = File(path).parent
            if (parent != null) {
                currentPath = parent
                focusedIndex = 0
                scope.launch {
                    loadEntries()
                    gridState.scrollToItem(0)
                }
            } else onDismiss()
        }
    }

    fun pickFocusedFile() {
        val e = entries.getOrNull(focusedIndex) ?: return
        if (e.isDirectory) return
        if (allowedExtensions.isNotEmpty() && e.extension.lowercase() !in allowedExtensions) return
        onFilePicked?.invoke(e)
    }

    fun isSelectableFile(entry: FileEntry): Boolean =
        !entry.isDirectory &&
            (allowedExtensions.isEmpty() || entry.extension.lowercase() in allowedExtensions)

    fun activateFocused() {
        if (entries.isEmpty() || focusedIndex !in entries.indices) return
        val e = entries[focusedIndex]
        inputMode.value = SettingsInputMode.GAMEPAD
        when {
            selected.isNotEmpty() -> selected =
                if (e.path in selected) selected - e.path else selected + e.path
            e.isDirectory -> openEntry(e)
            pickFileMode -> pickFocusedFile()
            pickFolderMode -> Unit
            else -> openFile(e)
        }
    }

    fun toggleSelect(entry: FileEntry) {
        selected = if (entry.path in selected) selected - entry.path else selected + entry.path
    }

    fun doCopySelected() {
        val sel = entries.filter { it.path in selected }
        if (sel.isEmpty()) return
        clipboard = sel
        clipboardMove = false
        selected = emptySet()
    }

    fun doCutSelected() {
        val sel = entries.filter { it.path in selected }
        if (sel.isEmpty()) return
        clipboard = sel
        clipboardMove = true
        selected = emptySet()
    }

    fun doPaste() {
        val path = currentPath ?: return
        val items = clipboard.toList()
        scope.launch {
            busy = true
            busyText = if (clipboardMove) "Перемещение…" else "Копирование…"
            val dest = File(path)
            withContext(Dispatchers.IO) {
                items.forEach { item ->
                    if (clipboardMove) FileOperations.move(File(item.path), dest)
                    else FileOperations.copy(File(item.path), dest)
                }
            }
            clipboard = emptyList()
            clipboardMove = false
            busy = false
            refresh()
        }
    }

    fun doDeleteSelected() {
        val sel = entries.filter { it.path in selected }
        scope.launch {
            busy = true
            busyText = "Удаление…"
            withContext(Dispatchers.IO) {
                sel.forEach { FileOperations.delete(File(it.path)) }
            }
            busy = false
            selected = emptySet()
            refresh()
        }
    }

    fun showProperties(entry: FileEntry) {
        propsEntry = entry
        propsSize = -1
        propsCount = -1
        if (entry.isDirectory) {
            scope.launch {
                val size = withContext(Dispatchers.IO) { FileOperations.sizeOf(File(entry.path)) }
                val count = withContext(Dispatchers.IO) { FileOperations.itemCount(File(entry.path)) }
                if (propsEntry?.path == entry.path) {
                    propsSize = size
                    propsCount = count
                }
            }
        }
    }

    LaunchedEffect(query, searching) {
        if (!searching || query.isBlank()) { searchResults = emptyList(); return@LaunchedEffect }
        searchResults = withContext(Dispatchers.IO) {
            FileSearch.search(File(currentPath ?: "/storage/emulated/0"), query)
        }
    }

    val overlayOpen = confirmTitle != null || propsEntry != null || showSortOverlay ||
        showNewMenu || createKind != null || renameTarget != null || rootWarningShow || showSearchKeyboard
    LaunchedEffect(overlayOpen) {
        if (!overlayOpen) {
            delay(80)
            listFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(Unit) {
        val roots = File("/storage/emulated/0")
        currentPath = if (roots.exists()) "/storage/emulated/0" else "/"
        loadEntries()
        delay(40)
        listFocusRequester.requestFocus()
    }

    LaunchedEffect(currentPath, entries) {
        if (searching) return@LaunchedEffect
        focusedIndex = focusedIndex.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
        delay(30)
        listFocusRequester.requestFocus()
    }

    BackHandler(enabled = true) {
        when {
            confirmTitle != null -> confirmTitle = null
            propsEntry != null -> propsEntry = null
              showSearchKeyboard -> showSearchKeyboard = false
              searching -> { searching = false; query = "" }
            showNewMenu -> showNewMenu = false
            selected.isNotEmpty() -> selected = emptySet()
            else -> goUp()
        }
    }

    CompositionLocalProvider(LocalSettingsInputMode provides inputMode) {
        Box(
            Modifier
                .fillMaxSize()
                .background(FileTheme.background)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode)
                    ) {
                        HorizonNavigation.requestHome()
                        true
                    } else false
                }
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleOverride ?: stringResource(R.string.files_title),
                        color = FileTheme.text,
                        fontSize = 25.sp
                    )
                    Spacer(Modifier.weight(1f))
                    HorizonFilesIconButton("+") { showNewMenu = true }
                    Spacer(Modifier.width(10.dp))
                    HorizonFilesIconButton("⇅") { showSortOverlay = true }
                    Spacer(Modifier.width(10.dp))
                    HorizonFilesIconButton("🔍") {
                        if (searching) {
                            searching = false
                            showSearchKeyboard = false
                        } else {
                            searching = true
                            showSearchKeyboard = true
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(FileTheme.pathBar, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentPath ?: "/",
                        color = FileTheme.muted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${entries.size}", color = FileTheme.muted, fontSize = 12.sp)
                }
                if (searching) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FileTheme.pathBar, RoundedCornerShape(6.dp))
                            .clickable {
                                inputMode.value = SettingsInputMode.TOUCH
                                showSearchKeyboard = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = query.ifEmpty { stringResource(R.string.files_search_hint) },
                            color = if (query.isEmpty()) FileTheme.muted else FileTheme.text,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                if (searching && query.isNotBlank()) {
                    LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                        itemsIndexed(searchResults) { index, hit ->
                            FileRow(
                                entry = hit.entry,
                                selected = hit.entry.path in selected,
                                focused = focusedIndex == index && inputMode.value == SettingsInputMode.GAMEPAD,
                                inputMode = inputMode.value,
                                subtitle = "${hit.entry.sizeLabel()} · ${hit.parent}",
                                onClick = { openFile(hit.entry) },
                                onFocus = { focusedIndex = index }
                            )
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
                        val cellMinWidth = 160.dp
                        val columns = (maxWidth / cellMinWidth).toInt().coerceAtLeast(1)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = gridState,
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(listFocusRequester)
                                .focusable()
                                .onKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) false
                                    else if (event.key == Key.DirectionUp || event.key == Key.DirectionDown ||
                                        event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                                    ) {
                                        LauncherAudioManager.play(
                                            LauncherSound.CLICK,
                                            LauncherInputSource.GAMEPAD
                                        )
                                        when (event.key) {
                                            Key.DirectionRight -> { moveGridFocus(columns, deltaColumns = 1); true }
                                            Key.DirectionLeft -> { moveGridFocus(columns, deltaColumns = -1); true }
                                            Key.DirectionDown -> { moveGridFocus(columns, deltaRows = 1); true }
                                            else -> { moveGridFocus(columns, deltaRows = -1); true }
                                        }
                                    } else if (isHorizonConfirmKey(event)) {
                                        activateFocused()
                                        true
                                    } else when (event.key) {
                                        Key.ButtonX -> {
                                            if (!pickerMode && entries.isNotEmpty() && focusedIndex in entries.indices) {
                                                inputMode.value = SettingsInputMode.GAMEPAD
                                                toggleSelect(entries[focusedIndex])
                                            }
                                            true
                                        }
                                        Key.ButtonB -> {
                                            if (selected.isNotEmpty()) selected = emptySet() else goUp()
                                            true
                                        }
                                        Key.ButtonStart -> {
                                            if (pickFolderMode) pickCurrentFolder()
                                            else if (pickFileMode) pickFocusedFile()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                        ) {
                            gridItemsIndexed(entries, key = { _, entry -> entry.path }) { index, entry ->
                                FileGridItem(
                                    entry = entry,
                                    selected = entry.path in selected,
                                    focused = focusedIndex == index,
                                    inputMode = inputMode.value,
                                    onClick = {
                                        inputMode.value = SettingsInputMode.TOUCH
                                        when {
                                            !pickerMode && selected.isNotEmpty() -> toggleSelect(entry)
                                            entry.isDirectory -> openEntry(entry)
                                            pickFileMode -> {
                                                if (isSelectableFile(entry)) onFilePicked?.invoke(entry)
                                            }
                                            pickerMode -> Unit
                                            else -> openFile(entry)
                                        }
                                    },
                                    onFocus = { focusedIndex = index }
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!pickerMode) {
                    if (selected.isNotEmpty()) {
                        Text(
                            text = "${selected.size}",
                            color = FileTheme.accent,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        HorizonFilesTextButton(stringResource(R.string.files_copy)) { doCopySelected() }
                        Spacer(Modifier.width(6.dp))
                        HorizonFilesTextButton(stringResource(R.string.files_cut)) { doCutSelected() }
                        Spacer(Modifier.width(6.dp))
                        HorizonFilesTextButton(stringResource(R.string.files_delete)) {
                            confirmTitle = context.getString(R.string.files_delete_title)
                            confirmMessage = context.getString(R.string.files_delete_message, selected.size)
                            confirmCall = { doDeleteSelected() }
                        }
                        if (selected.size == 1) {
                            val e = entries.firstOrNull { it.path in selected }
                            if (e != null) {
                                Spacer(Modifier.width(6.dp))
                                HorizonFilesTextButton("Свойства") { showProperties(e) }
                                Spacer(Modifier.width(6.dp))
                                HorizonFilesTextButton("Переименовать") { renameTarget = e }
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        HorizonFilesTextButton(stringResource(R.string.files_cancel)) { selected = emptySet() }
                    } else {
                        if (clipboard.isNotEmpty()) {
                            HorizonFilesTextButton(stringResource(R.string.files_paste)) { doPaste() }
                            Spacer(Modifier.width(6.dp))
                        }
                        HorizonFilesTextButton(stringResource(R.string.files_new)) { showNewMenu = true }
                        if (rootAvailable) {
                            Spacer(Modifier.width(8.dp))
                            Text("ROOT", color = FileTheme.accent, fontSize = 12.sp)
                        }
                    }
                    }
                    val focusedEntry = entries.getOrNull(focusedIndex)
                    if (pickFileMode && focusedEntry != null &&
                        !focusedEntry.isDirectory && !isSelectableFile(focusedEntry)
                    ) {
                        Text(
                            text = stringResource(R.string.games_rom_unsupported),
                            color = Color(0xFFFF6070),
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (pickFolderMode) {
                        FileFooterAction(
                            glyph = { HorizonStartGlyph(size = 18.dp, fill = FileTheme.text, contentColor = FileTheme.background) },
                            label = stringResource(R.string.files_pick_folder),
                            onClick = { pickCurrentFolder() }
                        )
                        Spacer(Modifier.width(16.dp))
                    } else if (pickFileMode && focusedEntry != null && isSelectableFile(focusedEntry)) {
                        FileFooterAction(
                            glyph = { HorizonStartGlyph(size = 18.dp, fill = FileTheme.text, contentColor = FileTheme.background) },
                            label = stringResource(R.string.files_pick_rom),
                            onClick = { pickFocusedFile() }
                        )
                        Spacer(Modifier.width(16.dp))
                    }
                    FileFooterAction(
                        glyph = { HorizonButtonGlyph("B", size = 18.dp, fill = FileTheme.text, contentColor = FileTheme.background) },
                        label = stringResource(R.string.settings_action_back),
                        onClick = { goUp() }
                    )
                    Spacer(Modifier.width(14.dp))
                    FileFooterAction(
                        glyph = { HorizonButtonGlyph("A", size = 18.dp, fill = FileTheme.text, contentColor = FileTheme.background) },
                        label = stringResource(R.string.action_ok),
                        onClick = { activateFocused() }
                    )
                    Spacer(Modifier.width(14.dp))
                    FileFooterAction(
                        glyph = { HorizonXboxGlyph(size = 18.dp, fill = FileTheme.text, contentColor = FileTheme.background) },
                        label = stringResource(R.string.files_home_hint),
                        onClick = { HorizonNavigation.requestHome() }
                    )
                }
            }

            if (busy) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Text(busyText, color = FileTheme.text, fontSize = 18.sp)
                }
            }
        }
    }

    if (rootWarningShow) {
        HorizonOverlay(
            title = stringResource(R.string.files_root_warning_title),
            onDismiss = { rootWarningShow = false }
        ) {
            Text(
                stringResource(R.string.files_root_warning_body),
                color = FileTheme.text,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.files_root_warning_go),
                selected = true,
                onClick = {
                    rootWarningShow = false
                    val p = entries.find { it.isDirectory && it.path == rootWarningPath }?.path
                        ?: rootWarningPath
                    if (p != null) {
                        currentPath = p
                        focusedIndex = 0
                        scope.launch { loadEntries() }
                    }
                }
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.files_cancel),
                selected = false,
                onClick = { rootWarningShow = false }
            )
        }
    }

    propsEntry?.let { entry ->
        PropertiesOverlay(
            entry = entry,
            size = propsSize,
            count = propsCount,
            onDismiss = { propsEntry = null }
        )
    }

    if (showSortOverlay) {
        SortOverlay(
            current = sortMode,
            onSelect = { mode ->
                sortMode = mode
                showSortOverlay = false
                refresh()
            },
            onDismiss = { showSortOverlay = false }
        )
    }

    if (showNewMenu) {
        NewItemOverlay(
            title = stringResource(R.string.files_new_title),
            onFolder = { showNewMenu = false; createKind = 0 },
            onFile = { showNewMenu = false; createKind = 1 },
            onCancel = { showNewMenu = false }
        )
    }

    createKind?.let { kind ->
        TextInputOverlay(
            title = stringResource(R.string.files_new_title),
            initial = if (kind == 0) context.getString(R.string.files_new_folder_default)
            else context.getString(R.string.files_new_file_default),
            onConfirm = { name ->
                val path = currentPath
                createKind = null
                if (path != null) scope.launch {
                    withContext(Dispatchers.IO) {
                        if (kind == 0) FileOperations.createFolder(File(path), name)
                        else FileOperations.createFile(File(path), name)
                    }
                    refresh()
                }
            },
            onCancel = { createKind = null }
        )
    }

    renameTarget?.let { entry ->
        TextInputOverlay(
            title = "Переименовать",
            initial = entry.name,
            onConfirm = { name ->
                renameTarget = null
                scope.launch {
                    withContext(Dispatchers.IO) { FileOperations.rename(File(entry.path), name) }
                    refresh()
                }
            },
            onCancel = { renameTarget = null }
        )
    }

    if (showSearchKeyboard) {
        HorizonKeyboardDialog(
            title = stringResource(R.string.files_search_hint),
            initialValue = query,
            onConfirm = { value -> query = value; showSearchKeyboard = false },
            onCancel = { showSearchKeyboard = false }
        )
    }

    confirmTitle?.let { title ->
        HorizonOverlay(
            title = title,
            onDismiss = { confirmTitle = null }
        ) {
            Text(
                confirmMessage,
                color = FileTheme.text,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            HorizonOverlayChoice(title = stringResource(R.string.files_cancel), selected = false, onClick = { confirmTitle = null })
            HorizonOverlayChoice(
                title = stringResource(R.string.files_delete_confirm),
                selected = true,
                onClick = {
                    confirmTitle = null
                    confirmCall?.invoke()
                }
            )
        }
    }

    error?.let { msg ->
        HorizonOverlay(title = stringResource(R.string.files_error_title), onDismiss = { error = null }) {
            Text(msg, color = FileTheme.text, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            HorizonOverlayChoice(title = stringResource(R.string.action_ok), selected = true, onClick = { error = null })
        }
    }
}

@Composable
private fun FileFooterAction(
    glyph: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    Row(
        modifier = Modifier
            .height(40.dp)
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        glyph()
        Spacer(Modifier.width(6.dp))
        Text(label, color = FileTheme.text, fontSize = 14.sp)
    }
}
