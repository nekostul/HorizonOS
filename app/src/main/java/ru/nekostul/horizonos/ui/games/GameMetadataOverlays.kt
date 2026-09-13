package ru.nekostul.horizonos.ui.games

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonOverlayTextField
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.games.Platform

/**
 * Small notice shown when the silent launch rescan added new games.
 * Names the platforms that gained games.
 */
@Composable
fun NewGamesAddedOverlay(
    platforms: List<Platform>,
    onDismiss: () -> Unit
) {
    HorizonOverlay(title = stringResource(R.string.new_games_title), onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.new_games_message, platformNames(platforms)),
            color = SettingsGray,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.action_ok),
            selected = true,
            onClick = onDismiss
        )
    }
}

@Composable
private fun platformNames(platforms: List<Platform>): String {
    val names = platforms.map { platform ->
        when (platform) {
            Platform.PLAYSTATION_1 -> stringResource(R.string.games_platform_ps1)
            Platform.PSP -> stringResource(R.string.games_platform_psp)
            Platform.PLAYSTATION_2 -> stringResource(R.string.games_platform_ps2)
            Platform.GAMECUBE_WII -> stringResource(R.string.games_platform_gamecube_wii)
            Platform.ANDROID -> stringResource(R.string.games_platform_android)
        }
    }
    return names.joinToString(", ")
}

/**
 * Per-game scan menu. Offers an automatic scan from the configured sources or a
 * fully manual pick of a custom cover/screenshot from local files.
 */
@Composable
fun GameScanMenuOverlay(
    game: Game,
    onAutoScan: () -> Unit,
    onCustomCover: () -> Unit,
    onCustomScreenshot: () -> Unit,
    onDismiss: () -> Unit
) {
    HorizonOverlay(title = stringResource(R.string.games_scan_title), onDismiss = onDismiss) {
        HorizonOverlayChoice(
            title = stringResource(R.string.games_scan_auto),
            selected = true,
            onClick = onAutoScan
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.games_scan_custom_cover),
            selected = false,
            onClick = onCustomCover
        )
        Text(
            text = stringResource(R.string.games_scan_custom_cover_hint),
            color = SettingsGray,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.games_scan_custom_screenshot),
            selected = false,
            onClick = onCustomScreenshot
        )
        Text(
            text = stringResource(R.string.games_scan_custom_screenshot_hint),
            color = SettingsGray,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )
    }
}

/** Manual title override dialog. The ROM file is never modified. */
@Composable
fun GameTitleEditorOverlay(
    game: Game,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(game.displayTitle) }
    HorizonOverlay(
        title = stringResource(R.string.games_edit_title),
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(R.string.games_title_current, game.displayTitle),
            color = SettingsGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizonOverlayTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = stringResource(R.string.games_title_hint),
            selected = true
        )
        Spacer(Modifier.height(12.dp))
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_save),
            selected = true,
            enabled = value.isNotBlank(),
            onClick = { onSave(value.trim()) }
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_cancel),
            selected = false,
            onClick = onDismiss
        )
    }
}
