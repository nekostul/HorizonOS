package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable fun DisplaySettingsScreen() = Text(stringResource(R.string.system_display_body), color = SettingsWhite, fontSize = 22.sp)
