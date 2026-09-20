package ru.nekostul.horizonos.ui.settings.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonOverlayFooterAction
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsBlue
import ru.nekostul.horizonos.ui.settings.SettingsCapabilitiesNote
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsToggleRow
import ru.nekostul.horizonos.ui.settings.SettingsTrack
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private const val BLUETOOTH_REFRESH_MILLIS = 1_000L
private const val SCAN_TIMEOUT_MILLIS = 15_000L

@Composable
private fun BluetoothDeviceCategory.label(): String = stringResource(
    when (this) {
        BluetoothDeviceCategory.AUDIO -> R.string.settings_bluetooth_category_audio
        BluetoothDeviceCategory.TV -> R.string.settings_bluetooth_category_tv
        BluetoothDeviceCategory.COMPUTER -> R.string.settings_bluetooth_category_computer
        BluetoothDeviceCategory.PHONE -> R.string.settings_bluetooth_category_phone
        BluetoothDeviceCategory.PERIPHERAL -> R.string.settings_bluetooth_category_peripheral
        BluetoothDeviceCategory.OTHER -> R.string.settings_bluetooth_category_other
    }
)

@Composable
fun BluetoothScreen(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    openRequest: Int = 0,
    onItemCountChange: (Int) -> Unit = {},
    rootAccessGranted: Boolean = false
) {
    val context = LocalContext.current
    val controller = remember { BluetoothSettingsController(context) }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf<Boolean?>(null) }
    var bonded by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var nameCacheRevision by remember { mutableIntStateOf(0) }
    var bondedRevision by remember { mutableIntStateOf(0) }
    val discovered = remember { mutableStateListOf<BluetoothDevice>() }
    val discoveredNames = remember { mutableStateMapOf<String, String>() }
    var scanning by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerIndex by remember { mutableIntStateOf(0) }
    var busyAddress by remember { mutableStateOf<String?>(null) }
    var scanRequest by remember { mutableIntStateOf(0) }
    var permissionRevision by remember { mutableIntStateOf(0) }
    var leScanning by remember { mutableStateOf(false) }
    val canScan = controller.canScan
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionRevision++ }
    val leScanCallback = remember(controller) {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                result.scanRecord?.deviceName
                    ?.takeIf(String::isNotBlank)
                    ?.let { discoveredNames[device.address] = it }
                result.scanRecord?.deviceName
                    ?.takeIf(String::isNotBlank)
                    ?.let {
                        controller.cacheName(device.address, it)
                        nameCacheRevision = controller.getNameCacheRevision()
                    }
                if (discovered.none { it.address == device.address }) {
                    discovered += device
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    val device = result.device
                    result.scanRecord?.deviceName
                        ?.takeIf(String::isNotBlank)
                        ?.let { discoveredNames[device.address] = it }
                    result.scanRecord?.deviceName
                        ?.takeIf(String::isNotBlank)
                        ?.let {
                            controller.cacheName(device.address, it)
                            nameCacheRevision = controller.getNameCacheRevision()
                        }
                    if (discovered.none { it.address == device.address }) {
                        discovered += device
                    }
                }
            }
        }
    }

    suspend fun refresh() {
        val currentEnabled = withContext(Dispatchers.IO) { controller.enabled() }
        val currentBonded = withContext(Dispatchers.IO) { controller.bondedDevices() }
        enabled = currentEnabled
        bonded = currentBonded
        bondedRevision++
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(BLUETOOTH_REFRESH_MILLIS)
        }
    }

    // Resolve names for bonded devices that don't have one yet.
    // Runs discovery briefly in the background to populate the name cache.
    var nameResolveAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(bonded.size, enabled, nameResolveAttempt) {
        if (bonded.isEmpty() || enabled != true) return@LaunchedEffect
        val needsResolve = bonded.any { controller.nameOf(it) == null }
        if (!needsResolve) return@LaunchedEffect
        if (nameResolveAttempt >= 3) return@LaunchedEffect
        withContext(Dispatchers.IO) { controller.resolveNames() }
        nameResolveAttempt++
        nameCacheRevision = controller.getNameCacheRevision()
        refresh()
    }

    DisposableEffect(controller) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val advertisedName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                            ?.takeIf(String::isNotBlank)
                        if (device != null && advertisedName != null) {
                            discoveredNames[device.address] = advertisedName
                            controller.cacheName(device.address, advertisedName)
                            nameCacheRevision = controller.getNameCacheRevision()
                        }
                        if (device != null && discovered.none { it.address == device.address }) {
                            discovered += device
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> scanning = true
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> scanning = leScanning
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                    BluetoothDevice.ACTION_ACL_CONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> scope.launch { refresh() }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
            }
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
            controller.stopLeScan(leScanCallback)
        }
    }

    fun openPicker() {
        pickerOpen = true
        scanRequest++
    }

    fun retryScan() {
        scanRequest++
    }

    LaunchedEffect(openRequest) {
        if (openRequest > 0) openPicker()
    }

    // Starts a discovery run whenever the picker opens or "search again" is
    // pressed. A timeout guarantees the retry action reappears even if the
    // system never delivers ACTION_DISCOVERY_FINISHED.
    LaunchedEffect(scanRequest, pickerOpen, permissionRevision) {
        if (!pickerOpen || scanRequest == 0) return@LaunchedEffect
        if (!controller.canScan) {
            scanning = false
            permissionLauncher.launch(
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            )
            return@LaunchedEffect
        }
        discovered.clear()
        discoveredNames.clear()
        pickerIndex = 0
        scanning = true
        val leStarted = withContext(Dispatchers.IO) {
            controller.startLeScan(leScanCallback)
        }
        leScanning = leStarted
        val classicStarted = withContext(Dispatchers.IO) { controller.startDiscovery() }
        scanning = leStarted || classicStarted || withContext(Dispatchers.IO) { controller.isDiscovering() }
        if (scanning) {
            try {
                delay(SCAN_TIMEOUT_MILLIS)
            } finally {
                withContext(Dispatchers.IO) { controller.cancelDiscovery() }
                controller.stopLeScan(leScanCallback)
                leScanning = false
                scanning = false
            }
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        busyAddress = device.address
        scope.launch {
            withContext(Dispatchers.IO) {
                if (controller.isBonded(device)) controller.connect(device) else controller.createBond(device)
            }
            delay(1_200)
            refresh()
            busyAddress = null
        }
    }

    LaunchedEffect(bonded.size) {
        onItemCountChange(bonded.size + 2)
    }

    Column {
        Text(stringResource(R.string.settings_bluetooth_title), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        SettingsToggleRow(
            title = stringResource(R.string.settings_bluetooth_title),
            checked = enabled == true,
            selected = selectedIndex == 0,
            enabled = controller.canControl,
            onClick = { onSelect(0) }
        )
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.settings_bluetooth_add_device),
                value = if (scanning) stringResource(R.string.settings_bluetooth_scanning) else "",
                description = stringResource(R.string.settings_bluetooth_add_description)
            ),
            selected = selectedIndex == 1,
            onClick = { onSelect(1) }
        )
        key(nameCacheRevision, bondedRevision) {
            bonded.forEachIndexed { index, device ->
                val name = controller.nameOf(device, discoveredNames[device.address])
                    ?: stringResource(R.string.settings_bluetooth_device_address, device.address)
                val status = when {
                    busyAddress == device.address -> stringResource(R.string.settings_bluetooth_connecting)
                    controller.isConnected(device) -> stringResource(R.string.settings_bluetooth_connected)
                    else -> stringResource(R.string.settings_status_paired)
                }
                HorizonSettingRow(
                    SettingRow(name, status, controller.categoryOf(device).label()),
                    selectedIndex == index + 2,
                    onClick = { onSelect(index + 2) }
                )
            }
        }
        if (!controller.available) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_missing))
        else if (!rootAccessGranted && !controller.canControl) SettingsCapabilitiesNote(stringResource(R.string.settings_bluetooth_permission))
    }

    if (pickerOpen) {
        val combined = (bonded + discovered).distinctBy { it.address }
        val grouped = combined.groupBy { controller.categoryOf(it) }
        val orderedCategories = BluetoothDeviceCategory.entries.filter { grouped[it].orEmpty().isNotEmpty() }
        val deviceRows = orderedCategories.flatMap { grouped[it].orEmpty() }
        if (pickerIndex > deviceRows.lastIndex) pickerIndex = deviceRows.lastIndex.coerceAtLeast(0)
        var deviceCursor = 0
        HorizonOverlay(
            title = stringResource(R.string.settings_bluetooth_add_device),
            onDismiss = {
                pickerOpen = false
                runCatching { controller.cancelDiscovery() }
            },
            centered = scanning,
            footerAction = if (!scanning) {
                {
                    HorizonOverlayFooterAction(
                        label = stringResource(R.string.settings_bluetooth_rescan),
                        onClick = { retryScan() }
                    )
                }
            } else {
                null
            },
            onDirectionalKey = { key ->
                if (deviceRows.isEmpty()) {
                    false
                } else {
                    when (key) {
                        Key.DirectionDown, Key.DirectionRight -> {
                            pickerIndex = (pickerIndex + 1).coerceAtMost(deviceRows.lastIndex)
                            true
                        }
                        Key.DirectionUp, Key.DirectionLeft -> {
                            pickerIndex = (pickerIndex - 1).coerceAtLeast(0)
                            true
                        }
                        else -> false
                    }
                }
            }
        ) {
            if (scanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BluetoothScanIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.settings_bluetooth_searching),
                        color = SettingsGray,
                        fontSize = 16.sp
                    )
                }
            } else if (!canScan) {
                Text(
                    stringResource(R.string.settings_bluetooth_scan_permission),
                    color = SettingsGray,
                    fontSize = 15.sp
                )
            } else if (enabled != true) {
                Text(
                    stringResource(R.string.settings_bluetooth_disabled),
                    color = SettingsGray,
                    fontSize = 15.sp
                )
            } else {
                if (combined.isEmpty()) {
                    Text(stringResource(R.string.settings_bluetooth_no_devices), color = SettingsGray, fontSize = 15.sp)
                } else {
                    Text(stringResource(R.string.settings_bluetooth_tap_to_connect), color = SettingsGray, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                }
                orderedCategories.forEach { category ->
                    Text(
                        category.label(),
                        color = SettingsWhite,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                    grouped[category].orEmpty().forEach { device ->
                        val index = deviceCursor
                        deviceCursor++
                        val name = controller.nameOf(device, discoveredNames[device.address])
                            ?: stringResource(R.string.settings_bluetooth_device_address, device.address)
                        val status = when {
                            busyAddress == device.address -> stringResource(R.string.settings_bluetooth_connecting)
                            controller.isConnected(device) -> stringResource(R.string.settings_bluetooth_connected)
                            controller.isBonded(device) -> stringResource(R.string.settings_status_paired)
                            else -> stringResource(R.string.settings_bluetooth_pair)
                        }
                        HorizonOverlayChoice(
                            title = name,
                            selected = pickerIndex == index,
                            value = status,
                            onClick = { connectDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothScanIndicator() {
    val trackColor = SettingsTrack
    val accentColor = SettingsBlue
    val transition = rememberInfiniteTransition(label = "bluetoothScan")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bluetoothScanAngle"
    )
    Canvas(Modifier.size(58.dp)) {
        val stroke = 5.dp.toPx()
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = accentColor,
            startAngle = angle,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
