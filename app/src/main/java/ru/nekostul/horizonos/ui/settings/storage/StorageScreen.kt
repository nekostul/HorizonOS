package ru.nekostul.horizonos.ui.settings.storage

import android.content.Context
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import java.util.Locale
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private fun formatGb(bytes: Long, unit: String): String = String.format(Locale.US, "%.1f %s", bytes / 1024.0 / 1024.0 / 1024.0, unit)

@Composable
fun StorageScreen(context: Context, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val stat = StatFs(context.filesDir.absolutePath)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val gigabytes = stringResource(R.string.storage_gb)
    val rows = listOf(
        SettingRow(stringResource(R.string.settings_storage_used), formatGb(total - free, gigabytes)), SettingRow(stringResource(R.string.settings_storage_free), formatGb(free, gigabytes)), SettingRow(stringResource(R.string.settings_storage_total), formatGb(total, gigabytes)),
        SettingRow(stringResource(R.string.settings_storage_games), stringResource(R.string.settings_status_unavailable_short)), SettingRow(stringResource(R.string.settings_storage_apps), stringResource(R.string.settings_status_unavailable_short)), SettingRow(stringResource(R.string.settings_storage_images), stringResource(R.string.settings_status_unavailable_short)),
        SettingRow(stringResource(R.string.settings_storage_videos), stringResource(R.string.settings_status_unavailable_short)), SettingRow(stringResource(R.string.settings_storage_music), stringResource(R.string.settings_status_unavailable_short)), SettingRow(stringResource(R.string.settings_storage_files), stringResource(R.string.settings_status_unavailable_short)), SettingRow(stringResource(R.string.settings_storage_other), stringResource(R.string.settings_status_unavailable_short))
    )
    Column {
        Text(stringResource(R.string.settings_storage_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { index, row -> HorizonSettingRow(row, selectedIndex == index, onClick = { onSelect(index) }) }
    }
}
