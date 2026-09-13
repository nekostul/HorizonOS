package ru.nekostul.horizonos.ui.games

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.SettingsBlue
import ru.nekostul.horizonos.ui.settings.SelectionFrameBlue
import ru.nekostul.horizonos.ui.settings.SelectionPulseDurationMillis
import ru.nekostul.horizonos.ui.settings.SettingsDivider
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsOverlayPanel
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.hideDialogSystemBars
import ru.nekostul.horizonos.ui.settings.launcher.scanning.GameMetadataEditor
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaType
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperRepository
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

private const val LibraryPage = 0
private const val PlatformPage = 1
private const val EmulatorPage = 2
private const val SourcePage = 3
private const val ConfirmPage = 4
private const val BiosWarningPage = 5
private const val AndroidAppPage = 6
private const val BiosWarningPreferences = "game_bios_warnings"

private enum class RomSource {
    FOLDER
}

@Composable
fun GamesScreen(
    onDismiss: () -> Unit,
    onOpenFolder: (((Uri?) -> Unit) -> Unit),
    onGamesAdded: (List<Game>) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = remember { GameLibrary(context) }
    val games by library.games.collectAsState(initial = emptyList())
    val scanner = remember { GameScanner(context) }
    val folderRepository = remember { GameFolderRepository(context) }
    val focusRequester = remember { FocusRequester() }

    var page by remember { mutableIntStateOf(LibraryPage) }
    var focusIndex by remember { mutableIntStateOf(0) }
    var selectedLibraryIndex by remember { mutableIntStateOf(0) }
    var selectedPlatform by remember { mutableStateOf(Platform.PLAYSTATION_1) }
    var selectedEmulator by remember { mutableStateOf(Emulator.DUCKSTATION) }
    var selectedSource by remember { mutableStateOf(RomSource.FOLDER) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var duplicateFolder by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var detailsGame by remember { mutableStateOf<Game?>(null) }
    var titleEditorGame by remember { mutableStateOf<Game?>(null) }
    var scanMenuGame by remember { mutableStateOf<Game?>(null) }
    var customCoverGame by remember { mutableStateOf<Game?>(null) }
    var customScreenshotGame by remember { mutableStateOf<Game?>(null) }
    val metadataEditor = remember { GameMetadataEditor(context.filesDir) }
    val androidApps = remember { AndroidAppRepository(context) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var appsLoading by remember { mutableStateOf(false) }
    val inputMode = remember { mutableStateOf(SettingsInputMode.TOUCH) }
    val gamesScrollState = rememberScrollState()
    val density = LocalDensity.current

    fun applyGameUpdate(updated: Game) {
        scope.launch { library.update(updated) }
        if (detailsGame?.id == updated.id) detailsGame = updated
    }

    fun persistPermission(uri: Uri, flags: Int) {
        val persistableFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, persistableFlags)
        }
    }

    fun resetToLibrary() {
        page = LibraryPage
        focusIndex = 0
        selectedLibraryIndex = selectedLibraryIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0))
        pendingUri = null
        pendingName = ""
    }

    fun openGameDetails(game: Game) {
        selectedLibraryIndex = games.indexOf(game).coerceAtLeast(0)
        detailsGame = game
    }

    fun isBiosWarningShown(emulator: Emulator): Boolean = context
        .getSharedPreferences(BiosWarningPreferences, 0)
        .getBoolean(emulator.name, false)

    fun markBiosWarningShown(emulator: Emulator) {
        context.getSharedPreferences(BiosWarningPreferences, 0)
            .edit()
            .putBoolean(emulator.name, true)
            .apply()
    }

    fun addPendingGame() {
        val uri = pendingUri ?: return
        if (isWorking) return
        isWorking = true
        message = null
        scope.launch {
            val scan = withContext(Dispatchers.IO) {
                val result = scanner.scanWithDetails(uri.toString(), selectedPlatform, selectedEmulator)
                // Older builds stored every disc referenced by an M3U as a
                // separate game. Remove those stale entries before adding the
                // playlist-backed game so rescanning repairs existing data.
                library.removeByRomUris(result.playlistMemberUris)
                result
            }
            val added = withContext(Dispatchers.IO) { library.addAll(scan.games) }
            isWorking = false
            if (added == 0) {
                // Every game in this folder is already in the library, so
                // surface a clear launcher-styled message and stay put.
                message = null
                duplicateFolder = true
                return@launch
            }
            // Remember the folder so it can be silently rescanned on launch.
            withContext(Dispatchers.IO) {
                folderRepository.remember(
                    GameFolderRepository.Folder(
                        path = uri.toString(),
                        name = pendingName.ifBlank { uri.toString() },
                        platform = selectedPlatform,
                        emulator = selectedEmulator
                    )
                )
            }
            message = context.getString(R.string.games_added_count, added)
            onGamesAdded(scan.games)
            // Close the Games window and return to the launcher Home screen.
            onDismiss()
        }
    }

    fun loadInstalledApps() {
        appsLoading = true
        installedApps = emptyList()
        // Pre-check applications that are already in the HorizonOS library.
        selectedApps = games.mapNotNull { it.packageName }.toSet()
        scope.launch {
            val apps = withContext(Dispatchers.IO) { androidApps.installedApps() }
            installedApps = apps
            appsLoading = false
        }
    }

    fun toggleAppSelection(packageName: String) {
        selectedApps = if (packageName in selectedApps) {
            selectedApps - packageName
        } else {
            selectedApps + packageName
        }
    }

    fun addSelectedAndroidApps() {
        if (selectedApps.isEmpty() || isWorking) return
        isWorking = true
        message = null
        scope.launch {
            val games = withContext(Dispatchers.IO) {
                installedApps
                    .filter { it.packageName in selectedApps }
                    .map { app ->
                        Game.fromAndroidApp(
                            label = app.label,
                            packageName = app.packageName,
                            launchActivity = app.activityName,
                            iconPath = androidApps.persistIcon(app)
                        )
                    }
            }
            val added = withContext(Dispatchers.IO) { library.addAll(games) }
            isWorking = false
            message = if (added == 0) {
                context.getString(R.string.games_already_added)
            } else {
                context.getString(R.string.games_added_count, added)
            }
            if (added > 0) onGamesAdded(games)
            // Close the Games window and return to the launcher Home screen.
            onDismiss()
        }
    }

    fun handleFolderResult(uri: Uri?) {
        if (uri == null) return
        // Reject a folder that was already added, even under another platform,
        // so the same games are never imported twice.
        if (folderRepository.load().any { it.path == uri.toString() }) {
            duplicateFolder = true
            return
        }
        persistPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        pendingUri = uri
        pendingName = DocumentFile.fromTreeUri(context, uri)?.name
            ?: context.getString(R.string.games_selected_folder)
        page = if (selectedEmulator.requiresBios && !isBiosWarningShown(selectedEmulator)) {
            BiosWarningPage
        } else {
            ConfirmPage
        }
        focusIndex = 0
        message = null
    }

    fun currentItemCount(): Int = when (page) {
        LibraryPage -> games.size + 1
        PlatformPage -> Platform.values().size
        EmulatorPage -> Emulator.values().count { it.platform == selectedPlatform }
        SourcePage -> 1
        ConfirmPage -> 2
        BiosWarningPage -> 2
        AndroidAppPage -> installedApps.size + 1
        else -> 1
    }

    fun moveFocus(direction: Int) {
        val count = currentItemCount()
        if (count > 0) {
            focusIndex = (focusIndex + direction + count) % count
            if (page == LibraryPage && focusIndex < games.size) {
                selectedLibraryIndex = focusIndex
            }
        }
    }

    fun confirmFocusedItem() {
        when (page) {
            LibraryPage -> when {
                focusIndex < games.size -> {
                    // The library can refresh asynchronously while a key
                    // event is being delivered. Resolve the item defensively
                    // so a stale focus index cannot crash the dialog.
                    games.getOrNull(focusIndex)?.let { game ->
                        selectedLibraryIndex = focusIndex
                        openGameDetails(game)
                    }
                }
                focusIndex == games.size -> {
                    page = PlatformPage
                    focusIndex = 0
                    message = null
                }
                else -> Unit
            }

            PlatformPage -> {
                Platform.values().getOrNull(focusIndex)?.let { platform ->
                    selectedPlatform = platform
                    if (platform == Platform.ANDROID) {
                        loadInstalledApps()
                        page = AndroidAppPage
                        focusIndex = 0
                        return@let
                    }
                    selectedEmulator = Emulator.values().firstOrNull { it.platform == platform }
                        ?: return@let
                    page = EmulatorPage
                    focusIndex = 0
                }
            }

            EmulatorPage -> {
                val supported = Emulator.values().filter { it.platform == selectedPlatform }
                supported.getOrNull(focusIndex)?.let { emulator ->
                    selectedEmulator = emulator
                    page = SourcePage
                    focusIndex = 0
                }
            }

            SourcePage -> {
                selectedSource = RomSource.FOLDER
                onOpenFolder(::handleFolderResult)
            }

            ConfirmPage -> if (focusIndex == 0) addPendingGame() else {
                page = SourcePage
                focusIndex = 0
                pendingUri = null
                pendingName = ""
            }

            BiosWarningPage -> if (focusIndex == 0) {
                markBiosWarningShown(selectedEmulator)
                page = ConfirmPage
                focusIndex = 0
            } else {
                page = SourcePage
                focusIndex = 0
                pendingUri = null
                pendingName = ""
            }

            AndroidAppPage -> {
                if (focusIndex < installedApps.size) {
                    installedApps.getOrNull(focusIndex)?.let { app ->
                        toggleAppSelection(app.packageName)
                    }
                } else {
                    addSelectedAndroidApps()
                }
            }
        }
    }

    fun handleBack(): Boolean {
        when (page) {
            LibraryPage -> onDismiss()
            PlatformPage -> resetToLibrary()
            EmulatorPage -> {
                page = PlatformPage
                focusIndex = selectedPlatform.ordinal
            }
            SourcePage -> {
                page = EmulatorPage
                focusIndex = Emulator.values()
                    .filter { it.platform == selectedPlatform }
                    .indexOf(selectedEmulator)
                    .coerceAtLeast(0)
            }
            ConfirmPage -> {
                page = SourcePage
                focusIndex = 0
                pendingUri = null
                pendingName = ""
            }
            BiosWarningPage -> {
                page = SourcePage
                focusIndex = 0
                pendingUri = null
                pendingName = ""
            }
            AndroidAppPage -> {
                page = PlatformPage
                focusIndex = selectedPlatform.ordinal
            }
        }
        return true
    }

    fun handleOverlayBack(): Boolean {
        if (detailsGame != null) {
            detailsGame = null
            return true
        }
        return handleBack()
    }

    // Keep Android Back consistent with the controller B button even when
    // the event is dispatched by the host activity instead of the dialog.
    BackHandler(enabled = true) {
        handleOverlayBack()
    }

    LaunchedEffect(page, games.size, focusIndex) {
        focusIndex = focusIndex.coerceIn(0, (currentItemCount() - 1).coerceAtLeast(0))
        // The Android app list scrolls only when the focused row is actually
        // outside the viewport (handled per-row via BringIntoViewRequester),
        // so it must not use the fixed offset scrolling below.
        if (page != AndroidAppPage) {
            val target = with(density) { (focusIndex * 78).dp.roundToPx() }
            gamesScrollState.animateScrollTo(target)
        }
        // HorizonOverlay creates a separate Dialog window. Request focus
        // after that window has attached so controller events reach this
        // screen instead of the scrim host.
        delay(260)
        focusRequester.requestFocus()
    }

    CompositionLocalProvider(LocalSettingsInputMode provides inputMode) {
        HorizonOverlay(
            title = stringResource(R.string.games_window_title),
            onDismiss = onDismiss,
            onControllerBack = ::handleOverlayBack,
            onFooterBack = { handleOverlayBack() },
            scrollState = gamesScrollState
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    inputMode.value = SettingsInputMode.GAMEPAD
                    if (isHorizonConfirmKey(event)) {
                        confirmFocusedItem()
                        true
                    } else when (event.key) {
                        Key.DirectionDown, Key.DirectionRight -> {
                            moveFocus(1)
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            moveFocus(-1)
                            true
                        }
                        Key.ButtonB, Key.Back -> handleOverlayBack()
                        else -> false
                    }
                }
        ) {
            when (page) {
                LibraryPage -> LibraryContent(
                    games = games,
                    focusIndex = focusIndex,
                    message = message,
                    onGameClick = ::openGameDetails,
                    onAddClick = {
                        page = PlatformPage
                        focusIndex = 0
                        message = null
                    }
                )

                PlatformPage -> PlatformContent(
                    focusIndex = focusIndex,
                    selected = selectedPlatform,
                    onSelect = {
                        selectedPlatform = it
                        if (it == Platform.ANDROID) {
                            loadInstalledApps()
                            page = AndroidAppPage
                            focusIndex = 0
                        } else {
                            selectedEmulator = Emulator.values().first { emulator -> emulator.platform == it }
                            page = EmulatorPage
                            focusIndex = 0
                        }
                    }
                )

                AndroidAppPage -> AndroidAppsContent(
                    apps = installedApps,
                    selected = selectedApps,
                    loading = appsLoading,
                    working = isWorking,
                    focusIndex = focusIndex,
                    onToggle = ::toggleAppSelection,
                    onConfirm = ::addSelectedAndroidApps
                )

                EmulatorPage -> EmulatorContent(
                    platform = selectedPlatform,
                    focusIndex = focusIndex,
                    selected = selectedEmulator,
                    onSelect = {
                        selectedEmulator = it
                        page = SourcePage
                        focusIndex = 0
                    }
                )

                SourcePage -> SourceContent(
                    platform = selectedPlatform,
                    emulator = selectedEmulator,
                    focusIndex = focusIndex,
                    onSelect = {
                        selectedSource = RomSource.FOLDER
                        onOpenFolder(::handleFolderResult)
                    }
                )

                ConfirmPage -> ConfirmContent(
                    source = selectedSource,
                    focusIndex = focusIndex,
                    isWorking = isWorking,
                    onConfirm = ::addPendingGame,
                    onCancel = ::handleBack
                )

                BiosWarningPage -> BiosWarningContent(
                    emulator = selectedEmulator,
                    focusIndex = focusIndex,
                    onContinue = {
                        markBiosWarningShown(selectedEmulator)
                        page = ConfirmPage
                        focusIndex = 0
                    },
                    onCancel = ::handleBack
                )
            }
        }
        }

    detailsGame?.let { game ->
        GameDetailsOverlay(
            game = game,
            onToggleHidden = {
                val updated = game.copy(hidden = !game.hidden)
                detailsGame = updated
                scope.launch { library.update(updated) }
            },
            onEditTitle = { titleEditorGame = game },
            onScan = { scanMenuGame = game },
            onDelete = {
                detailsGame = null
                scope.launch {
                    library.remove(game)
                    message = context.getString(R.string.games_removed)
                }
            },
            onDismiss = { detailsGame = null }
        )
    }

    titleEditorGame?.let { game ->
        GameTitleEditorOverlay(
            game = game,
            onSave = { title ->
                applyGameUpdate(metadataEditor.setTitle(game, title))
                titleEditorGame = null
            },
            onDismiss = { titleEditorGame = null }
        )
    }

    scanMenuGame?.let { game ->
        GameScanMenuOverlay(
            game = game,
            onAutoScan = {
                scanMenuGame = null
                ScanCoordinator.init(context)
                ScanCoordinator.enqueue(listOf(game))
                message = context.getString(R.string.games_scan_started)
            },
            onCustomCover = {
                scanMenuGame = null
                customCoverGame = game
            },
            onCustomScreenshot = {
                scanMenuGame = null
                customScreenshotGame = game
            },
            onDismiss = { scanMenuGame = null }
        )
    }

    customCoverGame?.let { game ->
        ImagePickerOverlay(
            title = stringResource(R.string.games_custom_cover_title),
            onPick = { entry ->
                customCoverGame = null
                scope.launch {
                    val updated = withContext(Dispatchers.IO) { metadataEditor.applyLocalCover(game, entry.path) }
                    if (updated != null) applyGameUpdate(updated)
                    else message = context.getString(R.string.games_media_apply_failed)
                }
            },
            onDismiss = { customCoverGame = null }
        )
    }

    customScreenshotGame?.let { game ->
        ImagePickerOverlay(
            title = stringResource(R.string.games_custom_screenshot_title),
            onPick = { entry ->
                customScreenshotGame = null
                scope.launch {
                    val updated = withContext(Dispatchers.IO) { metadataEditor.applyLocalScreenshot(game, entry.path) }
                    if (updated != null) applyGameUpdate(updated)
                    else message = context.getString(R.string.games_media_apply_failed)
                }
            },
            onDismiss = { customScreenshotGame = null }
        )
    }

    if (duplicateFolder) {
        HorizonOverlay(
            title = stringResource(R.string.games_duplicate_title),
            onDismiss = { duplicateFolder = false }
        ) {
            Text(
                text = stringResource(R.string.games_already_added_folder),
                color = SettingsWhite,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.action_ok),
                selected = true,
                onClick = { duplicateFolder = false }
            )
        }
    }

    }
}

