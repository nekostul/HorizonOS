package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import org.json.JSONArray
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

class TheGamesDbSource : GameMetadataSource {

    override val id = ScraperSourceId.THE_GAMES_DB

    override fun isAvailable(settings: ScraperSettings): Boolean =
        settings.theGamesDbApiKey.isNotBlank()

    private val platformId: Map<Platform, List<Int>> = mapOf(
        Platform.PLAYSTATION_1 to listOf(10),
        Platform.PSP to listOf(13),
        Platform.PLAYSTATION_2 to listOf(11),
        Platform.GAMECUBE_WII to listOf(2, 9),
        Platform.NINTENDO_SWITCH to listOf(4971),
        Platform.ANDROID to listOf(4916)
    )

    override suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult {
        val search = searchBest(game, settings) ?: return ScraperResult()
        val cover = imageVariants(search.images, MediaType.COVER).firstOrNull()?.url
            ?: resolveCoverFromInclude(search.include, search.id)
        val screenshot = imageVariants(search.images, MediaType.SCREENSHOT).firstOrNull()?.url
        return ScraperResult(fullTitle = search.title, coverUrl = cover, screenshotUrl = screenshot)
    }

    override suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> {
        val search = searchBest(game, settings) ?: return emptyList()
        val result = mutableListOf<MediaVariant>()
        result += imageVariants(search.images, MediaType.COVER)
        result += imageVariants(search.images, MediaType.SCREENSHOT)
        if (result.none { it.type == MediaType.COVER }) {
            resolveCoverFromInclude(search.include, search.id)?.let {
                result += MediaVariant(it, ScraperSourceId.THE_GAMES_DB, MediaType.COVER)
            }
        }
        return result
    }

    private suspend fun searchBest(game: Game, settings: ScraperSettings): SearchHit? {
        if (!isAvailable(settings)) return null
        val platforms = platformId[game.platform] ?: return null
        for (platform in platforms) {
            val searchUrl = "https://api.thegamesdb.net/v1/Games/ByGameName?apikey=" +
                "${HttpClient.encode(settings.theGamesDbApiKey)}" +
                "&name=${HttpClient.encode(game.searchName())}" +
                "&include=boxart&fields=players,rating,overview&filter%5Bplatform%5D=$platform"
            val raw = HttpClient.getText(searchUrl) ?: continue

            val root = runCatching { JSONObject(raw) }.getOrNull() ?: continue
            val data = root.optJSONObject("data") ?: continue

            val candidates = mutableListOf<GameCandidate>()
            val gamesArray = data.optJSONArray("games")
            if (gamesArray != null) {
                for (index in 0 until gamesArray.length()) {
                    val item = gamesArray.optJSONObject(index) ?: continue
                    val title = item.optString("game_title").takeIf { it.isNotBlank() } ?: continue
                    val id = item.optInt("id", -1)
                    if (id > 0) candidates += GameCandidate(id.toString(), title)
                }
            } else {
                val gamesObject = data.optJSONObject("games")
                if (gamesObject != null) {
                    val keys = gamesObject.keys()
                    while (keys.hasNext()) {
                        val id = keys.next()
                        val item = gamesObject.optJSONObject(id) ?: continue
                        val title = item.optString("game_title").takeIf { it.isNotBlank() } ?: continue
                        candidates += GameCandidate(id, title)
                    }
                }
            }
            if (candidates.isEmpty()) continue
            val matchIndex = TitleMatcher.best(game.searchName(), candidates.map { it.title })
                ?: continue
            val selected = candidates[matchIndex]

            val include = root.optJSONObject("include")
            val images = loadImages(settings, selected.id)
            return SearchHit(selected.id, selected.title, include, images)
        }
        return null
    }

    private suspend fun loadImages(settings: ScraperSettings, gameId: String): List<MediaVariant> {
        val imagesUrl = "https://api.thegamesdb.net/v1/Games/Images?apikey=" +
            "${HttpClient.encode(settings.theGamesDbApiKey)}&games_id=$gameId"
        val raw = HttpClient.getText(imagesUrl) ?: return emptyList()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val baseUrl = baseUrlFrom(json) ?: return emptyList()

        val result = mutableListOf<MediaVariant>()
        collectImageArrays(json, gameId).forEach { array ->
            for (index in 0 until array.length()) {
                val image = array.optJSONObject(index) ?: continue
                val fileName = image.optString("filename").takeIf { it.isNotBlank() } ?: continue
                val type = when (image.optString("type")) {
                    "boxart" -> {
                        if (image.optString("side") == "back") continue
                        MediaType.COVER
                    }
                    "screenshot" -> MediaType.SCREENSHOT
                    else -> continue
                }
                result += MediaVariant(baseUrl + fileName, ScraperSourceId.THE_GAMES_DB, type)
            }
        }
        return result
    }

    private fun baseUrlFrom(json: JSONObject): String? =
        json.optJSONObject("data")?.optJSONObject("base_url")?.optString("original")?.takeIf { it.isNotBlank() }
            ?: json.optJSONObject("include")?.optJSONObject("base_url")?.optString("original")?.takeIf { it.isNotBlank() }

    private fun collectImageArrays(json: JSONObject, gameId: String): List<JSONArray> {
        val result = mutableListOf<JSONArray>()
        json.optJSONObject("data")?.optJSONArray("images")?.let { result += it }
        json.optJSONArray("images")?.let { result += it }
        json.optJSONObject("data")?.optJSONObject("images")?.optJSONArray(gameId)?.let { result += it }
        return result
    }

    private fun imageVariants(images: List<MediaVariant>, type: MediaType): List<MediaVariant> =
        images.filter { it.type == type }

    private fun resolveCoverFromInclude(include: JSONObject?, gameId: String): String? {
        val boxart = include?.optJSONObject("boxart") ?: return null
        val baseUrl = boxart.optJSONObject("base_url")?.optString("original")?.takeIf { it.isNotBlank() } ?: return null
        val entries = boxart.optJSONObject("data")?.optJSONArray(gameId) ?: return null
        var fallback: String? = null
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            if (entry.optString("type") != "boxart") continue
            val fileName = entry.optString("filename").takeIf { it.isNotBlank() } ?: continue
            val url = baseUrl + fileName
            if (entry.optString("side") == "front") return url
            if (fallback == null) fallback = url
        }
        return fallback
    }

    private data class GameCandidate(val id: String, val title: String)
    private data class SearchHit(
        val id: String,
        val title: String,
        val include: JSONObject?,
        val images: List<MediaVariant>
    )
}
