package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.content.Context

/**
 * Persists scraper source toggles and API secrets. Secrets are encrypted
 * via the Android Keystore (SecretCipher) before being stored.
 */
class ScraperRepository(private val context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("scraper_settings", Context.MODE_PRIVATE)

    fun load(): ScraperSettings = ScraperSettings(
        enabled = ScraperSourceId.entries.associateWith { id ->
            preferences.getBoolean("enabled_${id.storageKey}", true)
        },
        screenScraperDevId = preferences.getString("screen_scraper_devid", null).orEmpty(),
        screenScraperDevPassword = SecretCipher.decrypt(preferences.getString("screen_scraper_devpassword", "").orEmpty()),
        screenScraperSoftName = preferences.getString("screen_scraper_softname", "HorizonOS").orEmpty(),
        screenScraperSsid = preferences.getString("screen_scraper_ssid", null).orEmpty(),
        screenScraperSspassword = SecretCipher.decrypt(preferences.getString("screen_scraper_sspassword", "").orEmpty()),
        theGamesDbApiKey = SecretCipher.decrypt(preferences.getString("the_games_db_apikey", "").orEmpty()),
        igdbClientId = preferences.getString("igdb_client_id", null).orEmpty(),
        igdbClientSecret = SecretCipher.decrypt(preferences.getString("igdb_client_secret", "").orEmpty()),
        steamGridDbApiKey = SecretCipher.decrypt(preferences.getString("steam_grid_db_apikey", "").orEmpty())
    )

    fun setEnabled(id: ScraperSourceId, enabled: Boolean) {
        preferences.edit().putBoolean("enabled_${id.storageKey}", enabled).apply()
    }

    fun setScreenScraperCredentials(devId: String, devPassword: String, softName: String) {
        preferences.edit()
            .putString("screen_scraper_devid", devId)
            .putString("screen_scraper_devpassword", SecretCipher.encrypt(devPassword))
            .putString("screen_scraper_softname", softName)
            .apply()
    }

    fun setScreenScraperSession(ssid: String, sspassword: String) {
        preferences.edit()
            .putString("screen_scraper_ssid", ssid)
            .putString("screen_scraper_sspassword", SecretCipher.encrypt(sspassword))
            .apply()
    }

    fun setTheGamesDbApiKey(value: String) {
        preferences.edit()
            .putString("the_games_db_apikey", SecretCipher.encrypt(value))
            .apply()
    }

    fun setIgdbCredentials(clientId: String, clientSecret: String) {
        preferences.edit()
            .putString("igdb_client_id", clientId)
            .putString("igdb_client_secret", SecretCipher.encrypt(clientSecret))
            .apply()
    }

    fun setSteamGridDbApiKey(value: String) {
        preferences.edit()
            .putString("steam_grid_db_apikey", SecretCipher.encrypt(value))
            .apply()
    }
}
