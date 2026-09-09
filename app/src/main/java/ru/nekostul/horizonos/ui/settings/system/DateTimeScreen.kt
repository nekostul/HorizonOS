package ru.nekostul.horizonos.ui.settings.system

import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsWhite

@Composable
fun DateTimeScreen() {
    val context = LocalContext.current
    val controller = remember { DateTimeController(context) }
    val now = remember { Calendar.getInstance() }
    var automatic by remember { mutableStateOf(controller.automaticTimeEnabled() == true) }
    var showPicker by remember { mutableStateOf(false) }
    var selectedMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val formatted = remember(selectedMillis) {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(selectedMillis))
    }

    Column {
        SettingsToggleRow(
            title = stringResource(R.string.settings_automatic_date_time),
            checked = automatic,
            selected = false,
            enabled = controller.canChange,
            onClick = {
                val next = !automatic
                if (controller.setAutomaticTimeEnabled(next)) automatic = next
            }
        )
        HorizonOverlayChoice(
            title = formatted,
            selected = false,
            enabled = controller.canChange && !automatic,
            onClick = { selectedMillis = System.currentTimeMillis(); showPicker = true }
        )
        HorizonOverlayChoice(
            title = TimeZone.getDefault().id,
            selected = false,
            enabled = false,
            onClick = {}
        )
        if (!controller.canChange) {
            SettingsCapabilitiesNote(stringResource(R.string.settings_datetime_capability))
        }
    }

    if (showPicker) {
        val initial = Calendar.getInstance().apply { timeInMillis = selectedMillis }
        var year by remember { mutableIntStateOf(initial.get(Calendar.YEAR)) }
        var month by remember { mutableIntStateOf(initial.get(Calendar.MONTH)) }
        var day by remember { mutableIntStateOf(initial.get(Calendar.DAY_OF_MONTH)) }
        var hour by remember { mutableIntStateOf(initial.get(Calendar.HOUR_OF_DAY)) }
        var minute by remember { mutableIntStateOf(initial.get(Calendar.MINUTE)) }
        HorizonOverlay(
            title = stringResource(R.string.settings_choose_date_time),
            onDismiss = { showPicker = false }
        ) {
            AndroidView(
                factory = { viewContext ->
                    DatePicker(viewContext).apply {
                        init(year, month, day) { _, newYear, newMonth, newDay ->
                            year = newYear
                            month = newMonth
                            day = newDay
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            AndroidView(
                factory = { viewContext ->
                    TimePicker(viewContext).apply {
                        setIs24HourView(true)
                        hour = hour
                        minute = minute
                        setOnTimeChangedListener { _, newHour, newMinute ->
                            hour = newHour
                            minute = newMinute
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                HorizonOverlayChoice(
                    stringResource(R.string.settings_action_apply),
                    selected = true,
                    onClick = {
                        val result = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (controller.setDateTime(result)) {
                            selectedMillis = result
                            showPicker = false
                        }
                    }
                )
                HorizonOverlayChoice(stringResource(R.string.settings_action_cancel), false, { showPicker = false })
            }
        }
    }
}
