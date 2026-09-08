package ru.nekostul.horizonos.ui.settings.system

import android.os.Build
import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun SystemInfoScreen() {
    val context = LocalContext.current
    val memory = context.getSystemService(ActivityManager::class.java)?.let { manager ->
        ActivityManager.MemoryInfo().also(manager::getMemoryInfo).let { info ->
            info.totalMem / 1024 / 1024 / 1024
        }
    }
    val stat = StatFs(context.filesDir.absolutePath)
    val totalStorage = stat.totalBytes / 1024 / 1024 / 1024
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim()
    } else stringResource(R.string.settings_status_unavailable_short)
    Text(
        stringResource(R.string.settings_system_info) + "\n" +
            stringResource(R.string.horizon_version, "1.0") + "\n" +
            stringResource(R.string.settings_info_device, Build.MANUFACTURER, Build.MODEL) + "\n" +
            stringResource(R.string.android_version, Build.VERSION.RELEASE) + "\n" +
            stringResource(R.string.settings_info_api, Build.VERSION.SDK_INT) + "\n" +
            stringResource(R.string.settings_info_ram, memory ?: 0) + "\n" +
            stringResource(R.string.settings_info_storage, totalStorage) + "\n" +
            stringResource(R.string.settings_info_soc, soc),
        color = SettingsWhite,
        fontSize = 22.sp
    )
}
