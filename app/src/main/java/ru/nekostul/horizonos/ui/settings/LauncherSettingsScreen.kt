package ru.nekostul.horizonos.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.airplane.AirplaneModeScreen
import ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothScreen
import ru.nekostul.horizonos.ui.settings.brightness.BrightnessScreen
import ru.nekostul.horizonos.ui.settings.controllers.ControllersScreen
import ru.nekostul.horizonos.ui.settings.lockscreen.LockScreenScreen
import ru.nekostul.horizonos.ui.settings.notifications.NotificationsScreen
import ru.nekostul.horizonos.ui.settings.sleep.SleepScreen
import ru.nekostul.horizonos.ui.settings.storage.StorageScreen
import ru.nekostul.horizonos.ui.settings.system.SystemScreen
import ru.nekostul.horizonos.ui.settings.themes.ThemesScreen
import ru.nekostul.horizonos.ui.settings.wifi.WifiScreen

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
    val leftListState = rememberLazyListState()

    fun optionCount(category: Int): Int = when (category) {
        0 -> if (settings.airplaneMode) 3 else 1
        1, 3 -> 2
        2 -> 2
        4 -> ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context).availableNetworkNames().size + 3
        5 -> 10
        6, 7 -> 2
        8 -> 6
        9 -> 5
        else -> 8
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
                    0 -> repository.setAirplaneMode(!settings.airplaneMode)
                    1 -> repository.setAirplaneWifiAllowed(!settings.airplaneWifiAllowed)
                    2 -> repository.setAirplaneBluetoothAllowed(!settings.airplaneBluetoothAllowed)
                }
                1 -> if (selectedOption == 0) repository.setAutoBrightness(!settings.autoBrightness)
                3 -> if (selectedOption == 0) repository.setLockScreenEnabled(!settings.lockScreenEnabled)
                6 -> repository.setTheme(if (selectedOption == 0) "dark" else "light")
                7 -> if (selectedOption == 0) repository.setNotificationsEnabled(!settings.notificationsEnabled)
                8 -> if (selectedOption == 0) repository.setSleepEnabled(!settings.sleepEnabled)
                9 -> if (selectedOption == 1) repository.setVibrationEnabled(!settings.vibrationEnabled)
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedOption = 0
        rightFocus = false
        leftListState.animateScrollToItem(selectedCategory)
        requestRuntimePermissions(selectedCategory)
    }
    LaunchedEffect(settings.airplaneMode) {
        if (selectedCategory == 0) selectedOption = selectedOption.coerceIn(0, optionCount(0) - 1)
    }

    Box(Modifier.fillMaxSize().background(SettingsBackground).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> { if (rightFocus) selectedOption = (selectedOption - 1).coerceAtLeast(0) else selectedCategory = (selectedCategory - 1).coerceAtLeast(0); true }
            Key.DirectionDown -> { if (rightFocus) selectedOption = (selectedOption + 1).coerceAtMost(optionCount(selectedCategory) - 1) else selectedCategory = (selectedCategory + 1).coerceAtMost(settingsCategories.lastIndex); true }
            Key.DirectionRight -> { rightFocus = true; true }
            Key.DirectionLeft -> { rightFocus = false; true }
            Key.Enter, Key.NumPadEnter -> { if (rightFocus) activateOption() else rightFocus = true; true }
            Key.Escape, Key.Back -> { onBack(); true }
            else -> false
        }
    }) {
        Column(Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚙", color = SettingsWhite, fontSize = 30.sp)
                Spacer(Modifier.width(15.dp))
                Text(stringResource(R.string.horizon_settings_title), color = SettingsWhite, fontSize = 25.sp)
            }
            Spacer(Modifier.height(11.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(state = leftListState, modifier = Modifier.fillMaxHeight().weight(0.43f).background(SettingsPanel), contentPadding = PaddingValues(vertical = 6.dp)) {
                    itemsIndexed(settingsCategories) { index, category ->
                        SettingsCategoryRow(stringResource(category.titleRes), selectedCategory == index, selectedCategory == index && !rightFocus) {
                            selectedCategory = index
                            rightFocus = false
                        }
                        if (category.dividerAfter) Box(Modifier.fillMaxWidth().height(10.dp).background(SettingsBackground))
                    }
                }
                Spacer(Modifier.width(24.dp))
                Column(Modifier.fillMaxHeight().weight(0.57f).verticalScroll(rememberScrollState())) {
                    SettingsContent(context, selectedCategory, settings, selectedOption, repository, scope, { selectedOption = it; rightFocus = true; activateOption() }, { scope.launch { repository.setBrightness(it) } }, { scope.launch { repository.setTheme(it) } })
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("B", color = SettingsWhite, fontSize = 18.sp); Spacer(Modifier.width(7.dp)); Text(stringResource(R.string.settings_action_back), color = SettingsWhite, fontSize = 16.sp); Spacer(Modifier.width(25.dp)); Text("A", color = SettingsWhite, fontSize = 18.sp); Spacer(Modifier.width(7.dp)); Text(stringResource(R.string.settings_action_select), color = SettingsWhite, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SettingsContent(context: Context, category: Int, settings: LauncherSettings, selectedOption: Int, repository: LauncherSettingsRepository, scope: kotlinx.coroutines.CoroutineScope, onOptionSelected: (Int) -> Unit, onBrightnessChange: (Float) -> Unit, onThemeSelected: (String) -> Unit) {
    when (category) {
        0 -> AirplaneModeScreen(context, settings, selectedOption, { onOptionSelected(0) }, { onOptionSelected(1) }, { onOptionSelected(2) })
        1 -> BrightnessScreen(settings, selectedOption, { onOptionSelected(0) }, onBrightnessChange)
        2 -> ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothScreen(selectedOption, onOptionSelected) {
            val controller = ru.nekostul.horizonos.ui.settings.bluetooth.BluetoothSettingsController(context)
            controller.setEnabled(controller.enabled() != true)
        }
        3 -> LockScreenScreen(context, settings, selectedOption, { onOptionSelected(0) }, { timeout -> scope.launch { repository.setLockScreenTimeoutMinutes(timeout) } })
        4 -> ru.nekostul.horizonos.ui.settings.wifi.WifiScreen(selectedOption, onOptionSelected) {
            val controller = ru.nekostul.horizonos.ui.settings.wifi.WifiSettingsController(context)
            controller.setEnabled(controller.enabled() != true)
        }
        5 -> StorageScreen(context, selectedOption, onOptionSelected)
        6 -> ThemesScreen(settings, selectedOption, onThemeSelected)
        7 -> NotificationsScreen(settings, selectedOption) { onOptionSelected(0) }
        8 -> SleepScreen(context, settings, selectedOption, { onOptionSelected(0) }, { timeout -> scope.launch { repository.setSleepTimeoutMinutes(timeout) } })
        9 -> ControllersScreen(settings, selectedOption, { scope.launch { repository.setVibrationEnabled(!settings.vibrationEnabled) } }, { value -> scope.launch { repository.setControllerSensitivity(value) } }, { value -> scope.launch { repository.setControllerDeadZone(value) } })
        else -> SystemScreen(settings.language, selectedOption, onOptionSelected, { language -> scope.launch { repository.setLanguage(language) } })
    }
}

@Composable
private fun SettingsCategoryRow(text: String, selected: Boolean, focused: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).background(if (selected) SettingsSelected else Color.Transparent).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(if (selected) 4.dp else 0.dp).height(38.dp).background(if (selected) SettingsBlue else Color.Transparent))
        Spacer(Modifier.width(if (selected) 13.dp else 17.dp))
        Text(text, color = if (focused) SettingsBlue else SettingsWhite, fontSize = 18.sp)
    }
}
