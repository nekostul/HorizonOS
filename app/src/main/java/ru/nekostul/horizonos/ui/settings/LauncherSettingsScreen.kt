package ru.nekostul.horizonos.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.airplane.AirplaneModeScreen
import ru.nekostul.horizonos.ui.settings.brightness.BrightnessScreen
import ru.nekostul.horizonos.ui.settings.controllers.ControllersScreen
import ru.nekostul.horizonos.ui.settings.controllers.ControllerManager
import ru.nekostul.horizonos.ui.settings.notifications.NotificationController
import ru.nekostul.horizonos.ui.settings.lockscreen.LockScreenScreen
import ru.nekostul.horizonos.ui.settings.notifications.NotificationsScreen
import ru.nekostul.horizonos.ui.settings.sleep.SleepScreen
import ru.nekostul.horizonos.ui.settings.storage.StorageScreen
import ru.nekostul.horizonos.ui.settings.system.SystemScreen
import ru.nekostul.horizonos.ui.settings.themes.ThemesScreen
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.isHorizonConfirmKey

private data class SettingsCategory(val titleRes: Int, val dividerAfter: Boolean = false)

private val settingsCategories = listOf(
    SettingsCategory(R.string.settings_category_airplane), SettingsCategory(R.string.settings_category_brightness),
    SettingsCategory(R.string.settings_category_bluetooth), SettingsCategory(R.string.settings_category_lock_screen, true),
    SettingsCategory(R.string.settings_category_wifi), SettingsCategory(R.string.settings_category_storage, true),
    SettingsCategory(R.string.settings_category_themes), SettingsCategory(R.string.settings_category_notifications),
    SettingsCategory(R.string.settings_category_sleep, true), SettingsCategory(R.string.settings_category_controllers),
    SettingsCategory(R.string.settings_category_system)
)

