package ru.nekostul.horizonos.ui.settings.system

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable fun BatterySettingsScreen() {
    val intent = LocalContext.current.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    Text(stringResource(R.string.settings_system_battery) + "\n" + if (level >= 0) "$level%" else stringResource(R.string.settings_status_unavailable_short), color = SettingsWhite, fontSize = 25.sp)
}
