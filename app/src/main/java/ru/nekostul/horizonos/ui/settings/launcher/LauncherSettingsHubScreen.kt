package ru.nekostul.horizonos.ui.settings.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanningSettingsScreen

@Composable
fun LauncherSettingsHubScreen(
    settings: LauncherSettings,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSoundModeChange: (String) -> Unit,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onHapticChange: (Boolean) -> Unit,
    openOverlayIndex: Int? = null,
    onOverlayRequestConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAudio by remember { mutableStateOf(false) }
    var showScanning by remember { mutableStateOf(false) }

    LaunchedEffect(openOverlayIndex) {
        openOverlayIndex?.let {
            when (it) {
                0 -> showAudio = true
                3 -> showScanning = true
            }
            onOverlayRequestConsumed()
        }
    }

    Column {
        Text(stringResource(R.string.settings_category_launcher), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_launcher_audio_title),
                description = stringResource(R.string.settings_launcher_audio_hint)
            ),
            selected = selectedIndex == 0,
            onClick = {
                onSelect(0)
                showAudio = true
            }
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_launcher_screenshot_background),
            checked = settings.screenshotBackgroundEnabled,
            selected = selectedIndex == 1,
            description = stringResource(R.string.settings_launcher_screenshot_background_description),
            onClick = { onSelect(1) }
        )
        SettingsToggleRow(
            title = stringResource(R.string.settings_launcher_show_download_progress),
            checked = settings.showDownloadProgress,
            selected = selectedIndex == 2,
            description = stringResource(R.string.settings_launcher_show_download_progress_description),
            onClick = { onSelect(2) }
        )
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_launcher_scanning),
                description = stringResource(R.string.settings_launcher_scanning_description)
            ),
            selected = selectedIndex == 3,
            onClick = {
                onSelect(3)
                showScanning = true
            }
        )
    }

    if (showAudio) {
        LauncherAudioSettingsScreen(
            settings = settings,
            onDismiss = { showAudio = false },
            onSoundModeChange = onSoundModeChange,
            onMusicEnabledChange = onMusicEnabledChange,
            onMusicVolumeChange = onMusicVolumeChange,
            onHapticChange = onHapticChange
        )
    }

    if (showScanning) {
        ScanningSettingsScreen(
            context = context,
            onDismiss = { showScanning = false }
        )
    }
}