@Composable
fun LauncherSettingsScreen(
    onBack: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { LauncherSettingsRepository(context) }
    val settings by repository.settings.collectAsState(initial = LauncherSettings())
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(0) }
    var rightFocus by remember { mutableStateOf(false) }
    var systemOverlayRequest by remember { mutableStateOf<Int?>(null) }
    val overlayVisible = remember { mutableStateOf(false) }
    val overlayBackHandlers = remember { mutableStateListOf<() -> Unit>() }
    val settingsFocusRequester = remember { FocusRequester() }
    val leftListState = rememberLazyListState()
    val rightScrollState = rememberScrollState()
    val inputMode = remember { mutableStateOf(SettingsInputMode.TOUCH) }
    var wifiActivationRequest by remember { mutableIntStateOf(0) }
    var sleepActivationRequest by remember { mutableIntStateOf(0) }
    var bluetoothItemCount by remember { mutableIntStateOf(2) }

    fun optionCount(category: Int): Int = when (category) {
        0 -> if (settings.airplaneMode) 3 else 1
        1 -> 2
        2 -> bluetoothItemCount
        3 -> 6
        4 -> ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context).availableNetworkNames().size + 3
        5 -> 10
        6 -> 2
        7 -> NotificationController(context).installedApps().size + 2
        8 -> 2
        9 -> ControllerManager.connectedControllers().size + 4
        else -> 8
    }

    LaunchedEffect(selectedCategory) {
        selectedOption = 0
        rightFocus = false
    }

    fun requestRuntimePermissions(category: Int) {
        val permissions = buildList {
            if (category == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.BLUETOOTH_CONNECT)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (category == 4 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (category == 7 && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) onRequestPermissions(permissions.toTypedArray())
    }

    fun activateOption() {
        scope.launch {
            when (selectedCategory) {
                0 -> when (selectedOption) {
                    0 -> {
                        val controller = ru.nekostul.horizonos.ui.settings.AirplaneModeController(context)
                        val next = !(controller.currentState() ?: false)
                        if (controller.setEnabled(next)) repository.setAirplaneMode(next)
                    }
                    1 -> repository.setAirplaneWifiAllowed(!settings.airplaneWifiAllowed)
                    2 -> repository.setAirplaneBluetoothAllowed(!settings.airplaneBluetoothAllowed)
                }
                1 -> if (selectedOption == 0) {
                    val enabled = !settings.autoBrightness
                    val controller = ru.nekostul.horizonos.ui.settings.brightness.BrightnessController(context)
                    if (controller.setAutomaticBrightnessEnabled(enabled)) repository.setAutoBrightness(enabled)
                }
                2 -> if (selectedOption == 0) {
                    val controller = ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothSettingsController(context)
                    controller.setEnabled(controller.enabled() != true)
                }
                3 -> when (selectedOption) {
                    0 -> repository.setLockScreenEnabled(!settings.lockScreenEnabled)
                    in 1..5 -> {
                        val timeout = listOf(0, 1, 5, 10, 30)[selectedOption - 1]
                        repository.setLockScreenTimeoutMinutes(timeout)
                        ru.nekostul.horizonos.ui.settings.lockscreen.LockScreenController(context)
                            .setSystemTimeout(timeout * 60 * 1000)
                    }
                }
                4 -> when (selectedOption) {
                    0 -> {
                        val controller = ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context)
                        val next = controller.enabled() != true
                        if (controller.setEnabled(next)) repository.setWifiEnabled(next)
                    }
                    2 -> ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context).scan()
                    in 3..Int.MAX_VALUE -> wifiActivationRequest++
                }
                6 -> repository.setTheme(if (selectedOption == 0) "dark" else "light")
                7 -> if (selectedOption == 0) repository.setNotificationsEnabled(!settings.notificationsEnabled)
                8 -> when (selectedOption) {
                    0 -> sleepActivationRequest++
                    1 -> repository.setSleepMediaEnabled(!settings.sleepMediaEnabled)
                }
                9 -> if (selectedOption == 1) repository.setVibrationEnabled(!settings.vibrationEnabled)
                10 -> systemOverlayRequest = selectedOption
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedOption = 0
        rightFocus = false
        leftListState.animateScrollToItem(selectedCategory)
        requestRuntimePermissions(selectedCategory)
    }
    LaunchedEffect(Unit) {
        settingsFocusRequester.requestFocus()
    }
    LaunchedEffect(settings.airplaneMode) {
        if (selectedCategory == 0) selectedOption = selectedOption.coerceIn(0, optionCount(0) - 1)
    }

    CompositionLocalProvider(
        LocalSettingsOverlayVisible provides overlayVisible,
        LocalSettingsInputMode provides inputMode
    ) {
    // Only the controller B button navigates back. Native Android Back is
    // consumed by MainActivity and never reaches this screen.
    fun handleControllerBack() {
        val dismissOverlay = overlayBackHandlers.lastOrNull()
        if (dismissOverlay != null) {
            dismissOverlay()
        } else if (rightFocus) {
            rightFocus = false
        } else {
            onBack()
        }
    }

    CompositionLocalProvider(
        LocalSettingsOverlayBackHandlers provides overlayBackHandlers
    ) {
    Box(Modifier
        .fillMaxSize()
        .background(SettingsBackground)
        .focusRequester(settingsFocusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        inputMode.value = SettingsInputMode.GAMEPAD
        if (isHorizonConfirmKey(event)) {
            if (rightFocus) activateOption() else rightFocus = true
            return@onPreviewKeyEvent true
        }

        when (event.key) {
            Key.DirectionUp -> { if (rightFocus) selectedOption = (selectedOption - 1).coerceAtLeast(0) else selectedCategory = (selectedCategory - 1).coerceAtLeast(0); true }
            Key.DirectionDown -> { if (rightFocus) selectedOption = (selectedOption + 1).coerceAtMost(optionCount(selectedCategory) - 1) else selectedCategory = (selectedCategory + 1).coerceAtMost(settingsCategories.lastIndex); true }
            Key.DirectionRight -> { rightFocus = true; true }
            Key.DirectionLeft -> { rightFocus = false; true }
            Key.ButtonB -> { handleControllerBack(); true }
            else -> false
        }
    }) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (overlayVisible.value) Modifier.blur(7.dp) else Modifier)
                .padding(horizontal = 30.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.setting),
                    contentDescription = null,
                    modifier = Modifier.width(32.dp).height(32.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SettingsWhite)
                )
                Spacer(Modifier.width(15.dp))
                Text(stringResource(R.string.horizon_settings_title), color = SettingsWhite, fontSize = 25.sp)
            }
            Spacer(Modifier.height(11.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(state = leftListState, modifier = Modifier.fillMaxHeight().weight(0.34f).background(SettingsPanel), contentPadding = PaddingValues(vertical = 6.dp)) {
                    itemsIndexed(settingsCategories) { index, category ->
                        SettingsCategoryRow(stringResource(category.titleRes), selectedCategory == index, selectedCategory == index && !rightFocus) {
                            selectedCategory = index
                            rightFocus = false
                        }
                        if (category.dividerAfter) Box(Modifier.fillMaxWidth().height(10.dp).background(SettingsBackground))
                    }
                }
                Spacer(Modifier.width(24.dp))
                Column(Modifier.fillMaxHeight().weight(0.66f).verticalScroll(rightScrollState)) {
                    CompositionLocalProvider(LocalSettingsRightMenuFocused provides rightFocus) {
                        SettingsContent(
                            context,
                            selectedCategory,
                            settings,
                            selectedOption,
                            repository,
                            scope,
                            { selectedOption = it; rightFocus = true; activateOption() },
                            { value ->
                                scope.launch {
                                    repository.setBrightness(value)
                                    ru.nekostul.horizonos.ui.settings.brightness.BrightnessController(context)
                                        .setGlobalBrightness(value)
                                }
                            },
                            { scope.launch { repository.setTheme(it) } }
                            ,systemOverlayRequest,
                            { systemOverlayRequest = null },
                            wifiActivationRequest,
                            sleepActivationRequest,
                            { count ->
                                bluetoothItemCount = count
                                selectedOption = selectedOption.coerceIn(0, (count - 1).coerceAtLeast(0))
                            }
                        )
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
            Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                SettingsFooterButton(
                    glyph = "B",
                    label = stringResource(R.string.settings_action_back),
                    onClick = { handleControllerBack() }
                )
                Spacer(Modifier.width(25.dp))
                SettingsFooterButton(
                    glyph = "A",
                    label = stringResource(R.string.settings_action_select),
                    onClick = {
                        if (rightFocus) activateOption() else rightFocus = true
                    }
                )
            }
        }
    }
    }
}

}


