package ru.nekostul.horizonos.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

        val theme =
            stringPreferencesKey("theme")

        val accentColor =
            stringPreferencesKey("accent_color")
    }

    val settings: Flow<LauncherSettings> =
        context.launcherSettingsDataStore.data.map { preferences ->

            LauncherSettings(
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

                theme =
                    preferences[Keys.theme] ?: "dark",

                accentColor =
                    preferences[Keys.accentColor] ?: "cyan"
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
}