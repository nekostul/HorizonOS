package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.GameLibrary
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonOverlayTextField
import ru.nekostul.horizonos.ui.settings.SettingsAccentTeal
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private enum class EditorTarget { SCREEN_SCRAPER, THE_GAMES_DB, IGDB, STEAM_GRID_DB }

/**
 * "Scanning" screen. Sources are always shown in [ScraperSourceId] order,
 * which is also the scraping priority order.
 */
@Composable
fun ScanningSettingsScreen(
    context: Context,
    onDismiss: () -> Unit
) {
    val repository = remember { ScraperRepository(context) }
    var settings by remember { mutableStateOf(repository.load()) }
    val scope = rememberCoroutineScope()

    // 0..4 sources, 5 ScreenScraper, 6 TheGamesDB, 7 IGDB, 8 SteamGridDB, 9 run
    var selectedIndex by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    val scanning by ScanCoordinator.scanning.collectAsState()
    val progress by ScanCoordinator.progress.collectAsState()
    val summary by ScanCoordinator.summary.collectAsState()

    fun toggleSource(id: ScraperSourceId) {
        val next = !settings.isEnabled(id)
        repository.setEnabled(id, next)
        settings = settings.copy(enabled = settings.enabled + (id to next))
    }

    fun startScan() {
        ScanCoordinator.init(context)
        ScanCoordinator.enqueueAll()
    }

    fun activate(index: Int) {
        when (index) {
            0 -> toggleSource(ScraperSourceId.SCREEN_SCRAPER)
            1 -> toggleSource(ScraperSourceId.LIBRETRO)
            2 -> toggleSource(ScraperSourceId.THE_GAMES_DB)
            3 -> toggleSource(ScraperSourceId.IGDB)
            4 -> toggleSource(ScraperSourceId.STEAM_GRID_DB)
            5 -> editor = EditorTarget.SCREEN_SCRAPER
            6 -> editor = EditorTarget.THE_GAMES_DB
            7 -> editor = EditorTarget.IGDB
            8 -> editor = EditorTarget.STEAM_GRID_DB
            9 -> startScan()
        }
    }

    val target = editor
    if (target == null) {
        HorizonOverlay(
            title = stringResource(R.string.settings_scanning_title),
            onDismiss = onDismiss,
            onDirectionalKey = { key ->
                when (key) {
                    Key.DirectionDown, Key.DirectionRight -> {
                        selectedIndex = (selectedIndex + 1).coerceAtMost(9)
                        true
                    }
                    Key.DirectionUp, Key.DirectionLeft -> {
                        selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        true
                    }
                    else -> false
                }
            }
        ) {
            SourceToggleRow(
                title = stringResource(R.string.settings_scraper_screen_scraper),
                enabledSource = settings.isEnabled(ScraperSourceId.SCREEN_SCRAPER),
                selected = selectedIndex == 0
            ) { activate(0) }
            SourceToggleRow(
                title = stringResource(R.string.settings_scraper_libretro),
                enabledSource = settings.isEnabled(ScraperSourceId.LIBRETRO),
                selected = selectedIndex == 1
            ) { activate(1) }
            SourceToggleRow(
                title = stringResource(R.string.settings_scraper_thegamesdb),
                enabledSource = settings.isEnabled(ScraperSourceId.THE_GAMES_DB),
                selected = selectedIndex == 2
            ) { activate(2) }
            SourceToggleRow(
                title = stringResource(R.string.settings_scraper_igdb),
                enabledSource = settings.isEnabled(ScraperSourceId.IGDB),
                selected = selectedIndex == 3
            ) { activate(3) }
            SourceToggleRow(
                title = stringResource(R.string.settings_scraper_steam_grid_db),
                enabledSource = settings.isEnabled(ScraperSourceId.STEAM_GRID_DB),
                selected = selectedIndex == 4
            ) { activate(4) }

            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_scanning_order_hint),
                color = SettingsGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(8.dp))
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_scraper_screen_scraper_credentials),
                value = if (settings.screenScraperDevId.isNotBlank() && settings.screenScraperDevPassword.isNotBlank())
                    stringResource(R.string.settings_status_on) else stringResource(R.string.settings_status_off),
                selected = selectedIndex == 5,
                onClick = { activate(5) }
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_scraper_thegamesdb_api_key),
                value = keyState(settings.theGamesDbApiKey),
                selected = selectedIndex == 6,
                onClick = { activate(6) }
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_scraper_igdb_credentials),
                value = if (settings.igdbClientId.isNotBlank() && settings.igdbClientSecret.isNotBlank())
                    stringResource(R.string.settings_status_on) else stringResource(R.string.settings_status_off),
                selected = selectedIndex == 7,
                onClick = { activate(7) }
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_scraper_steam_api_key),
                value = keyState(settings.steamGridDbApiKey),
                selected = selectedIndex == 8,
                onClick = { activate(8) }
            )
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_scanner_run),
                value = if (scanning) stringResource(R.string.settings_scanner_running) else "",
                selected = selectedIndex == 9,
                enabled = !scanning,
                onClick = { activate(9) }
            )

            if (scanning) {
                Spacer(Modifier.height(10.dp))
                val p = progress
                Text(
                    stringResource(R.string.settings_scanner_progress, p?.index ?: 0, p?.total ?: 0),
                    color = SettingsWhite,
                    fontSize = 16.sp
                )
                p?.let { Text(it.gameTitle, color = SettingsGray, fontSize = 14.sp) }
            }

            summary?.let { result ->
                Spacer(Modifier.height(10.dp))
                Text(
                    if (result.networkUnavailable) {
                        stringResource(R.string.settings_scanner_no_network)
                    } else {
                        stringResource(R.string.settings_scanner_done, result.updated, result.failed)
                    },
                    color = if (result.networkUnavailable) SettingsGray else SettingsAccentTeal,
                    fontSize = 14.sp
                )
                if (result.diagnostics.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_scanner_diagnostics),
                        color = SettingsGray,
                        fontSize = 12.sp
                    )
                    result.diagnostics.takeLast(40).forEach { line ->
                        Text(
                            text = line,
                            color = SettingsGray,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    } else {
        CredentialsEditor(
            target = target,
            settings = settings,
            onDismiss = { editor = null },
            onSave = { updated ->
                when (target) {
                    EditorTarget.SCREEN_SCRAPER -> repository.setScreenScraperCredentials(
                        updated.screenScraperDevId, updated.screenScraperDevPassword, updated.screenScraperSoftName
                    )
                    EditorTarget.THE_GAMES_DB -> repository.setTheGamesDbApiKey(updated.theGamesDbApiKey)
                    EditorTarget.IGDB -> repository.setIgdbCredentials(updated.igdbClientId, updated.igdbClientSecret)
                    EditorTarget.STEAM_GRID_DB -> repository.setSteamGridDbApiKey(updated.steamGridDbApiKey)
                }
                settings = updated
                editor = null
            }
        )
    }
}

