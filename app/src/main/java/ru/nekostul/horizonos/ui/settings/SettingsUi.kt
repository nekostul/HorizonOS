package ru.nekostul.horizonos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R

internal val SettingsBackground = Color(0xFF2B2B2B)
internal val SettingsPanel = Color(0xFF333333)
internal val SettingsSelected = Color(0xFF3A3A3A)
internal val SettingsWhite = Color(0xFFF2F2F2)
internal val SettingsGray = Color(0xFFAAAAAA)
internal val SettingsBlue = Color(0xFF00C8FF)

internal data class SettingRow(
    val title: String,
    val value: String = "",
    val description: String = "",
    val enabled: Boolean = true
)

@Composable
internal fun HorizonSettingRow(
    row: SettingRow,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) SettingsSelected else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) SettingsBlue else Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            )
            .clickable(enabled = row.enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.title,
                color = if (row.enabled) SettingsWhite else SettingsGray,
                fontSize = 18.sp
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
            if (row.value.isNotEmpty()) {
                Text(
                    text = row.value,
                    color = if (selected) SettingsBlue else SettingsGray,
                    fontSize = 17.sp
                )
            }
        }
        if (row.description.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(row.description, color = SettingsGray, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    selected: Boolean,
    description: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    HorizonSettingRow(
        row = SettingRow(
            title = title,
            value = stringResource(if (checked) R.string.settings_status_on else R.string.settings_status_off),
            description = description,
            enabled = enabled
        ),
        selected = selected,
        onClick = onClick
    )
}

@Composable
internal fun SettingsSliderRow(
    title: String,
    value: Float,
    selected: Boolean,
    enabled: Boolean = true,
    description: String = "",
    valueLabel: String = "${(value * 100).toInt()}%",
    onValueChange: (Float) -> Unit
) {
    HorizonSettingRow(
        row = SettingRow(title, valueLabel, description, enabled),
        selected = selected,
        onClick = {},
        trailing = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                valueRange = 0f..1f,
                modifier = Modifier.width(190.dp)
            )
        }
    )
}

@Composable
internal fun SettingsPage(
    title: String,
    rows: List<SettingRow>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    footer: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        rows.forEachIndexed { index, row ->
            HorizonSettingRow(
                row = row,
                selected = selectedIndex == index,
                onClick = {
                    onSelectedIndexChange(index)
                    onActivate(index)
                }
            )
        }
        footer?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SettingsGray, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun toggleValue(value: Boolean): String = stringResource(
    if (value) R.string.settings_status_on else R.string.settings_status_off
)

@Composable
internal fun minutesLabel(minutes: Int): String = when (minutes) {
    0 -> stringResource(R.string.timeout_never)
    1 -> stringResource(R.string.timeout_minute)
    60 -> stringResource(R.string.timeout_hour)
    else -> stringResource(R.string.timeout_minutes, minutes)
}

@Composable
internal fun SettingsCapabilitiesNote(text: String) {
    Spacer(Modifier.height(10.dp))
    Text(text, color = SettingsGray, fontSize = 14.sp)
}
