package ru.nekostul.horizonos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalView
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import androidx.compose.runtime.LaunchedEffect

internal val SettingsBackground: Color
    @Composable get() = LocalHorizonColors.current.background
internal val SettingsPanel: Color
    @Composable get() = LocalHorizonColors.current.panel
internal val SettingsOverlayPanel: Color
    @Composable get() = LocalHorizonColors.current.overlayPanel
internal val SettingsSelected: Color
    @Composable get() = LocalHorizonColors.current.selected
internal val SettingsWhite: Color
    @Composable get() = LocalHorizonColors.current.text
internal val SettingsGray: Color
    @Composable get() = LocalHorizonColors.current.mutedText
internal val SettingsBlue: Color
    @Composable get() = LocalHorizonColors.current.accent
internal val SettingsDivider: Color
    @Composable get() = LocalHorizonColors.current.divider
internal val SettingsTrack: Color
    @Composable get() = SettingsGray.copy(alpha = 0.30f)
internal val SettingsThumb: Color
    @Composable get() = SettingsGray

internal val SelectionFrameBlue = Color(0xFF08A8E6)
internal const val SelectionPulseDurationMillis = 560

internal val SettingsAccentTeal = Color(0xFF00B180)

internal val LocalSettingsRightMenuFocused = compositionLocalOf { false }

internal val LocalSliderEditing = compositionLocalOf { false }

internal enum class SettingsInputMode { TOUCH, GAMEPAD }

internal val LocalSettingsInputMode =
    compositionLocalOf<MutableState<SettingsInputMode>?> { null }

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
    trailing: (@Composable () -> Unit)? = null,
    valueColor: Color? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "settingsRowSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsRowSelectionPulseValue"
    )
    val rightMenuFocused = LocalSettingsRightMenuFocused.current
    val localView = LocalView.current
    val frameActive = inputMode?.value == SettingsInputMode.GAMEPAD &&
        rightMenuFocused && selected
    LaunchedEffect(frameActive) {
        if (frameActive) bringIntoViewRequester.bringIntoView()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (frameActive) SettingsDivider.copy(alpha = 0.24f) else Color.Transparent)
            .then(
                if (frameActive) Modifier.border(
                    width = 3.dp,
                    color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                ) else Modifier
            )
            .clickable(enabled = row.enabled) {
                inputMode?.value = SettingsInputMode.TOUCH
                LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                LauncherAudioManager.performHapticFeedback(localView)
                onClick()
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusable(row.enabled)
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(vertical = 8.dp),
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
                    color = valueColor ?: SettingsGray,
                    fontSize = 17.sp
                )
            }
        }
        if (row.description.isNotEmpty()) {
            Text(
                row.description,
                color = SettingsGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
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
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsSliderRow(
    title: String,
    value: Float,
    selected: Boolean,
    enabled: Boolean = true,
    description: String = "",
    valueLabel: String = "${(value * 100).toInt()}%",
    leadingIcon: (@Composable () -> Unit)? = null,
    onValueChange: (Float) -> Unit
) {
    val activeTrackColor = SettingsBlue
    val inactiveTrackColor = SettingsTrack
    val rightMenuFocused = LocalSettingsRightMenuFocused.current
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "settingsSliderSelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsSliderSelectionPulseValue"
    )
    val frameActive = inputMode?.value == SettingsInputMode.GAMEPAD &&
        rightMenuFocused && selected
    val sliderEditing = LocalSliderEditing.current && selected && enabled
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(frameActive) {
        if (frameActive) bringIntoViewRequester.bringIntoView()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (frameActive) Modifier.border(
                    width = 3.dp,
                    color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                ) else Modifier
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .padding(horizontal = 14.dp)
    ) {
        if (title.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = if (enabled) SettingsWhite else SettingsGray, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text(valueLabel, color = SettingsGray, fontSize = 17.sp)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(18.dp))
            Slider(
                value = value,
                onValueChange = {
                    inputMode?.value = SettingsInputMode.TOUCH
                    onValueChange(it)
                },
                enabled = enabled,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = activeTrackColor,
                    inactiveTrackColor = inactiveTrackColor,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = SettingsThumb.copy(alpha = 0.45f),
                    disabledActiveTrackColor = inactiveTrackColor,
                    disabledInactiveTrackColor = inactiveTrackColor
                ),
                track = { sliderState ->
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        val centerY = size.height / 2f
                        val endX = size.width * sliderState.coercedValueAsFraction
                        drawLine(
                            color = inactiveTrackColor,
                            start = androidx.compose.ui.geometry.Offset(0f, centerY),
                            end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = if (enabled) activeTrackColor else inactiveTrackColor,
                            start = androidx.compose.ui.geometry.Offset(0f, centerY),
                            end = androidx.compose.ui.geometry.Offset(endX, centerY),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                },
                thumb = {
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(SettingsThumb, CircleShape)
                            .border(
                                width = if (sliderEditing) 3.dp else 1.dp,
                                color = if (sliderEditing) {
                                    SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                                } else {
                                    SettingsDivider
                                },
                                shape = CircleShape
                            )
                    )
                }
            )
        }
        if (description.isNotEmpty()) {
            Text(
                description,
                color = SettingsGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 9.dp)
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsDivider.copy(alpha = 0.38f)))
    }
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
    Spacer(Modifier.height(4.dp))
    Text(text, color = SettingsGray, fontSize = 12.sp, lineHeight = 16.sp)
}
