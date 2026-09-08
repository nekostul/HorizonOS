package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun DateTimeScreen() {
    Text(stringResource(R.string.settings_system_date_time) + "\n" + SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()), color = SettingsWhite, fontSize = 25.sp)
}
