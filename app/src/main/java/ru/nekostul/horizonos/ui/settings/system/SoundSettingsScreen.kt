package ru.nekostul.horizonos.ui.settings.system

import android.media.AudioManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.SettingsSliderRow
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow

@Composable
fun SoundSettingsScreen(settings: LauncherSettings, onInterfaceSoundsToggle: () -> Unit) {
    val audio = LocalContext.current.getSystemService(AudioManager::class.java)
    val current = audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    val maximum = audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
    Column {
        SettingsSliderRow(
            title = stringResource(R.string.settings_media_volume),
            value = current.toFloat() / maximum,
            selected = false,
            valueLabel = "${current * 100 / maximum}%",
            onValueChange = { value ->
                audio?.setStreamVolume(AudioManager.STREAM_MUSIC, (value * maximum).toInt(), 0)
            }
        )
        Spacer(Modifier.height(8.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_interface_sounds),
            checked = settings.interfaceSounds,
            selected = false,
            onClick = onInterfaceSoundsToggle
        )
    }
}
