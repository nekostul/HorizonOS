package ru.nekostul.horizonos.ui.games

import android.content.Context
import kotlinx.coroutines.flow.first

/**
 * Silently rescans every remembered ROM folder on launch and adds any new
 * games found. Runs at most once per process.
 */
object FolderRescan {

    data class Result(val added: List<Game>, val platforms: List<Platform>)

    @Volatile
    private var claimed = false

    @Volatile
    private var finished = false

    /** Returns true when this process has already started the rescan. */
    fun isClaimed(): Boolean = claimed

    /** Claims the one-shot rescan; returns false when it was already claimed. */
    @Synchronized
    fun claim(): Boolean {
        if (claimed) return false
        claimed = true
        return true
    }

    fun isFinished(): Boolean = finished

    suspend fun rescan(context: Context): Result {
        val folders = GameFolderRepository(context).load()
        if (folders.isEmpty()) {
            finished = true
            return Result(emptyList(), emptyList())
        }
        val library = GameLibrary(context)
        val scanner = GameScanner(context)
        val seen = library.games.first().mapTo(mutableSetOf()) { it.identityKey }
        val added = mutableListOf<Game>()

        folders.forEach { folder ->
            val scan = runCatching {
                scanner.scanWithDetails(folder.path, folder.platform, folder.emulator)
            }.getOrNull() ?: return@forEach
            val fresh = scan.games.filter { it.identityKey !in seen }
            if (fresh.isNotEmpty()) {
                library.addAll(fresh)
                fresh.forEach { seen.add(it.identityKey) }
                added += fresh
            }
        }

        finished = true
        return Result(added = added, platforms = added.map { it.platform }.distinct())
    }
}
