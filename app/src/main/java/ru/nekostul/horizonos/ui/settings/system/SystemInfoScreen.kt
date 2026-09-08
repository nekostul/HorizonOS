package ru.nekostul.horizonos.ui.settings.system

import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable fun SystemInfoScreen() = Text(stringResource(R.string.settings_system_info) + "\n" + stringResource(R.string.horizon_version, "1.0") + "\n${Build.MANUFACTURER} ${Build.MODEL}\n" + stringResource(R.string.android_version, Build.VERSION.RELEASE), color = SettingsWhite, fontSize = 22.sp)