@Composable
private fun SourceToggleRow(
    title: String,
    enabledSource: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    HorizonOverlayChoice(
        title = title,
        value = if (enabledSource) "✓" else "",
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun keyState(value: String): String =
    if (value.isNotBlank()) stringResource(R.string.settings_status_on) else stringResource(R.string.settings_status_off)

@Composable
private fun CredentialsEditor(
    target: EditorTarget,
    settings: ScraperSettings,
    onDismiss: () -> Unit,
    onSave: (ScraperSettings) -> Unit
) {
    var first by remember(target) {
        mutableStateOf(
            when (target) {
                EditorTarget.SCREEN_SCRAPER -> settings.screenScraperDevId
                EditorTarget.THE_GAMES_DB -> settings.theGamesDbApiKey
                EditorTarget.IGDB -> settings.igdbClientId
                EditorTarget.STEAM_GRID_DB -> settings.steamGridDbApiKey
            }
        )
    }
    var second by remember(target) {
        mutableStateOf(
            when (target) {
                EditorTarget.SCREEN_SCRAPER -> settings.screenScraperDevPassword
                EditorTarget.IGDB -> settings.igdbClientSecret
                else -> ""
            }
        )
    }

    val fieldCount = when (target) {
        EditorTarget.SCREEN_SCRAPER, EditorTarget.IGDB -> 2
        else -> 1
    }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val hasDelete = when (target) {
        EditorTarget.SCREEN_SCRAPER -> settings.screenScraperDevId.isNotBlank()
        EditorTarget.THE_GAMES_DB -> settings.theGamesDbApiKey.isNotBlank()
        EditorTarget.IGDB -> settings.igdbClientId.isNotBlank()
        EditorTarget.STEAM_GRID_DB -> settings.steamGridDbApiKey.isNotBlank()
    }
    val saveIndex = fieldCount
    val deleteIndex = if (hasDelete) fieldCount + 1 else -1
    val cancelIndex = fieldCount + if (hasDelete) 2 else 1

    val title = when (target) {
        EditorTarget.SCREEN_SCRAPER -> stringResource(R.string.settings_scraper_screen_scraper_credentials)
        EditorTarget.THE_GAMES_DB -> stringResource(R.string.settings_scraper_thegamesdb_api_key)
        EditorTarget.IGDB -> stringResource(R.string.settings_scraper_igdb_credentials)
        EditorTarget.STEAM_GRID_DB -> stringResource(R.string.settings_scraper_steam_api_key)
    }

    HorizonOverlay(
        title = title,
        onDismiss = onDismiss,
        onDirectionalKey = { key ->
            when (key) {
                Key.DirectionDown, Key.DirectionRight -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(cancelIndex)
                    true
                }
                Key.DirectionUp, Key.DirectionLeft -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    true
                }
                else -> false
            }
        }
    ) {
        if (fieldCount >= 1) {
            HorizonOverlayTextField(
                value = first,
                onValueChange = { first = it },
                password = target != EditorTarget.THE_GAMES_DB,
                placeholder = when (target) {
                    EditorTarget.SCREEN_SCRAPER -> stringResource(R.string.settings_scraper_screen_scraper_devid_hint)
                    EditorTarget.THE_GAMES_DB -> stringResource(R.string.settings_scraper_thegamesdb_api_key_hint)
                    EditorTarget.IGDB -> stringResource(R.string.settings_scraper_igdb_client_id_hint)
                    EditorTarget.STEAM_GRID_DB -> stringResource(R.string.settings_scraper_steam_api_key_hint)
                },
                selected = selectedIndex == 0
            )
            Spacer(Modifier.height(10.dp))
        }
        if (fieldCount == 2) {
            HorizonOverlayTextField(
                value = second,
                onValueChange = { second = it },
                password = true,
                placeholder = if (target == EditorTarget.SCREEN_SCRAPER)
                    stringResource(R.string.settings_scraper_screen_scraper_devpassword_hint)
                else stringResource(R.string.settings_scraper_igdb_client_secret_hint),
                selected = selectedIndex == 1
            )
            Spacer(Modifier.height(12.dp))
        }

        val canSave = if (fieldCount == 2) first.isNotBlank() && second.isNotBlank() else first.isNotBlank()
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_save),
            selected = selectedIndex == saveIndex,
            enabled = canSave,
            onClick = {
                onSave(
                    when (target) {
                        EditorTarget.SCREEN_SCRAPER -> settings.copy(
                            screenScraperDevId = first.trim(),
                            screenScraperDevPassword = second.trim(),
                            screenScraperSoftName = "HorizonOS"
                        )
                        EditorTarget.THE_GAMES_DB -> settings.copy(theGamesDbApiKey = first.trim())
                        EditorTarget.IGDB -> settings.copy(igdbClientId = first.trim(), igdbClientSecret = second.trim())
                        EditorTarget.STEAM_GRID_DB -> settings.copy(steamGridDbApiKey = first.trim())
                    }
                )
            }
        )
        if (hasDelete) {
            HorizonOverlayChoice(
                title = stringResource(R.string.settings_action_delete),
                selected = selectedIndex == deleteIndex,
                onClick = {
                    onSave(
                        when (target) {
                            EditorTarget.SCREEN_SCRAPER -> settings.copy(screenScraperDevId = "", screenScraperDevPassword = "")
                            EditorTarget.THE_GAMES_DB -> settings.copy(theGamesDbApiKey = "")
                            EditorTarget.IGDB -> settings.copy(igdbClientId = "", igdbClientSecret = "")
                            EditorTarget.STEAM_GRID_DB -> settings.copy(steamGridDbApiKey = "")
                        }
                    )
                }
            )
        }
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_cancel),
            selected = selectedIndex == cancelIndex,
            onClick = onDismiss
        )
    }
}
