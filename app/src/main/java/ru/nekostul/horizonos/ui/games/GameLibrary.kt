package ru.nekostul.horizonos.ui.games

import android.content.Context
import kotlinx.coroutines.flow.Flow

class GameLibrary(context: Context) {
    private val repository = GameRepository(context)

    val games: Flow<List<Game>> = repository.games

    suspend fun add(game: Game): Int = repository.addAll(listOf(game))

    suspend fun addAll(games: Iterable<Game>): Int = repository.addAll(games)

    suspend fun remove(game: Game) = repository.remove(game.id)

    suspend fun update(game: Game) = repository.update(game)

    suspend fun setHidden(game: Game, hidden: Boolean) = update(game.copy(hidden = hidden))

    suspend fun removeByRomUris(romUris: Set<String>) = repository.removeByRomUris(romUris)
}
