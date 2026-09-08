package ru.nekostul.horizonos.ui.settings.controllers

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun ControllerMappingScreen() {
    Column {
        Text(stringResource(R.string.settings_controller_setup), color = SettingsWhite, fontSize = 25.sp)
        Text(stringResource(R.string.settings_controller_setup_description), color = SettingsWhite, fontSize = 17.sp)
    }
}
