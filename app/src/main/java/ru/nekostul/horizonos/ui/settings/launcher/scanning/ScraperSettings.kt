package ru.nekostul.horizonos.ui.settings.launcher.scanning

enum class ScraperSourceId(val storageKey: String) {
    SCREEN_SCRAPER("screen_scraper"),
    LIBRETRO("libretro"),
    THE_GAMES_DB("the_games_db"),
    IGDB("igdb"),
    STEAM_GRID_DB("steam_grid_db")
}

data class ScraperSettings(
    val enabled: Map<ScraperSourceId, Boolean> = ScraperSourceId.entries.associateWith { true },
    val screenScraperDevId: String = "",
    val screenScraperDevPassword: String = "",
    val screenScraperSoftName: String = "HorizonOS",
    val screenScraperSsid: String = "",
    val screenScraperSspassword: String = "",
    val theGamesDbApiKey: String = "",
    val igdbClientId: String = "",
    val igdbClientSecret: String = "",
    val steamGridDbApiKey: String = ""
) {
    fun isEnabled(id: ScraperSourceId): Boolean = enabled[id] ?: true
}
