package ru.nekostul.horizonos.ui.settings.system

import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.os.BatteryManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow

@Composable fun BatterySettingsScreen() {
    val context = LocalContext.current
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.takeIf { it >= 0 }?.let { "${it / 10f} °C" }
    val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> stringResource(R.string.settings_battery_charging)
        BatteryManager.BATTERY_STATUS_FULL -> stringResource(R.string.settings_battery_full)
        else -> stringResource(R.string.settings_battery_not_charging)
    }
    val powerManager = context.getSystemService(PowerManager::class.java)
    Column {
        HorizonSettingRow(SettingRow(stringResource(R.string.settings_battery_level), if (level >= 0) "$level%" else stringResource(R.string.settings_status_unavailable_short)), false, {})
        HorizonSettingRow(SettingRow(stringResource(R.string.settings_battery_status), status), false, {})
        HorizonSettingRow(SettingRow(stringResource(R.string.settings_battery_temperature), temperature ?: stringResource(R.string.settings_status_unavailable_short)), false, {})
        HorizonSettingRow(SettingRow(stringResource(R.string.settings_battery_saver), if (powerManager?.isPowerSaveMode == true) stringResource(R.string.settings_status_on) else stringResource(R.string.settings_status_off)), false, {})
    }
}
