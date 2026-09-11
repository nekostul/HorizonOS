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
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanningSettingsScreen

/**
 * "Launcher Settings" category. Currently hosts the scanning data sources
 * screen. Kept separate from the system settings category on purpose.
 */
@Composable
fun LauncherSettingsHubScreen(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    openOverlayIndex: Int? = null,
    onOverlayRequestConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var showScanning by remember { mutableStateOf(false) }

    LaunchedEffect(openOverlayIndex) {
        openOverlayIndex?.let {
            showScanning = true
            onOverlayRequestConsumed()
        }
    }

    Column {
        Text(stringResource(R.string.settings_category_launcher), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_launcher_scanning),
                description = stringResource(R.string.settings_launcher_scanning_description)
            ),
            selected = selectedIndex == 0,
            onClick = {
                onSelect(0)
                showScanning = true
            }
        )
    }

    if (showScanning) {
        ScanningSettingsScreen(
            context = context,
            onDismiss = { showScanning = false }
        )
    }
}
