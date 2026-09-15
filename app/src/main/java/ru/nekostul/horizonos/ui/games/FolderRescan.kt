package ru.nekostul.horizonos.ui.games

import android.content.Context
import kotlinx.coroutines.flow.first

object FolderRescan {

    data class Result(val added: List<Game>, val platforms: List<Platform>)

    @Volatile
    private var claimed = false

    @Volatile
    private var finished = false

    fun isClaimed(): Boolean = claimed

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
        val deleted = DeletedRomRepository(context).all()
        val seen = library.games.first().mapTo(mutableSetOf()) { it.identityKey }
        val added = mutableListOf<Game>()

        folders.forEach { folder ->
            val scan = runCatching {
                if (folder.path.startsWith("content://")) {
                    scanner.scanWithDetails(folder.path, folder.platform, folder.emulator)
                } else {
                    scanner.scanDirectory(java.io.File(folder.path), folder.platform, folder.emulator)
                }
            }.getOrNull() ?: return@forEach
            val fresh = scan.games.filter { it.identityKey !in seen && it.romUri !in deleted }
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
