package ru.nekostul.horizonos.ui.settings

data class LauncherSettings(
    val animations: Boolean = true,
    val interfaceScale: Float = 1.0f,

    val showClock: Boolean = true,
    val showBattery: Boolean = true,

    val confirmGameLaunch: Boolean = true,
    val rememberLastGame: Boolean = true,

    val interfaceSounds: Boolean = true,

    val theme: String = "dark",
    val language: String = "system",
    val accentColor: String = "cyan",
    val airplaneMode: Boolean = false,
    val airplaneWifiAllowed: Boolean = false,
    val airplaneBluetoothAllowed: Boolean = false,
    val autoBrightness: Boolean = false,
    val brightness: Float = 0.7f,
    val lockScreenEnabled: Boolean = true,
    val lockScreenTimeoutMinutes: Int = 5,
    val wifiEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val sleepTimeoutMinutes: Int = 10,
    val vibrationEnabled: Boolean = true,
    val controllerSensitivity: Float = 1.0f,
    val controllerDeadZone: Float = 0.15f
)
