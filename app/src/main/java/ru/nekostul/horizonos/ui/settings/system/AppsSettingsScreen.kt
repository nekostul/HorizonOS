package ru.nekostul.horizonos.ui.settings.system

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private data class InstalledApp(val label: String, val packageName: String, val version: String)

@Composable
fun AppsSettingsScreen() {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val apps = remember {
        val installed = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") packageManager.getInstalledApplications(0)
        }
        installed.map { info ->
            InstalledApp(
                label = packageManager.getApplicationLabel(info).toString(),
                packageName = info.packageName,
                version = runCatching {
                    @Suppress("DEPRECATION") packageManager.getPackageInfo(info.packageName, 0).versionName
                }.getOrNull() ?: "—"
            )
        }.filter { it.packageName != context.packageName }.sortedBy { it.label.lowercase() }
    }
    var selectedApp by remember { mutableStateOf<InstalledApp?>(null) }
    Column {
        apps.take(40).forEach { app ->
            HorizonSettingRow(
                SettingRow(app.label, app.version, app.packageName),
                selected = selectedApp == app,
                onClick = { selectedApp = app }
            )
        }
    }
    selectedApp?.let { app ->
        HorizonOverlay(
            title = app.label,
            onDismiss = { selectedApp = null }
        ) {
            Text(app.packageName, color = SettingsWhite, fontSize = 16.sp)
            Text(stringResource(R.string.settings_app_version, app.version), color = SettingsWhite, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            HorizonOverlayChoice(
                stringResource(R.string.settings_app_launch),
                selected = true,
                onClick = {
                    packageManager.getLaunchIntentForPackage(app.packageName)?.let(context::startActivity)
                }
            )
            HorizonOverlayChoice(stringResource(R.string.settings_action_cancel), false, { selectedApp = null })
        }
    }
}
