package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.content.pm.ShortcutInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator

/**
 * Quietly adds newly created GameHub / GameNative / Winlator shortcuts to the
 * library every time the launcher comes back to the foreground.
 */
object WindowsShortcutSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var running = false

    @Volatile
    private var initialSweepDone = false

    fun syncAsync(context: Context) {
        if (running) return
        running = true
        val appContext = context.applicationContext
        scope.launch {
            try {
                sync(appContext)
            } catch (_: Throwable) {
            } finally {
                running = false
            }
        }
    }

    suspend fun sync(context: Context): Int {
        val library = GameLibrary(context)
        val current = runCatching { library.games.first() }.getOrDefault(emptyList())
        val ignored = DeletedRomRepository(context).all()
        val known = current.mapTo(mutableSetOf()) { it.identityKey }

        val scanned = WindowsShortcutRepository(context).scan()
        val fresh = scanned.filter { it.romUri !in ignored && it.identityKey !in known }
        val added = if (fresh.isNotEmpty()) library.addAll(fresh) else 0

        // On the first sync of a process also pick up shortcut games that were
        // added earlier but never had their metadata scanned.
        val sweep = if (!initialSweepDone) {
            initialSweepDone = true
            current.filter {
                it.platform == Platform.WINDOWS &&
                    it.coverPath == null &&
                    it.screenshotPath == null
            }
        } else {
            emptyList()
        }

        val toScan = (fresh + sweep).distinctBy { it.id }
        if (toScan.isNotEmpty()) enqueueScan(context, toScan)
        return added
    }

    /** Adds a single shortcut right after the user confirms pinning it. */
    fun addAsync(context: Context, info: ShortcutInfo) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                val game = WindowsShortcutRepository(appContext).gameFrom(info) ?: return@launch
                if (game.romUri in DeletedRomRepository(appContext).all()) return@launch
                val added = GameLibrary(appContext).addAll(listOf(game))
                if (added > 0) enqueueScan(appContext, listOf(game))
            }
        }
    }

    private fun enqueueScan(context: Context, games: List<Game>) {
        ScanCoordinator.init(context)
        ScanCoordinator.enqueue(games)
    }
}