@Composable
private fun SettingsContent(
    context: Context,
    category: Int,
    settings: LauncherSettings,
    selectedOption: Int,
    repository: LauncherSettingsRepository,
    scope: CoroutineScope,
    onOptionSelected: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onThemeSelected: (String) -> Unit,
    systemOverlayRequest: Int?,
    onSystemOverlayConsumed: () -> Unit,
    wifiActivationRequest: Int,
    sleepActivationRequest: Int,
    onBluetoothItemCountChange: (Int) -> Unit
) {
    when (category) {
        0 -> AirplaneModeScreen(context, settings, selectedOption, { onOptionSelected(0) }, { onOptionSelected(1) }, { onOptionSelected(2) })
        1 -> BrightnessScreen(settings, selectedOption, { onOptionSelected(0) }, onBrightnessChange)
        2 -> ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothScreen(
            selectedIndex = selectedOption,
            onSelect = onOptionSelected,
            onItemCountChange = onBluetoothItemCountChange
        ) {
            val controller = ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothSettingsController(context)
            controller.setEnabled(controller.enabled() != true)
        }
        3 -> LockScreenScreen(context, settings, selectedOption, { onOptionSelected(0) }, { timeout -> scope.launch { repository.setLockScreenTimeoutMinutes(timeout) } })
        4 -> ru.nekostul.horizonos.ui.settings.wifi.WifiScreen(
            selectedIndex = selectedOption,
            onSelect = onOptionSelected,
            activationRequest = wifiActivationRequest
        ) {
            val controller = ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context)
            controller.setEnabled(controller.enabled() != true)
        }
        5 -> StorageScreen(context, selectedOption, onOptionSelected)
        6 -> ThemesScreen(settings, selectedOption, onThemeSelected)
        7 -> NotificationsScreen(settings, selectedOption) { onOptionSelected(0) }
        8 -> SleepScreen(
            context = context,
            settings = settings,
            selectedIndex = selectedOption,
            activationRequest = sleepActivationRequest,
            onTimeoutSelected = { timeout -> scope.launch { repository.setSleepTimeoutMinutes(timeout) } },
            onMediaToggle = { scope.launch { repository.setSleepMediaEnabled(!settings.sleepMediaEnabled) } }
        )
        9 -> ControllersScreen(settings, selectedOption, { scope.launch { repository.setVibrationEnabled(!settings.vibrationEnabled) } }, { value -> scope.launch { repository.setControllerSensitivity(value) } }, { value -> scope.launch { repository.setControllerDeadZone(value) } })
        else -> SystemScreen(
            settings = settings,
            language = settings.language,
            selectedIndex = selectedOption,
            onSelect = onOptionSelected,
            onLanguageSelected = { language -> scope.launch { repository.setLanguage(language) } },
            openOverlayIndex = systemOverlayRequest,
            onOverlayRequestConsumed = onSystemOverlayConsumed
        )
    }
}

@Composable
private fun SettingsCategoryRow(text: String, selected: Boolean, focused: Boolean, onClick: () -> Unit) {
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "settingsCategorySelectionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settingsCategorySelectionPulseValue"
    )
    val frameActive = inputMode?.value == SettingsInputMode.GAMEPAD && focused
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (frameActive) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                    )
                } else Modifier
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(if (selected) 4.dp else 0.dp).height(38.dp).background(if (selected) SettingsBlue else Color.Transparent))
        Spacer(Modifier.width(if (selected) 13.dp else 17.dp))
        Text(text, color = SettingsWhite, fontSize = 18.sp)
    }
}

@Composable
private fun SettingsFooterButton(
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    Row(
        modifier = Modifier
            .height(44.dp)
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizonButtonGlyph(label = glyph, size = 28.dp)
        Spacer(Modifier.width(7.dp))
        Text(label, color = SettingsWhite, fontSize = 16.sp)
    }
}