@Composable
private fun LibraryContent(
    games: List<Game>,
    focusIndex: Int,
    message: String?,
    onGameClick: (Game) -> Unit,
    onAddClick: () -> Unit
) {
    games.forEachIndexed { index, game ->
        GameOptionRow(
title = game.displayTitle,
            subtitle = stringResource(
                R.string.games_selection_summary,
                platformLabel(game.platform),
                emulatorLabel(game.emulator)
            ),
            selected = false,
            focused = focusIndex == index,
            hidden = game.hidden,
            onClick = { onGameClick(game) }
        )
    }
    GameOptionRow(
        title = stringResource(R.string.games_add),
        subtitle = if (games.isEmpty()) {
            stringResource(R.string.games_empty)
        } else {
            stringResource(R.string.games_add_description)
        },
        selected = false,
        focused = focusIndex == games.size,
        accent = true,
        onClick = onAddClick
    )
    message?.let {
        Text(
            text = it,
            color = SettingsBlue,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun PlatformContent(
    focusIndex: Int,
    selected: Platform,
    onSelect: (Platform) -> Unit
) {
    Platform.values().forEachIndexed { index, platform ->
        GameOptionRow(
            title = platformLabel(platform),
            subtitle = if (platform == Platform.ANDROID) {
                stringResource(R.string.games_android_apps_description)
            } else {
                platform.romExtensions.sorted().joinToString(", ") { ".${it.uppercase()}" }
            },
            selected = selected == platform,
            focused = focusIndex == index,
            onClick = { onSelect(platform) }
        )
    }
}

@Composable
private fun EmulatorContent(
    platform: Platform,
    focusIndex: Int,
    selected: Emulator,
    onSelect: (Emulator) -> Unit
) {
    Emulator.values().filter { it.platform == platform }.forEachIndexed { index, emulator ->
        GameOptionRow(
            title = emulatorLabel(emulator),
            subtitle = platformLabel(platform),
            selected = selected == emulator,
            focused = focusIndex == index,
            onClick = { onSelect(emulator) }
        )
    }
}

@Composable
private fun SourceContent(
    platform: Platform,
    emulator: Emulator,
    focusIndex: Int,
    onSelect: (RomSource) -> Unit
) {
    Text(
        text = stringResource(R.string.games_selection_summary, platformLabel(platform), emulatorLabel(emulator)),
        color = SettingsGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    )
    GameOptionRow(
        title = stringResource(R.string.games_choose_folder),
        subtitle = stringResource(R.string.games_choose_folder_description),
        selected = true,
        focused = focusIndex == 0,
        accent = true,
        onClick = { onSelect(RomSource.FOLDER) }
    )
}

@Composable
private fun AndroidAppsContent(
    apps: List<InstalledAppInfo>,
    selected: Set<String>,
    loading: Boolean,
    working: Boolean,
    focusIndex: Int,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Text(
        text = stringResource(R.string.games_android_apps_title),
        color = SettingsBlue,
        fontSize = 19.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
    )
    Text(
        text = stringResource(R.string.games_android_apps_description),
        color = SettingsGray,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
    )
    if (loading) {
        Text(
            text = stringResource(R.string.games_android_apps_loading),
            color = SettingsGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    } else if (apps.isEmpty()) {
        Text(
            text = stringResource(R.string.games_android_apps_empty),
            color = SettingsGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
    apps.forEachIndexed { index, app ->
        AndroidAppRow(
            app = app,
            checked = app.packageName in selected,
            focused = focusIndex == index,
            onClick = { onToggle(app.packageName) }
        )
    }
    GameOptionRow(
        title = if (working) stringResource(R.string.games_scanning)
        else stringResource(R.string.action_ok),
        subtitle = stringResource(R.string.games_android_apps_selected, selected.size),
        selected = false,
        focused = focusIndex == apps.size,
        accent = true,
        bringIntoViewWhenFocused = true,
        onClick = onConfirm
    )
}

@Composable
private fun AndroidAppRow(
    app: InstalledAppInfo,
    checked: Boolean,
    focused: Boolean,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "androidAppSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "androidAppSelectionPulseValue"
    )
    val active = inputMode?.value == SettingsInputMode.GAMEPAD && focused
    val icon = rememberAppIcon(app.icon)
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    // Scroll minimally only when the focused row is not fully visible.
    LaunchedEffect(focused) {
        if (focused) bringIntoViewRequester.bringIntoView()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) SettingsDivider.copy(alpha = 0.32f) else Color.Transparent)
            .then(
                if (active) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                    )
                } else Modifier
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckboxGlyph(checked = checked)
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = app.label,
            color = SettingsWhite,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
}

@Composable
private fun CheckboxGlyph(checked: Boolean) {
    val color = if (checked) SettingsBlue else SettingsGray
    Canvas(Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke)
        )
        if (checked) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.52f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.72f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.72f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun rememberAppIcon(drawable: Drawable?): ImageBitmap? {
    return remember(drawable) {
        drawable?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
}

@Composable
private fun BiosWarningContent(    emulator: Emulator,
    focusIndex: Int,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.games_bios_warning_title),
        color = SettingsBlue,
        fontSize = 19.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp)
    )
    Text(
        text = stringResource(R.string.games_bios_warning_description, emulatorLabel(emulator)),
        color = SettingsWhite,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
    )
    GameOptionRow(
        title = stringResource(R.string.games_bios_warning_continue),
        subtitle = stringResource(R.string.games_bios_warning_continue_description),
        selected = false,
        focused = focusIndex == 0,
        accent = true,
        onClick = onContinue
    )
    GameOptionRow(
        title = stringResource(R.string.games_cancel),
        subtitle = "",
        selected = false,
        focused = focusIndex == 1,
        onClick = onCancel
    )
}

@Composable
private fun ConfirmContent(
    source: RomSource,
    focusIndex: Int,
    isWorking: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.games_confirm_folder_title),
        color = SettingsBlue,
        fontSize = 19.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
    )
    Text(
        text = stringResource(R.string.games_confirm_folder_description),
        color = SettingsGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
    )
    GameOptionRow(
        title = if (isWorking) stringResource(R.string.games_scanning)
        else stringResource(R.string.games_confirm),
        subtitle = stringResource(R.string.games_confirm_description),
        selected = false,
        focused = focusIndex == 0,
        accent = true,
        onClick = onConfirm
    )
    GameOptionRow(
        title = stringResource(R.string.games_choose_another_folder),
        subtitle = stringResource(R.string.games_choose_another_folder_description),
        selected = false,
        focused = focusIndex == 1,
        onClick = onCancel
    )
}

