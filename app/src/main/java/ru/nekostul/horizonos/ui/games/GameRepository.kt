package ru.nekostul.horizonos.ui.games

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gameLibraryDataStore by preferencesDataStore(name = "game_library")

class GameRepository(context: Context) {
    private val appContext = context.applicationContext
    private val gamesKey = stringPreferencesKey("games_json")

    val games: Flow<List<Game>> = appContext.gameLibraryDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences -> decode(preferences[gamesKey].orEmpty()) }

    suspend fun addAll(newGames: Iterable<Game>): Int {
        var added = 0
        appContext.gameLibraryDataStore.edit { preferences ->
            val current = decode(preferences[gamesKey].orEmpty()).toMutableList()
            val known = current.mapTo(mutableSetOf()) { it.identityKey }
            newGames.forEach { game ->
                if (known.add(game.identityKey)) {
                    current += game
                    added++
                }
            }
            preferences[gamesKey] = encode(current)
        }
        return added
    }

    suspend fun remove(gameId: String) {
        appContext.gameLibraryDataStore.edit { preferences ->
            preferences[gamesKey] = encode(
                decode(preferences[gamesKey].orEmpty()).filterNot { it.id == gameId }
            )
        }
    }

    suspend fun update(updatedGame: Game) {
        appContext.gameLibraryDataStore.edit { preferences ->
            preferences[gamesKey] = encode(
                decode(preferences[gamesKey].orEmpty()).map { game ->
                    if (game.id == updatedGame.id) updatedGame else game
                }
            )
        }
    }

    suspend fun removeByRomUris(romUris: Set<String>) {
        if (romUris.isEmpty()) return
        appContext.gameLibraryDataStore.edit { preferences ->
            preferences[gamesKey] = encode(
                decode(preferences[gamesKey].orEmpty()).filterNot { it.romUri in romUris }
            )
        }
    }

    private fun encode(games: List<Game>): String {
        val array = JSONArray()
        games.forEach { game ->
            array.put(
                JSONObject().apply {
                    put("id", game.id)
                    put("title", game.title)
                    put("platform", game.platform.name)
                    put("emulator", game.emulator.name)
                    put("romUri", game.romUri)
                    put("romName", game.romName)
                    put("hidden", game.hidden)
                }
            )
        }
        return array.toString()
    }

    private fun decode(json: String): List<Game> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val platform = item.optString("platform")
                        .let { value -> runCatching { Platform.valueOf(value) }.getOrNull() }
                        ?: continue
                    val emulator = item.optString("emulator")
                        .let { value -> runCatching { Emulator.valueOf(value) }.getOrNull() }
                        ?: continue
                    val uri = item.optString("romUri")
                    if (uri.isBlank()) continue
                    add(
                        Game(
                            id = item.optString("id").ifBlank {
                                Game.fromRom(
                                    title = item.optString("title").ifBlank { uri },
                                    platform = platform,
                                    emulator = emulator,
                                    romUri = uri,
                                    romName = item.optString("romName").ifBlank { uri }
                                ).id
                            },
                            title = item.optString("title").ifBlank { item.optString("romName").ifBlank { uri } },
                            platform = platform,
                            emulator = emulator,
                            romUri = uri,
                            romName = item.optString("romName").ifBlank { uri },
                            hidden = item.optBoolean("hidden", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
