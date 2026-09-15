package ru.nekostul.horizonos.ui.settings.launcher.scanning.sources

import org.json.JSONArray
import org.json.JSONObject
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.settings.launcher.scanning.HttpClient
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaType
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperResult
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId
import ru.nekostul.horizonos.ui.settings.launcher.scanning.TitleMatcher
import ru.nekostul.horizonos.ui.settings.launcher.scanning.searchName

class SteamGridDbSource : GameMetadataSource {

    override val id = ScraperSourceId.STEAM_GRID_DB

    override fun isAvailable(settings: ScraperSettings): Boolean =
        settings.steamGridDbApiKey.isNotBlank()

    override suspend fun searchMetadata(game: Game, settings: ScraperSettings): ScraperResult {
        val match = bestGame(game, settings) ?: return ScraperResult()
        val cover = grids(match.id, settings).firstOrNull()?.url
            ?: heroes(match.id, settings).firstOrNull()
        return ScraperResult(fullTitle = match.name, coverUrl = cover, screenshotUrl = null)
    }

    override suspend fun searchVariants(game: Game, settings: ScraperSettings): List<MediaVariant> {
        val match = bestGame(game, settings) ?: return emptyList()
        return grids(match.id, settings)
    }

    private suspend fun bestGame(game: Game, settings: ScraperSettings): GameCandidate? {
        if (!isAvailable(settings)) return null
        val searchRaw = HttpClient.getText(
            url = "https://www.steamgriddb.com/api/v2/search/autocomplete/" +
                HttpClient.encode(game.searchName()),
            headers = bearer(settings)
        ) ?: return null

        val root = runCatching { JSONObject(searchRaw) }.getOrNull() ?: return null
        val results = root.optJSONArray("data") ?: return null
        val candidates = buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
                val id = item.optInt("id", -1)
                if (id > 0) add(GameCandidate(name, id))
            }
        }
        val matchIndex = TitleMatcher.best(game.searchName(), candidates.map { it.name })
            ?: return null
        return candidates[matchIndex]
    }

    private suspend fun grids(gameId: Int, settings: ScraperSettings): List<MediaVariant> {
        val raw = HttpClient.getText(
            url = "https://www.steamgriddb.com/api/v2/grids/game/$gameId",
            headers = bearer(settings)
        ) ?: return emptyList()
        val items = parseArray(raw) ?: return emptyList()
        val variants = buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val url = item.optString("url").takeIf { it.isNotBlank() } ?: continue
                add(
                    MediaVariant(
                        url = url,
                        source = ScraperSourceId.STEAM_GRID_DB,
                        type = MediaType.COVER,
                        width = item.optInt("width", 0),
                        height = item.optInt("height", 0)
                    )
                )
            }
        }
        return variants.sortedByDescending { it.isSquare }
    }

    private suspend fun heroes(gameId: Int, settings: ScraperSettings): List<String> {
        val raw = HttpClient.getText(
            url = "https://www.steamgriddb.com/api/v2/heroes/game/$gameId",
            headers = bearer(settings)
        ) ?: return emptyList()
        val items = parseArray(raw) ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                item.optString("url").takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun parseArray(raw: String): JSONArray? = runCatching {
        val root = JSONObject(raw)
        root.optJSONArray("data")
    }.getOrNull()

    private fun bearer(settings: ScraperSettings): Map<String, String> =
        mapOf("Authorization" to "Bearer ${settings.steamGridDbApiKey}")

    private data class GameCandidate(val name: String, val id: Int)
}
