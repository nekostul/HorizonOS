package ru.nekostul.horizonos.ui.games

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import ru.nekostul.horizonos.ui.settings.SettingsBlue
import ru.nekostul.horizonos.ui.settings.SettingsDivider
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsOverlayPanel
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.hideDialogSystemBars
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
private const val BiosWarningPreferences = "game_bios_warnings"

private enum class RomSource {
    FOLDER
}

@Composable
fun GamesScreen(
    onDismiss: () -> Unit,
    onOpenFolder: (((Uri?) -> Unit) -> Unit)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val library = remember { GameLibrary(context) }
    val games by library.games.collectAsState(initial = emptyList())
    val scanner = remember { GameScanner(context) }
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
    var isWorking by remember { mutableStateOf(false) }
    var detailsGame by remember { mutableStateOf<Game?>(null) }
    var touchArmedIndex by remember { mutableIntStateOf(-1) }

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
        touchArmedIndex = -1
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
            val added = withContext(Dispatchers.IO) {
                val scan = scanner.scanWithDetails(uri.toString(), selectedPlatform, selectedEmulator)
                // Older builds stored every disc referenced by an M3U as a
                // separate game. Remove those stale entries before adding the
                // playlist-backed game so rescanning repairs existing data.
                library.removeByRomUris(scan.playlistMemberUris)
                library.addAll(scan.games)
            }
            isWorking = false
            message = if (added == 0) {
                context.getString(R.string.games_already_added)
            } else {
                context.getString(R.string.games_added_count, added)
            }
            resetToLibrary()
        }
    }

    fun handleFolderResult(uri: Uri?) {
        if (uri == null) return
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
        else -> 1
    }

    fun moveFocus(direction: Int) {
        val count = currentItemCount()
        if (count > 0) {
            touchArmedIndex = -1
            focusIndex = (focusIndex + direction + count) % count
            if (page == LibraryPage && focusIndex < games.size) {
                selectedLibraryIndex = focusIndex
            }
        }
    }

    fun confirmFocusedItem() {
        touchArmedIndex = -1
        when (page) {
            LibraryPage -> when {
                focusIndex < games.size -> {
                    selectedLibraryIndex = focusIndex
                    openGameDetails(games[focusIndex])
                }
                focusIndex == games.size -> {
                    page = PlatformPage
                    focusIndex = 0
                    message = null
                }
                else -> Unit
            }

            PlatformPage -> {
                selectedPlatform = Platform.values()[focusIndex]
                selectedEmulator = Emulator.values().first { it.platform == selectedPlatform }
                page = EmulatorPage
                focusIndex = 0
            }

            EmulatorPage -> {
                val supported = Emulator.values().filter { it.platform == selectedPlatform }
                selectedEmulator = supported[focusIndex]
                page = SourcePage
                focusIndex = 0
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

    LaunchedEffect(page, games.size, focusIndex) {
        focusIndex = focusIndex.coerceIn(0, (currentItemCount() - 1).coerceAtLeast(0))
        // HorizonOverlay creates a separate Dialog window. Request focus
        // after that window has attached so controller events reach this
        // screen instead of the scrim host.
        delay(260)
        focusRequester.requestFocus()
    }

    HorizonOverlay(
        title = stringResource(R.string.games_window_title),
        onDismiss = onDismiss,
        onControllerBack = ::handleOverlayBack,
        onFooterBack = { handleOverlayBack() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
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
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
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
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
                    selected = selectedPlatform,
                    onSelect = {
                        selectedPlatform = it
                        selectedEmulator = Emulator.values().first { emulator -> emulator.platform == it }
                        page = EmulatorPage
                        focusIndex = 0
                    }
                )

                EmulatorPage -> EmulatorContent(
                    platform = selectedPlatform,
                    focusIndex = focusIndex,
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
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
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
                    onSelect = {
                        selectedSource = RomSource.FOLDER
                        onOpenFolder(::handleFolderResult)
                    }
                )

                ConfirmPage -> ConfirmContent(
                    source = selectedSource,
                    focusIndex = focusIndex,
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
                    isWorking = isWorking,
                    onConfirm = ::addPendingGame,
                    onCancel = ::handleBack
                )

                BiosWarningPage -> BiosWarningContent(
                    emulator = selectedEmulator,
                    focusIndex = focusIndex,
                    touchArmedIndex = touchArmedIndex,
                    onTouchArm = { index -> touchArmedIndex = index },
                    onTouchDisarm = { touchArmedIndex = -1 },
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
}

@Composable
private fun LibraryContent(
    games: List<Game>,
    focusIndex: Int,
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
    message: String?,
    onGameClick: (Game) -> Unit,
    onAddClick: () -> Unit
) {
    games.forEachIndexed { index, game ->
        GameOptionRow(
            title = game.title,
            subtitle = "${game.platform.title} · ${game.emulator.title}",
            selected = false,
            focused = focusIndex == index,
            touchArmed = touchArmedIndex == index,
            hidden = game.hidden,
            onClick = {
                if (touchArmedIndex == index) {
                    onTouchDisarm()
                    onGameClick(game)
                } else {
                    onTouchArm(index)
                }
            }
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
        touchArmed = touchArmedIndex == games.size,
        accent = true,
        onClick = {
            if (touchArmedIndex == games.size) {
                onTouchDisarm()
                onAddClick()
            } else {
                onTouchArm(games.size)
            }
        }
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
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
    selected: Platform,
    onSelect: (Platform) -> Unit
) {
    Platform.values().forEachIndexed { index, platform ->
        GameOptionRow(
            title = platformLabel(platform),
            subtitle = platform.romExtensions.sorted().joinToString(", ") { ".${it.uppercase()}" },
            selected = selected == platform,
            focused = focusIndex == index,
            touchArmed = touchArmedIndex == index,
            onClick = {
                if (touchArmedIndex == index) {
                    onTouchDisarm()
                    onSelect(platform)
                } else {
                    onTouchArm(index)
                }
            }
        )
    }
}

@Composable
private fun EmulatorContent(
    platform: Platform,
    focusIndex: Int,
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
    selected: Emulator,
    onSelect: (Emulator) -> Unit
) {
    Emulator.values().filter { it.platform == platform }.forEachIndexed { index, emulator ->
        GameOptionRow(
            title = emulator.title,
            subtitle = platformLabel(platform),
            selected = selected == emulator,
            focused = focusIndex == index,
            touchArmed = touchArmedIndex == index,
            onClick = {
                if (touchArmedIndex == index) {
                    onTouchDisarm()
                    onSelect(emulator)
                } else {
                    onTouchArm(index)
                }
            }
        )
    }
}

@Composable
private fun SourceContent(
    platform: Platform,
    emulator: Emulator,
    focusIndex: Int,
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
    onSelect: (RomSource) -> Unit
) {
    Text(
        text = stringResource(R.string.games_selection_summary, platformLabel(platform), emulator.title),
        color = SettingsGray,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    )
    GameOptionRow(
        title = stringResource(R.string.games_choose_folder),
        subtitle = stringResource(R.string.games_choose_folder_description),
        selected = true,
        focused = focusIndex == 0,
        touchArmed = touchArmedIndex == 0,
        accent = true,
        onClick = {
            if (touchArmedIndex == 0) {
                onTouchDisarm()
                onSelect(RomSource.FOLDER)
            } else {
                onTouchArm(0)
            }
        }
    )
}

@Composable
private fun BiosWarningContent(
    emulator: Emulator,
    focusIndex: Int,
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
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
        text = stringResource(R.string.games_bios_warning_description, emulator.title),
        color = SettingsWhite,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
    )
    GameOptionRow(
        title = stringResource(R.string.games_bios_warning_continue),
        subtitle = stringResource(R.string.games_bios_warning_continue_description),
        selected = false,
        focused = focusIndex == 0,
        touchArmed = touchArmedIndex == 0,
        accent = true,
        onClick = {
            if (touchArmedIndex == 0) {
                onTouchDisarm()
                onContinue()
            } else {
                onTouchArm(0)
            }
        }
    )
    GameOptionRow(
        title = stringResource(R.string.games_cancel),
        subtitle = "",
        selected = false,
        focused = focusIndex == 1,
        touchArmed = touchArmedIndex == 1,
        onClick = {
            if (touchArmedIndex == 1) {
                onTouchDisarm()
                onCancel()
            } else {
                onTouchArm(1)
            }
        }
    )
}

@Composable
private fun ConfirmContent(
    source: RomSource,
    focusIndex: Int,
    touchArmedIndex: Int,
    onTouchArm: (Int) -> Unit,
    onTouchDisarm: () -> Unit,
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
        touchArmed = touchArmedIndex == 0,
        accent = true,
        onClick = {
            if (touchArmedIndex == 0) {
                onTouchDisarm()
                onConfirm()
            } else {
                onTouchArm(0)
            }
        }
    )
    GameOptionRow(
        title = stringResource(R.string.games_choose_another_folder),
        subtitle = stringResource(R.string.games_choose_another_folder_description),
        selected = false,
        focused = focusIndex == 1,
        touchArmed = touchArmedIndex == 1,
        onClick = {
            if (touchArmedIndex == 1) {
                onTouchDisarm()
                onCancel()
            } else {
                onTouchArm(1)
            }
        }
    )
}

@Composable
private fun GameOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    focused: Boolean,
    touchArmed: Boolean,
    accent: Boolean = false,
    hidden: Boolean = false,
    onClick: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "gameOptionSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gameOptionSelectionPulseValue"
    )
    val active = focused || touchArmed
    val selectionColor = SettingsBlue
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) SettingsDivider.copy(alpha = 0.32f) else Color.Transparent)
            .then(
                if (focused || touchArmed) {
                    Modifier.drawBehind {
                        drawRect(
                            color = selectionColor.copy(alpha = if (focused) 0.95f else 0.20f + pulseValue * 0.16f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = if (active || (accent && selected)) SettingsBlue else SettingsWhite,
                fontSize = 16.sp
            )
            Spacer(Modifier.weight(1f))
            if (hidden) {
                HiddenEyeIcon(selected = active)
            } else if (selected) {
                Box(
                    modifier = Modifier.size(22.dp).background(SettingsBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = SettingsOverlayPanel, fontSize = 15.sp)
                }
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
}

/** A deliberately different, compact dialog for per-game actions. */
@Composable
private fun GameDetailsOverlay(
    game: Game,
    onToggleHidden: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val detailScrollState = rememberScrollState()
    val density = LocalDensity.current
    var focusIndex by remember { mutableIntStateOf(0) }
    var touchArmedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(focusIndex) {
        val target = when (focusIndex) {
            0, 1 -> 0.dp
            2 -> 66.dp
            else -> 132.dp
        }
        detailScrollState.animateScrollTo(with(density) { target.roundToPx() })
    }

    fun moveFocus(direction: Int) {
        focusIndex = (focusIndex + direction + 4) % 4
        touchArmedIndex = -1
    }

    fun activate(index: Int) {
        when (index) {
            0 -> onToggleHidden()
            1 -> Unit
            2 -> Unit
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
                        text = game.title,
                        color = SettingsWhite,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    Text(
                        text = stringResource(
                            R.string.games_selection_summary,
                            game.platform.title,
                            game.emulator.title
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
                            touchArmed = touchArmedIndex == 0,
                            onClick = {
                                if (touchArmedIndex == 0) {
                                    touchArmedIndex = -1
                                    onToggleHidden()
                                } else {
                                    touchArmedIndex = 0
                                }
                            }
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_refresh_cover),
                            subtitle = stringResource(R.string.games_action_coming_soon),
                            selected = focusIndex == 1,
                            touchArmed = touchArmedIndex == 1,
                            onClick = {
                                if (touchArmedIndex == 1) touchArmedIndex = -1 else touchArmedIndex = 1
                            }
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_add_screenshot),
                            subtitle = stringResource(R.string.games_action_coming_soon),
                            selected = focusIndex == 2,
                            touchArmed = touchArmedIndex == 2,
                            onClick = {
                                if (touchArmedIndex == 2) touchArmedIndex = -1 else touchArmedIndex = 2
                            }
                        )
                        GameDetailsActionRow(
                            title = stringResource(R.string.games_delete),
                            subtitle = stringResource(R.string.games_delete_description),
                            selected = focusIndex == 3,
                            touchArmed = touchArmedIndex == 3,
                            destructive = true,
                            onClick = {
                                if (touchArmedIndex == 3) {
                                    touchArmedIndex = -1
                                    onDelete()
                                } else {
                                    touchArmedIndex = 3
                                }
                            }
                        )
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
    touchArmed: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "gameDetailsSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gameDetailsSelectionPulseValue"
    )
    val active = selected || touchArmed
    val activeColor = if (destructive) Color(0xFFFF6070) else SettingsBlue
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .background(
                if (active) SettingsDivider.copy(alpha = 0.42f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .then(
                if (selected || touchArmed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = activeColor.copy(alpha = if (selected) 0.95f else 0.20f + pulseValue * 0.16f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Text(
            text = title,
            color = if (active) activeColor else SettingsWhite,
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
