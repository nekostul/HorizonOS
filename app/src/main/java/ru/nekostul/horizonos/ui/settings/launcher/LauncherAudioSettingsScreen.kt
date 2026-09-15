package ru.nekostul.horizonos.ui.settings.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSoundMode
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.LocalSettingsRightMenuFocused
import ru.nekostul.horizonos.ui.settings.LocalSliderEditing
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow

@Composable
fun LauncherAudioSettingsScreen(
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onSoundModeChange: (String) -> Unit,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onHapticChange: (Boolean) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    fun activate(index: Int) {
        when (index) {
            0 -> onSoundModeChange(LauncherSoundMode.next(settings.soundMode))
            1 -> onMusicEnabledChange(!settings.backgroundMusicEnabled)
            3 -> onHapticChange(!settings.hapticFeedbackEnabled)
        }
    }

    HorizonOverlay(
        title = stringResource(R.string.settings_launcher_audio_title),
        onDismiss = onDismiss,
        onDirectionalKey = { key ->
            when {
                selectedIndex == 2 && key == Key.DirectionLeft -> {
                    onMusicVolumeChange((settings.backgroundMusicVolume - 0.05f).coerceAtLeast(0f))
                    true
                }
                selectedIndex == 2 && key == Key.DirectionRight -> {
                    onMusicVolumeChange((settings.backgroundMusicVolume + 0.05f).coerceAtMost(1f))
                    true
                }
                key == Key.DirectionDown -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(3)
                    true
                }
                key == Key.DirectionUp -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    true
                }
                key == Key.DirectionLeft -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    true
                }
                key == Key.DirectionRight -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(3)
                    true
                }
                else -> false
            }
        }
    ) {
        CompositionLocalProvider(
            LocalSettingsRightMenuFocused provides true,
            LocalSliderEditing provides (selectedIndex == 2)
        ) {
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_launcher_sound_mode),
            value = when (settings.soundMode) {
                LauncherSoundMode.OFF -> stringResource(R.string.settings_launcher_sound_mode_off)
                LauncherSoundMode.GAMEPAD_ONLY -> stringResource(R.string.settings_launcher_sound_mode_gamepad)
                else -> stringResource(R.string.settings_launcher_sound_mode_all)
            },
            selected = selectedIndex == 0,
            onClick = { activate(0) }
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_launcher_background_music),
            value = if (settings.backgroundMusicEnabled) {
                stringResource(R.string.settings_status_on)
            } else {
                stringResource(R.string.settings_status_off)
            },
            selected = selectedIndex == 1,
            onClick = { activate(1) }
        )
        SettingsSliderRow(
            title = stringResource(R.string.settings_launcher_background_music_volume),
            value = settings.backgroundMusicVolume,
            selected = selectedIndex == 2,
            enabled = settings.backgroundMusicEnabled,
            description = stringResource(R.string.settings_launcher_background_music_volume_description),
            onValueChange = onMusicVolumeChange
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_launcher_haptic_feedback),
            value = if (settings.hapticFeedbackEnabled) {
                stringResource(R.string.settings_status_on)
            } else {
                stringResource(R.string.settings_status_off)
            },
            selected = selectedIndex == 3,
            onClick = { activate(3) }
        )
        androidx.compose.material3.Text(
            text = stringResource(R.string.settings_launcher_audio_hint),
            color = SettingsGray,
            modifier = androidx.compose.ui.Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp
        )
        }
    }
}