@Composable
private fun GameOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    focused: Boolean,
    accent: Boolean = false,
    hidden: Boolean = false,
    bringIntoViewWhenFocused: Boolean = false,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "gameOptionSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gameOptionSelectionPulseValue"
    )
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) {
        if (focused && bringIntoViewWhenFocused) bringIntoViewRequester.bringIntoView()
    }
    val active = inputMode?.value == SettingsInputMode.GAMEPAD && focused
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) SettingsDivider.copy(alpha = 0.32f) else Color.Transparent)
            .then(
                if (active) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                    )
                } else Modifier
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = if (accent) SettingsBlue else SettingsWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f))
            if (hidden) {
                HiddenEyeIcon(selected = active)
            }
        }
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(text = subtitle, color = SettingsGray, fontSize = 12.sp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
}

@Composable
private fun HiddenEyeIcon(selected: Boolean) {
    val color = if (selected) SettingsBlue else SettingsGray
    Canvas(Modifier.size(24.dp)) {
        val eyeWidth = size.width * 0.72f
        val eyeHeight = size.height * 0.46f
        val left = (size.width - eyeWidth) / 2f
        val top = (size.height - eyeHeight) / 2f
        drawOval(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(color = color, radius = 2.5.dp.toPx(), center = center)
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(3.dp.toPx(), size.height - 3.dp.toPx()),
            end = androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), 3.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun platformLabel(platform: Platform): String = when (platform) {
    Platform.PLAYSTATION_1 -> stringResource(R.string.games_platform_ps1)
    Platform.PSP -> stringResource(R.string.games_platform_psp)
    Platform.PLAYSTATION_2 -> stringResource(R.string.games_platform_ps2)
    Platform.GAMECUBE_WII -> stringResource(R.string.games_platform_gamecube_wii)
    Platform.ANDROID -> stringResource(R.string.games_platform_android)
}

@Composable
private fun emulatorLabel(emulator: Emulator): String = stringResource(emulator.titleRes)

/** A deliberately different, compact dialog for per-game actions. */
@Composable
private fun GameDetailsOverlay(
    game: Game,
    onToggleHidden: () -> Unit,
    onEditTitle: () -> Unit,
    onScan: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val detailScrollState = rememberScrollState()
    val density = LocalDensity.current
    var focusIndex by remember { mutableIntStateOf(0) }
    val inputMode = LocalSettingsInputMode.current
    // Compose Dialog uses a separate window context. Preserve the localized
    // parent context so the title and all action rows use the same language.
    val localizedContext = LocalContext.current
    val rowCount = 4

    LaunchedEffect(focusIndex) {
        detailScrollState.animateScrollTo(with(density) { (focusIndex * 58).dp.roundToPx() })
    }

    fun moveFocus(direction: Int) {
        focusIndex = (focusIndex + direction + rowCount) % rowCount
    }

    fun activate(index: Int) {
        when (index) {
            0 -> onToggleHidden()
            1 -> onEditTitle()
            2 -> onScan()
            3 -> onDelete()
        }
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
        CompositionLocalProvider(LocalContext provides localizedContext) {
            BackHandler(enabled = true, onBack = onDismiss)

            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            DisposableEffect(dialogWindow) {
            if (dialogWindow == null) {
                onDispose { }
            } else {
                hideDialogSystemBars(dialogWindow)
                val focusListener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) hideDialogSystemBars(dialogWindow)
                }
                dialogWindow.decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
                val previousWindowCallback = dialogWindow.callback
                val backKeyCallback = previousWindowCallback?.let { previous ->
                    object : android.view.Window.Callback by previous {
                        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                            if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                                if (event.action == android.view.KeyEvent.ACTION_UP) onDismiss()
                                return true
                            }
                            return previous.dispatchKeyEvent(event)
                        }
                    }
                }
                if (backKeyCallback != null) dialogWindow.callback = backKeyCallback
                var nativeBackCallback: android.window.OnBackInvokedCallback? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    nativeBackCallback = android.window.OnBackInvokedCallback { onDismiss() }
                    dialogWindow.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                        android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                        nativeBackCallback!!
                    )
                }
                onDispose {
                    dialogWindow.decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
                    if (backKeyCallback != null && dialogWindow.callback === backKeyCallback) {
                        dialogWindow.callback = previousWindowCallback
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        nativeBackCallback?.let {
                            dialogWindow.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
                        }
                    }
                }
            }
            }
            LaunchedEffect(dialogWindow) {
            delay(120)
            focusRequester.requestFocus()
            }
            Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    inputMode?.value = SettingsInputMode.GAMEPAD
                    if (isHorizonConfirmKey(event)) {
                        activate(focusIndex)
                        true
                    } else when (event.key) {
                        Key.DirectionDown, Key.DirectionRight -> {
                            moveFocus(1)
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            moveFocus(-1)
                            true
                        }
                        Key.ButtonB, Key.Back -> {
                            onDismiss()
                            true
                        }
                        else -> false
                    }
                }
                .background(Color.Black.copy(alpha = 0.70f)),
            contentAlignment = Alignment.Center
            ) {
            BoxWithConstraints {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.56f)
                        .widthIn(max = 560.dp)
                        .heightIn(max = maxHeight * 0.90f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SettingsOverlayPanel)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {})
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.ButtonB || event.key == Key.Back)
                            ) {
                                onDismiss()
                                true
                            } else false
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 12.dp, top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.games_details_title),
                            color = SettingsBlue,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("←", color = SettingsWhite, fontSize = 27.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .height(1.dp)
                            .background(SettingsDivider)
                    )
                    Text(
                        text = game.displayTitle,
                        color = SettingsWhite,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.games_selection_summary,
                            platformLabel(game.platform),
                            emulatorLabel(game.emulator)
                        ),
                        color = SettingsGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .verticalScroll(detailScrollState)
                            .padding(bottom = 12.dp)
                    ) {
                        GameDetailsActionRow(
                            title = if (game.hidden) stringResource(R.string.games_show)
                            else stringResource(R.string.games_hide),
                            subtitle = if (game.hidden) stringResource(R.string.games_show_description)
                            else stringResource(R.string.games_hide_description),
                            selected = focusIndex == 0,
                            onClick = onToggleHidden
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_edit_title),
                            subtitle = game.displayTitle,
                            selected = focusIndex == 1,
                            onClick = onEditTitle
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_scan_action),
                            subtitle = if (game.coverPath != null) stringResource(R.string.games_cover_added)
                            else stringResource(R.string.games_cover_not_added),
                            selected = focusIndex == 2,
                            onClick = onScan
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_delete),
                            subtitle = stringResource(R.string.games_delete_description),
                            selected = focusIndex == 3,
                            destructive = true,
                            onClick = onDelete
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun GameDetailsActionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "gameDetailsSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gameDetailsSelectionPulseValue"
    )
    val active = inputMode?.value == SettingsInputMode.GAMEPAD && selected
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .background(
                if (active) SettingsDivider.copy(alpha = 0.42f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .then(
                if (active) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f),
                        shape = RoundedCornerShape(6.dp)
                    )
                } else Modifier
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = title,
            color = if (destructive) Color(0xFFFF6070) else SettingsWhite,
            fontSize = 16.sp
        )
        Text(
            text = subtitle,
            color = SettingsGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
