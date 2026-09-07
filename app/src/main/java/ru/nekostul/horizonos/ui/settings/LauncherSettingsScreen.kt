package ru.nekostul.horizonos.ui.settings

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.view.InputDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository

private val SettingsBackground = Color(0xFF2B2B2B)
private val SettingsPanel = Color(0xFF333333)
private val SettingsSelected = Color(0xFF3A3A3A)
private val SettingsWhite = Color(0xFFF2F2F2)
private val SettingsGray = Color(0xFFAAAAAA)
private val SettingsDarkGray = Color(0xFF444444)
private val SettingsBlue = Color(0xFF00C8FF)

private data class SettingsCategory(
    val title: String,
    val dividerAfter: Boolean = false
)

private data class SettingsOption(
    val title: String,
    val value: String = "",
    val description: String = ""
)

@Composable
fun LauncherSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val repository = remember { LauncherSettingsRepository(context) }
    val settings by repository.settings.collectAsState(
        initial = LauncherSettings()
    )
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableIntStateOf(0) }
    var rightFocus by remember { mutableStateOf(false) }
    var airplaneMode by remember { mutableStateOf(false) }
    var airplaneWifi by remember { mutableStateOf(false) }
    var airplaneBluetooth by remember { mutableStateOf(false) }
    // Эти два переключателя являются разрешениями HorizonOS внутри режима полета.
    // Реальное включение/выключение системных радиомодулей добавим отдельно через Android API.
    var autoBrightness by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val leftListState = rememberLazyListState()
    val rightListState = rememberLazyListState()

    val categories = remember {
        listOf(
            SettingsCategory("Режим полета"),
            SettingsCategory("Яркость экрана"),
            SettingsCategory("Bluetooth"),
            SettingsCategory("Экран блокировки", true),
            SettingsCategory("Wi-Fi"),
            SettingsCategory("Хранилище", true),
            SettingsCategory("Темы"),
            SettingsCategory("Уведомления"),
            SettingsCategory("Режим сна", true),
            SettingsCategory("Контроллеры"),
            SettingsCategory("Система")
        )
    }

    val brightnessState = remember {
        mutableFloatStateOf(
            context.resources.displayMetrics.run {
                val window = view.context as? android.app.Activity
                window?.window?.attributes?.screenBrightness
                    ?.takeIf { it >= 0f }
                    ?: 0.7f
            }
        )
    }

    val options = remember(
        selectedCategory,
        settings,
        brightnessState.floatValue
    ) {
        when (selectedCategory) {
            0 -> buildList {
                add(
                    SettingsOption(
                        "Режим полета",
                        if (airplaneMode) "ВКЛ." else "ВЫКЛ.",
                        "Отключает беспроводные подключения. Wi-Fi и Bluetooth можно разрешить отдельно."
                    )
                )

                if (airplaneMode) {
                    add(
                        SettingsOption(
                            "Wi-Fi",
                            if (airplaneWifi) "ВКЛ." else "ВЫКЛ.",
                            "Разрешить Wi-Fi во время режима полета."
                        )
                    )
                    add(
                        SettingsOption(
                            "Bluetooth",
                            if (airplaneBluetooth) "ВКЛ." else "ВЫКЛ.",
                            "Разрешить Bluetooth во время режима полета."
                        )
                    )
                }
            }

            1 -> listOf(
                SettingsOption(
                    "Автоматическая яркость",
                    if (autoBrightness) "ВКЛ." else "ВЫКЛ.",
                    "Автоматическая регулировка яркости экрана."
                ),
                SettingsOption(
                    "Яркость",
                    "${(brightnessState.floatValue * 100).toInt()}%",
                    "Яркость экрана HorizonOS."
                )
            )

            2 -> bluetoothOptions(context)

            3 -> listOf(
                SettingsOption(
                    "Блокировка экрана",
                    "ВЫКЛ.",
                    "Блокировка после выхода консоли из режима ожидания."
                ),
                SettingsOption(
                    "Таймер блокировки",
                    "5 минут",
                    "Через какое время экран должен блокироваться."
                )
            )

            4 -> wifiOptions(context)

            5 -> storageOptions(context)

            6 -> listOf(
                SettingsOption(
                    "Светлая тема",
                    if (settings.theme == "light") "ВЫБРАНО" else ""
                ),
                SettingsOption(
                    "Темная тема",
                    if (settings.theme == "dark") "ВЫБРАНО" else ""
                )
            )

            7 -> listOf(
                SettingsOption(
                    "Уведомления",
                    if (notificationsEnabled) "ВКЛ." else "ВЫКЛ.",
                    "Разрешить HorizonOS показывать уведомления."
                ),
                SettingsOption(
                    "Уведомления приложений",
                    "",
                    "Настройка уведомлений отдельных приложений Android."
                )
            )

            8 -> listOf(
                SettingsOption(
                    "Автоматический режим сна",
                    "ВКЛ.",
                    "Автоматически переводить консоль в режим ожидания."
                ),
                SettingsOption("Через 1 минуту"),
                SettingsOption("Через 3 минуты"),
                SettingsOption("Через 5 минут"),
                SettingsOption("Через 10 минут"),
                SettingsOption("Через 30 минут"),
                SettingsOption("Никогда")
            )

            9 -> controllerOptions()

            else -> systemOptions(context)
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedOption = 0
        rightListState.scrollToItem(0)
        leftListState.animateScrollToItem(selectedCategory)
    }

    LaunchedEffect(selectedOption, rightFocus) {
        if (rightFocus && options.isNotEmpty()) {
            rightListState.animateScrollToItem(
                selectedOption.coerceIn(0, options.lastIndex)
            )
        }
    }

    fun activateOption() {
        if (options.isEmpty()) return

        when (selectedCategory) {
            0 -> {
                when (selectedOption) {
                    0 -> {
                        airplaneMode = !airplaneMode
                    }
                    1 -> {
                        if (airplaneMode) airplaneWifi = !airplaneWifi
                    }
                    2 -> {
                        if (airplaneMode) airplaneBluetooth = !airplaneBluetooth
                    }
                }
            }

            1 -> {
                if (selectedOption == 0) {
                    autoBrightness = !autoBrightness
                } else {
                    val activity = view.context as? android.app.Activity
                    activity?.window?.attributes =
                        activity.window.attributes.apply {
                            screenBrightness = brightnessState.floatValue
                        }
                }
            }

            2 -> {
                // Bluetooth полностью отображается внутри HorizonOS.
                // Подключение/поиск устройств подключим через Bluetooth API,
                // без открытия системных настроек.
            }

            3 -> {
                // Экран блокировки — внутренний экран HorizonOS.
            }

            4 -> {
                // Wi-Fi — внутренний экран HorizonOS.
                // Сети и подключение будут работать через Android Wi-Fi API.
            }

            5 -> {
                // Хранилище — уже показывает реальные данные памяти.
            }

            6 -> {
                val nextTheme =
                    if (selectedOption == 0) "light" else "dark"

                scope.launch {
                    repository.setTheme(nextTheme)
                }
            }

            7 -> {
                if (selectedOption == 0) {
                    notificationsEnabled = !notificationsEnabled
                }
            }

            8 -> {
                // Режим сна настраивается внутри HorizonOS.
            }

            9 -> {
                // Контроллеры и их параметры — внутри HorizonOS.
            }

            10 -> {
                // Системные параметры — собственные экраны HorizonOS.
                // Никаких переходов в системное приложение Настройки.
            }
        }
    }

    fun moveCategory(delta: Int) {
        selectedCategory =
            (selectedCategory + delta).coerceIn(0, categories.lastIndex)
    }

    fun moveOption(delta: Int) {
        selectedOption =
            (selectedOption + delta).coerceIn(
                0,
                options.lastIndex.coerceAtLeast(0)
            )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBackground)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                when (event.key) {
                    Key.DirectionUp -> {
                        if (rightFocus) moveOption(-1)
                        else moveCategory(-1)
                        true
                    }

                    Key.DirectionDown -> {
                        if (rightFocus) moveOption(1)
                        else moveCategory(1)
                        true
                    }

                    Key.DirectionRight -> {
                        rightFocus = true
                        true
                    }

                    Key.DirectionLeft -> {
                        rightFocus = false
                        true
                    }

                    Key.Enter,
                    Key.NumPadEnter -> {
                        if (rightFocus) activateOption()
                        else rightFocus = true
                        true
                    }

                    Key.Escape,
                    Key.Back -> {
                        onBack()
                        true
                    }

                    else -> false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 30.dp,
                    vertical = 18.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙",
                    color = SettingsWhite,
                    fontSize = 30.sp
                )

                Spacer(Modifier.width(15.dp))

                Text(
                    text = "Настройки HorizonOS",
                    color = SettingsWhite,
                    fontSize = 25.sp
                )
            }

            Spacer(Modifier.height(11.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SettingsGray)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    state = leftListState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.43f)
                        .background(SettingsPanel),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    itemsIndexed(categories) { index, category ->
                        SettingsCategoryRow(
                            text = category.title,
                            selected = selectedCategory == index,
                            focused = selectedCategory == index && !rightFocus,
                            onClick = {
                                selectedCategory = index
                                selectedOption = 0
                                rightFocus = false
                            }
                        )

                        if (category.dividerAfter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(SettingsBackground)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.57f)
                ) {
                    LazyColumn(
                        state = rightListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(options) { index, option ->
                            SettingsOptionRow(
                                title = option.title,
                                value = option.value,
                                description = option.description,
                                selected = rightFocus && selectedOption == index,
                                onClick = {
                                    selectedOption = index
                                    rightFocus = true
                                    activateOption()
                                },
                                slider =
                                    selectedCategory == 1 &&
                                            selectedOption == 1 &&
                                            index == 1,
                                sliderValue = brightnessState.floatValue,
                                onSliderChange = {
                                    brightnessState.floatValue = it
                                    val activity =
                                        view.context as? android.app.Activity
                                    activity?.window?.attributes =
                                        activity.window.attributes.apply {
                                            screenBrightness = it
                                        }
                                }
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SettingsGray)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "B",
                    color = SettingsWhite,
                    fontSize = 18.sp
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    text = "Назад",
                    color = SettingsWhite,
                    fontSize = 16.sp
                )

                Spacer(Modifier.width(25.dp))

                Text(
                    text = "A",
                    color = SettingsWhite,
                    fontSize = 18.sp
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    text = "Выбрать",
                    color = SettingsWhite,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun bluetoothOptions(context: Context): List<SettingsOption> {
    val adapter = BluetoothAdapter.getDefaultAdapter()

    if (adapter == null) {
        return listOf(
            SettingsOption(
                "Bluetooth",
                "НЕДОСТУПЕН",
                "Это устройство не поддерживает Bluetooth."
            )
        )
    }

    // Android 12+ требует BLUETOOTH_CONNECT для isEnabled/bondedDevices.
    // Не обращаемся к Bluetooth API без разрешения, чтобы экран настроек не падал.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return listOf(
            SettingsOption(
                "Bluetooth",
                "ТРЕБУЕТСЯ ДОСТУП",
                "HorizonOS нужен доступ к Bluetooth для отображения подключенных устройств."
            ),
            SettingsOption(
                "Подключенные устройства",
                "",
                "После выдачи разрешения здесь появятся сопряженные устройства."
            )
        )
    }

    val result = mutableListOf<SettingsOption>()

    result += SettingsOption(
        "Bluetooth",
        if (adapter.isEnabled) "ВКЛ." else "ВЫКЛ.",
        "Беспроводные контроллеры, наушники и другие устройства."
    )

    result += SettingsOption(
        "Подключить устройство",
        "",
        "Поиск и подключение устройств будет работать внутри HorizonOS."
    )

    @Suppress("DEPRECATION")
    adapter.bondedDevices
        .sortedBy { it.name ?: "" }
        .forEach { device ->
            result += SettingsOption(
                device.name ?: "Неизвестное устройство",
                "СОПРЯЖЕНО",
                device.address
            )
        }

    return result
}

private fun wifiOptions(context: Context): List<SettingsOption> {
    return listOf(
        SettingsOption(
            "Wi-Fi",
            "НАСТРОЙКИ",
            "Подключение к Интернету и локальным сетям."
        ),
        SettingsOption(
            "Доступные сети",
            "",
            "Список сетей доступен в системном меню Wi-Fi."
        )
    )
}

private fun storageOptions(context: Context): List<SettingsOption> {
    val stat = StatFs(context.filesDir.absolutePath)
    val total = stat.totalBytes
    val free = stat.availableBytes
    val used = total - free

    fun gb(value: Long): String =
        String.format(
            java.util.Locale.US,
            "%.1f ГБ",
            value / 1024.0 / 1024.0 / 1024.0
        )

    return listOf(
        SettingsOption(
            "Внутренняя память",
            "${gb(used)} / ${gb(total)}",
            "Используется хранилищем устройства."
        ),
        SettingsOption(
            "Свободно",
            gb(free),
            "Свободное место для игр, приложений и данных."
        ),
        SettingsOption(
            "Данные HorizonOS",
            "",
            "Настройки, кэш и данные лаунчера."
        )
    )
}

private fun controllerOptions(): List<SettingsOption> {
    val ids = InputDevice.getDeviceIds()
    val controllers = ids.toList().mapNotNull { id ->
        val device = InputDevice.getDevice(id)
        if (
            device != null &&
            (device.sources and InputDevice.SOURCE_GAMEPAD != 0 ||
                    device.sources and InputDevice.SOURCE_JOYSTICK != 0)
        ) {
            device.name
        } else {
            null
        }
    }

    val result = mutableListOf<SettingsOption>()

    result += SettingsOption(
        "Подключенные контроллеры",
        if (controllers.isEmpty()) "НЕТ" else controllers.size.toString(),
        "Контроллеры, которые сейчас видит Android."
    )

    controllers.forEach {
        result += SettingsOption(
            it,
            "ПОДКЛЮЧЕН",
            "Игровой контроллер."
        )
    }

    result += SettingsOption(
        "Настройка кнопок",
        "",
        "Переназначение кнопок контроллера."
    )

    result += SettingsOption(
        "Стики и чувствительность",
        "",
        "Чувствительность и мёртвые зоны."
    )

    result += SettingsOption(
        "Вибрация",
        "ВКЛ.",
        "Вибрация совместимых контроллеров."
    )

    return result
}

private fun systemOptions(context: Context): List<SettingsOption> {
    return listOf(
        SettingsOption(
            "Дата и время",
            "",
            "Дата, время и часовой пояс Android."
        ),
        SettingsOption(
            "Язык",
            "",
            "Язык системы Android."
        ),
        SettingsOption(
            "Экран",
            "",
            "Дисплей, частота обновления и другие параметры экрана."
        ),
        SettingsOption(
            "Звук",
            "",
            "Громкость, звуки и вибрация."
        ),
        SettingsOption(
            "Специальные возможности",
            "",
            "Системные функции доступности Android."
        ),
        SettingsOption(
            "Приложения",
            "",
            "Установленные приложения и разрешения."
        ),
        SettingsOption(
            "Уведомления Android",
            "",
            "Системные настройки уведомлений."
        ),
        SettingsOption(
            "Батарея",
            "",
            "Энергопотребление и режимы экономии."
        ),
        SettingsOption(
            "Все настройки Android",
            "",
            "Открыть полный системный экран настроек Android."
        )
    )
}

@Composable
private fun SettingsCategoryRow(
    text: String,
    selected: Boolean,
    focused: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                if (selected) SettingsSelected
                else Color.Transparent
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(if (selected) 4.dp else 0.dp)
                .height(38.dp)
                .background(
                    if (selected) SettingsBlue
                    else Color.Transparent
                )
        )

        Spacer(
            Modifier.width(
                if (selected) 13.dp else 17.dp
            )
        )

        Text(
            text = text,
            color =
                if (focused) SettingsBlue
                else SettingsWhite,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    value: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    slider: Boolean = false,
    sliderValue: Float = 0f,
    onSliderChange: (Float) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) SettingsSelected
                else Color.Transparent
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color =
                    if (selected) SettingsBlue
                    else Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            )
            .clickable { onClick() }
            .padding(
                horizontal = 13.dp,
                vertical = 10.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = SettingsWhite,
                fontSize = 18.sp
            )

            Spacer(Modifier.weight(1f))

            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    color =
                        if (selected) SettingsBlue
                        else SettingsGray,
                    fontSize = 17.sp
                )
            }
        }

        if (description.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = description,
                color = SettingsGray,
                fontSize = 14.sp
            )
        }

        if (slider) {
            Spacer(Modifier.height(5.dp))

            Slider(
                value = sliderValue,
                onValueChange = onSliderChange,
                valueRange = 0f..1f
            )
        }
    }
}
