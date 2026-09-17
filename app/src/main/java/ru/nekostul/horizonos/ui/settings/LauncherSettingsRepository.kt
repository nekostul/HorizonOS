package ru.nekostul.horizonos.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherSettingsDataStore by preferencesDataStore(
    name = "launcher_settings"
)

class LauncherSettingsRepository(
    private val context: Context
) {

    private object Keys {

        val firstSetupCompleted =
            booleanPreferencesKey("first_setup_completed")

        val animations =
            booleanPreferencesKey("animations")

        val interfaceScale =
            floatPreferencesKey("interface_scale")

        val showClock =
            booleanPreferencesKey("show_clock")

        val showBattery =
            booleanPreferencesKey("show_battery")

        val confirmGameLaunch =
            booleanPreferencesKey("confirm_game_launch")

        val rememberLastGame =
            booleanPreferencesKey("remember_last_game")

        val interfaceSounds =
            booleanPreferencesKey("interface_sounds")

        val soundMode =
            stringPreferencesKey("sound_mode")

        val backgroundMusicEnabled =
            booleanPreferencesKey("background_music_enabled")

        val backgroundMusicVolume =
            floatPreferencesKey("background_music_volume")

        val hapticFeedbackEnabled =
            booleanPreferencesKey("haptic_feedback_enabled")

        val theme =
            stringPreferencesKey("theme")

        val language =
            stringPreferencesKey("language")

        val accentColor =
            stringPreferencesKey("accent_color")

        val airplaneMode = booleanPreferencesKey("airplane_mode")
        val airplaneWifiAllowed = booleanPreferencesKey("airplane_wifi_allowed")
        val airplaneBluetoothAllowed = booleanPreferencesKey("airplane_bluetooth_allowed")
        val autoBrightness = booleanPreferencesKey("auto_brightness")
        val brightness = floatPreferencesKey("brightness")
        val lockScreenEnabled = booleanPreferencesKey("lock_screen_enabled")
        val lockScreenTimeoutMinutes = intPreferencesKey("lock_screen_timeout_minutes")
        val wifiEnabled = booleanPreferencesKey("wifi_enabled")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val sleepEnabled = booleanPreferencesKey("sleep_enabled")
        val sleepTimeoutMinutes = intPreferencesKey("sleep_timeout_minutes")
        val sleepMediaEnabled = booleanPreferencesKey("sleep_media_enabled")
        val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val controllerSensitivity = floatPreferencesKey("controller_sensitivity")
        val controllerDeadZone = floatPreferencesKey("controller_dead_zone")
        val screenshotBackgroundEnabled = booleanPreferencesKey("screenshot_background_enabled")
    }

    val settings: Flow<LauncherSettings> =
        context.launcherSettingsDataStore.data.map { preferences ->

            LauncherSettings(
                firstSetupCompleted =
                    preferences[Keys.firstSetupCompleted] ?: false,

                animations =
                    preferences[Keys.animations] ?: true,

                interfaceScale =
                    preferences[Keys.interfaceScale] ?: 1.0f,

                showClock =
                    preferences[Keys.showClock] ?: true,

                showBattery =
                    preferences[Keys.showBattery] ?: true,

                confirmGameLaunch =
                    preferences[Keys.confirmGameLaunch] ?: true,

                rememberLastGame =
                    preferences[Keys.rememberLastGame] ?: true,

                interfaceSounds =
                    preferences[Keys.interfaceSounds] ?: true,

                soundMode = preferences[Keys.soundMode]
                    ?: if (preferences[Keys.interfaceSounds] == false) {
                        LauncherSoundMode.OFF
                    } else {
                        LauncherSoundMode.ALL
                    },

                backgroundMusicEnabled = preferences[Keys.backgroundMusicEnabled] ?: true,

                backgroundMusicVolume =
                    (preferences[Keys.backgroundMusicVolume] ?: 0.65f).coerceIn(0f, 1f),

                hapticFeedbackEnabled = preferences[Keys.hapticFeedbackEnabled] ?: true,

                theme =
                    preferences[Keys.theme] ?: "dark",

                language =
                    preferences[Keys.language] ?: "system",

                accentColor =
                    preferences[Keys.accentColor] ?: "cyan"
                ,airplaneMode = preferences[Keys.airplaneMode] ?: false
                ,airplaneWifiAllowed = preferences[Keys.airplaneWifiAllowed] ?: false
                ,airplaneBluetoothAllowed = preferences[Keys.airplaneBluetoothAllowed] ?: false
                ,autoBrightness = preferences[Keys.autoBrightness] ?: false
                ,brightness = preferences[Keys.brightness] ?: 0.7f
                ,lockScreenEnabled = preferences[Keys.lockScreenEnabled] ?: true
                ,lockScreenTimeoutMinutes = preferences[Keys.lockScreenTimeoutMinutes] ?: 5
                ,wifiEnabled = preferences[Keys.wifiEnabled] ?: true
                ,notificationsEnabled = preferences[Keys.notificationsEnabled] ?: true
                ,sleepEnabled = preferences[Keys.sleepEnabled] ?: true
                ,sleepTimeoutMinutes = preferences[Keys.sleepTimeoutMinutes] ?: 10
                ,sleepMediaEnabled = preferences[Keys.sleepMediaEnabled] ?: true
                ,vibrationEnabled = preferences[Keys.vibrationEnabled] ?: true
                ,controllerSensitivity = preferences[Keys.controllerSensitivity] ?: 1.0f
                ,controllerDeadZone = preferences[Keys.controllerDeadZone] ?: 0.15f
                ,screenshotBackgroundEnabled = preferences[Keys.screenshotBackgroundEnabled] ?: true
            )
        }

    suspend fun setAnimations(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.animations] = value
        }
    }

    suspend fun setInterfaceScale(value: Float) {
        context.launcherSettingsDataStore.edit {
            it[Keys.interfaceScale] = value
        }
    }

    suspend fun setShowClock(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.showClock] = value
        }
    }

    suspend fun setShowBattery(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.showBattery] = value
        }
    }

    suspend fun setConfirmGameLaunch(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.confirmGameLaunch] = value
        }
    }

    suspend fun setRememberLastGame(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.rememberLastGame] = value
        }
    }

    suspend fun setInterfaceSounds(value: Boolean) {
        context.launcherSettingsDataStore.edit {
            it[Keys.interfaceSounds] = value
            it[Keys.soundMode] = if (value) LauncherSoundMode.ALL else LauncherSoundMode.OFF
        }
    }

    suspend fun setFirstSetupCompleted(value: Boolean) = update {
        it[Keys.firstSetupCompleted] = value
    }

    suspend fun setSoundMode(value: String) {
        context.launcherSettingsDataStore.edit {
            it[Keys.soundMode] = value
            it[Keys.interfaceSounds] = value != LauncherSoundMode.OFF
        }
    }

    suspend fun setTheme(value: String) {
        context.launcherSettingsDataStore.edit {
            it[Keys.theme] = value
        }
    }

    suspend fun setAccentColor(value: String) {
        context.launcherSettingsDataStore.edit {
            it[Keys.accentColor] = value
        }
    }

    suspend fun setLanguage(value: String) {
        context.launcherSettingsDataStore.edit {
            it[Keys.language] = value
        }
    }

    suspend fun setAirplaneMode(value: Boolean) = update { it[Keys.airplaneMode] = value }
    suspend fun setAirplaneWifiAllowed(value: Boolean) = update { it[Keys.airplaneWifiAllowed] = value }
    suspend fun setAirplaneBluetoothAllowed(value: Boolean) = update { it[Keys.airplaneBluetoothAllowed] = value }
    suspend fun setAutoBrightness(value: Boolean) = update { it[Keys.autoBrightness] = value }
    suspend fun setBrightness(value: Float) = update { it[Keys.brightness] = value.coerceIn(0f, 1f) }
    suspend fun setLockScreenEnabled(value: Boolean) = update { it[Keys.lockScreenEnabled] = value }
    suspend fun setLockScreenTimeoutMinutes(value: Int) = update { it[Keys.lockScreenTimeoutMinutes] = value }
    suspend fun setWifiEnabled(value: Boolean) = update { it[Keys.wifiEnabled] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = update { it[Keys.notificationsEnabled] = value }
    suspend fun setSleepEnabled(value: Boolean) = update { it[Keys.sleepEnabled] = value }
    suspend fun setSleepTimeoutMinutes(value: Int) = update { it[Keys.sleepTimeoutMinutes] = value }
    suspend fun setSleepMediaEnabled(value: Boolean) = update { it[Keys.sleepMediaEnabled] = value }
    suspend fun setVibrationEnabled(value: Boolean) = update { it[Keys.vibrationEnabled] = value }
    suspend fun setControllerSensitivity(value: Float) = update { it[Keys.controllerSensitivity] = value.coerceIn(0.5f, 2f) }
    suspend fun setControllerDeadZone(value: Float) = update { it[Keys.controllerDeadZone] = value.coerceIn(0f, 0.5f) }
    suspend fun setScreenshotBackgroundEnabled(value: Boolean) = update { it[Keys.screenshotBackgroundEnabled] = value }
    suspend fun setBackgroundMusicEnabled(value: Boolean) = update { it[Keys.backgroundMusicEnabled] = value }
    suspend fun setBackgroundMusicVolume(value: Float) = update {
        it[Keys.backgroundMusicVolume] = value.coerceIn(0f, 1f)
    }
    suspend fun setHapticFeedbackEnabled(value: Boolean) = update { it[Keys.hapticFeedbackEnabled] = value }

    private suspend fun update(block: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.launcherSettingsDataStore.edit(block)
    }
}
