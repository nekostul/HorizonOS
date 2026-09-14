package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import org.json.JSONObject
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.Platform
import ru.nekostul.horizonos.ui.settings.launcher.scanning.HttpClient
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaType
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperResult
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId
import ru.nekostul.horizonos.ui.settings.launcher.scanning.TitleMatcher
import ru.nekostul.horizonos.ui.settings.launcher.scanning.searchName

/**
 * ScreenScraper.fr — requires a developer account (devid/devpassword).
 * Search: GET /api2/jeuRecherche.php. Media types: box-2D (cover), ss (screenshot).
 */
class ScreenScraperSource : GameMetadataSource {

    override val id = ScraperSourceId.SCREEN_SCRAPER

    override fun isAvailable(settings: ScraperSettings): Boolean =
        settings.screenScraperDevId.isNotBlank() &&
            settings.screenScraperDevPassword.isNotBlank() &&
            settings.screenScraperSoftName.isNotBlank()

    private val platformSystemeId: Map<Platform, List<Int>> = mapOf(
        Platform.PLAYSTATION_1 to listOf(57),
        Platform.PSP to listOf(61),
        Platform.PLAYSTATION_2 to listOf(58),
        Platform.GAMECUBE_WII to listOf(13, 16),
        Platform.NINTENDO_SWITCH to listOf(225)
    )

    override suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult {
        val best = bestMatch(game, settings) ?: return ScraperResult()
        return ScraperResult(
            fullTitle = titleOf(best),
            coverUrl = mediaUrls(best, COVER_TYPES).firstOrNull()?.url,
            screenshotUrl = mediaUrls(best, SCREENSHOT_TYPES).firstOrNull()?.url
        )
    }

    override suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> {
        val best = bestMatch(game, settings) ?: return emptyList()
        return mediaUrls(best, COVER_TYPES, MediaType.COVER) +
            mediaUrls(best, SCREENSHOT_TYPES, MediaType.SCREENSHOT)
    }

    private suspend fun bestMatch(game: Game, settings: ScraperSettings): JSONObject? {
        if (!isAvailable(settings)) return null
        val systemeIds = platformSystemeId[game.platform] ?: return null
        // Combined platforms (GameCube/Wii) try every system until a hit is found.
        for (systemeId in systemeIds) {
            val query = "devid=${HttpClient.encode(settings.screenScraperDevId)}" +
                "&devpassword=${HttpClient.encode(settings.screenScraperDevPassword)}" +
                "&softname=${HttpClient.encode(settings.screenScraperSoftName)}" +
                "&output=json" +
                "&ssid=${HttpClient.encode(settings.screenScraperSsid)}" +
                "&sspassword=${HttpClient.encode(settings.screenScraperSspassword)}" +
                "&systemeid=$systemeId" +
                "&recherche=${HttpClient.encode(game.searchName())}"

            val raw = HttpClient.getText("https://api.screenscraper.fr/api2/jeuRecherche.php?$query")
                ?: continue
            val games = parseGames(raw)
            if (games.isEmpty()) continue
            val matchIndex = TitleMatcher.best(
                query = game.searchName(),
                candidates = games.map { titleOf(it) ?: "" }
            ) ?: continue
            return games[matchIndex]
        }
        return null
    }

    /** jeuRecherche returns `response.jeux`; jeuInfos returns `response.jeu`. */
    private fun parseGames(raw: String): List<JSONObject> = runCatching {
        val root = JSONObject(raw)
        val response = root.optJSONObject("response") ?: root
        val array = response.optJSONArray("jeux")
        if (array != null) {
            buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { add(it) }
                }
            }
        } else {
            listOfNotNull(response.optJSONObject("jeu"))
        }
    }.getOrDefault(emptyList())

    private fun titleOf(jeu: JSONObject): String? {
        val names = jeu.optJSONArray("noms")
        if (names != null) {
            for (index in 0 until names.length()) {
                val name = names.optJSONObject(index)?.optString("text")?.takeIf { it.isNotBlank() }
                if (name != null) return name
            }
        }
        return jeu.optString("nom").takeIf { it.isNotBlank() }
    }

    /** Collects every media entry of the requested types. */
    private fun mediaUrls(
        jeu: JSONObject,
        types: List<String>,
        mediaType: MediaType = MediaType.COVER
    ): List<MediaVariant> {
        val result = mutableListOf<MediaVariant>()
        val medias = jeu.optJSONArray("medias")
        if (medias != null) {
            for (index in 0 until medias.length()) {
                val media = medias.optJSONObject(index) ?: continue
                if (media.optString("type") !in types) continue
                media.optString("url").takeIf { it.isNotBlank() }?.let { url ->
                    result += MediaVariant(url, ScraperSourceId.SCREEN_SCRAPER, mediaType)
                }
            }
        }
        if (result.isEmpty()) {
            val grouped = jeu.optJSONObject("medias") ?: return result
            for (type in types) {
                val groupType = groupKey(type) ?: continue
                val group = grouped.optJSONObject(groupType) ?: continue
                val keys = group.keys()
                while (keys.hasNext()) {
                    val media = group.optJSONObject(keys.next()) ?: continue
                    media.optString("url").takeIf { it.isNotBlank() }?.let { url ->
                        result += MediaVariant(url, ScraperSourceId.SCREEN_SCRAPER, mediaType)
                    }
                }
            }
        }
        return result
    }

    private fun groupKey(type: String): String? = when (type) {
        "box-2D" -> "media_boitiers_2d"
        "box-3D" -> "media_boitiers_3d"
        "ss" -> "media_screenshot"
        "sstitle" -> "media_ss_title"
        else -> null
    }

    private companion object {
        val COVER_TYPES = listOf("box-2D", "box-3D")
        val SCREENSHOT_TYPES = listOf("ss", "sstitle")
    }
}
