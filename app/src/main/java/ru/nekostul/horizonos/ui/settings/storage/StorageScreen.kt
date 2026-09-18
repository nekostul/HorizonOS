package ru.nekostul.horizonos.ui.settings.storage

import android.content.Context
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsDivider
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private fun formatGb(bytes: Long, unit: String): String =
    String.format(Locale.US, "%.1f %s", bytes / 1024.0 / 1024.0 / 1024.0, unit)

private fun standardCapacityLabel(bytes: Long, gbUnit: String, tbUnit: String): String {
    val standards = listOf(4L, 8L, 16L, 32L, 64L, 128L, 256L, 512L, 1024L, 2048L, 4096L)
    val gbDecimal = bytes / 1_000_000_000.0
    val nearest = standards.minByOrNull { kotlin.math.abs(it - gbDecimal) } ?: 256L
    return if (nearest >= 1024) {
        "${nearest / 1024} $tbUnit"
    } else {
        "$nearest $gbUnit"
    }
}

@Composable
fun StorageScreen(context: Context) {
    val stat = StatFs(context.filesDir.absolutePath)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val gbUnit = stringResource(R.string.storage_gb)
    val tbUnit = stringResource(R.string.storage_tb)
    Column {
        Text(stringResource(R.string.settings_storage_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        StorageInfoRow(stringResource(R.string.settings_storage_used), formatGb(total - free, gbUnit))
        StorageInfoRow(stringResource(R.string.settings_storage_free), formatGb(free, gbUnit))
        StorageInfoRow(stringResource(R.string.settings_storage_total), standardCapacityLabel(total, gbUnit, tbUnit))
    }
}

@Composable
private fun StorageInfoRow(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = SettingsWhite, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Text(value, color = SettingsGray, fontSize = 17.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
    }
}
