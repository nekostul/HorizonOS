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

/**
 * IGDB.com — requires a Twitch OAuth2 app (Client ID + Client Secret).
 * POST /v4/games with an apicalypse-style body; images built from image_id.
 */
class IgdbSource : GameMetadataSource {

    override val id = ScraperSourceId.IGDB

    private var cachedToken: String? = null

    override fun isAvailable(settings: ScraperSettings): Boolean =
        settings.igdbClientId.isNotBlank() && settings.igdbClientSecret.isNotBlank()

    private val platformIds: Map<Platform, List<Int>> = mapOf(
        Platform.PLAYSTATION_1 to listOf(7),
        Platform.PSP to listOf(38),
        Platform.PLAYSTATION_2 to listOf(8),
        Platform.GAMECUBE_WII to listOf(21, 5),
        Platform.ANDROID to listOf(34)
    )

    override suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult {
        val hit = bestGame(game, settings) ?: return ScraperResult()
        val coverId = hit.item.optJSONObject("cover")?.optString("image_id")
        val screenshotId = hit.item.optJSONArray("screenshots")
            ?.takeIf { it.length() > 0 }
            ?.optJSONObject(0)
            ?.optString("image_id")
        return ScraperResult(
            fullTitle = hit.name,
            coverUrl = coverId?.takeIf { it.isNotBlank() }?.let { imageUrl("cover_big", it) },
            screenshotUrl = screenshotId?.takeIf { it.isNotBlank() }?.let { imageUrl("screenshot_big", it) }
        )
    }

    override suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> {
        val hit = bestGame(game, settings) ?: return emptyList()
        val gameId = hit.item.optInt("id", 0).takeIf { it > 0 } ?: return emptyList()
        val token = cachedToken ?: return emptyList()

        val result = mutableListOf<MediaVariant>()
        result += queryMedia(settings, token, "covers", gameId, "cover_big", MediaType.COVER)
        result += queryMedia(settings, token, "screenshots", gameId, "screenshot_big", MediaType.SCREENSHOT)
        return result
    }

    private suspend fun queryMedia(
        settings: ScraperSettings,
        token: String,
        endpoint: String,
        gameId: Int,
        size: String,
        type: MediaType
    ): List<MediaVariant> {
        val raw = HttpClient.postText(
            url = "https://api.igdb.com/v4/$endpoint",
            body = "fields image_id,width,height; where game = $gameId; limit 50;",
            headers = mapOf(
                "Client-ID" to settings.igdbClientId,
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        ) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val imageId = item.optString("image_id").takeIf { it.isNotBlank() } ?: continue
                add(
                    MediaVariant(
                        url = imageUrl(size, imageId),
                        source = ScraperSourceId.IGDB,
                        type = type,
                        width = item.optInt("width", 0),
                        height = item.optInt("height", 0)
                    )
                )
            }
        }
    }

    private suspend fun bestGame(game: Game, settings: ScraperSettings): GameCandidate? {
        if (!isAvailable(settings)) return null
        val token = obtainToken(settings) ?: return null
        val platforms = platformIds[game.platform]?.takeIf { it.isNotEmpty() } ?: return null

        val body = buildString {
            append("search \"${game.searchName().replace("\"", "\\\"")}\"; ")
            append("fields name,id,cover.image_id,screenshots.image_id; ")
            append("where platforms = (${platforms.joinToString(",")}); ")
            append("limit 10;")
        }
        val raw = HttpClient.postText(
            url = "https://api.igdb.com/v4/games",
            body = body,
            headers = mapOf(
                "Client-ID" to settings.igdbClientId,
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        ) ?: return null

        val results = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        val candidates = buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                add(GameCandidate(name, item))
            }
        }
        val matchIndex = TitleMatcher.best(game.searchName(), candidates.map { it.name })
            ?: return null
        return candidates[matchIndex]
    }

    private suspend fun obtainToken(settings: ScraperSettings): String? {
        cachedToken?.let { return it }
        val raw = HttpClient.postText(
            url = "https://id.twitch.tv/oauth2/token?client_id=${HttpClient.encode(settings.igdbClientId)}&client_secret=${HttpClient.encode(settings.igdbClientSecret)}&grant_type=client_credentials",
            body = ""
        ) ?: return null
        val token = runCatching { JSONObject(raw).optString("access_token") }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (token != null) cachedToken = token
        return token
    }

    private fun imageUrl(size: String, imageId: String): String =
        "https://images.igdb.com/igdb/image/upload/t_$size/$imageId.jpg"

    private data class GameCandidate(val name: String, val item: JSONObject)
}
