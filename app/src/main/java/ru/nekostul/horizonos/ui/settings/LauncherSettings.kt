package ru.nekostul.horizonos.ui.settings

data class LauncherSettings(
    val firstSetupCompleted: Boolean = false,
    val animations: Boolean = true,
    val interfaceScale: Float = 1.0f,

    val showClock: Boolean = true,
    val showBattery: Boolean = true,

    val confirmGameLaunch: Boolean = true,
    val rememberLastGame: Boolean = true,

    val interfaceSounds: Boolean = true,
    val soundMode: String = LauncherSoundMode.ALL,
    val backgroundMusicEnabled: Boolean = true,
    val backgroundMusicVolume: Float = 0.65f,
    val hapticFeedbackEnabled: Boolean = true,

    val theme: String = "dark",
    val language: String = "system",
    val accentColor: String = "cyan",
    val airplaneMode: Boolean = false,
    val airplaneWifiAllowed: Boolean = false,
    val airplaneBluetoothAllowed: Boolean = false,
    val autoBrightness: Boolean = false,
    val brightness: Float = 0.7f,
    val lockScreenEnabled: Boolean = false,
    val lockScreenTimeoutMinutes: Int = 5,
    val wifiEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val sleepEnabled: Boolean = true,
    val sleepTimeoutMinutes: Int = 10,
    val sleepMediaEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val controllerSensitivity: Float = 1.0f,
    val controllerDeadZone: Float = 0.15f,
    val screenshotBackgroundEnabled: Boolean = true,
    val rootAccessGranted: Boolean = false
)

object LauncherSoundMode {
    const val OFF = "off"
    const val ALL = "all"
    const val GAMEPAD_ONLY = "gamepad_only"

    fun next(value: String): String = when (value) {
        OFF -> ALL
        ALL -> GAMEPAD_ONLY
        else -> OFF
    }
}
