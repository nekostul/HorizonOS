package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLibrary

/**
 * Application-wide scan queue. It survives leaving Settings and Home and is
 * kept alive in the background by [ScanService]. The position is persisted
 * after every game, so if the process is killed the scan resumes from the
 * same point on the next launch.
 */
object ScanCoordinator {

    private const val TAG = "ScanCoordinator"
    private const val PREFS = "scan_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_IDS = "ids"
    private const val KEY_DONE = "done"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _progress = MutableStateFlow<ScraperProgress?>(null)
    val progress: StateFlow<ScraperProgress?> = _progress.asStateFlow()

    private val _summary = MutableStateFlow<ScanSummary?>(null)
    val summary: StateFlow<ScanSummary?> = _summary.asStateFlow()

    private val _hint = MutableStateFlow<List<String>>(emptyList())
    val hint: StateFlow<List<String>> = _hint.asStateFlow()

    // Ordered ids for the whole run; `done` counts finished ones.
    private val runIds = mutableListOf<String>()
    private val runIdSet = mutableSetOf<String>()
    private var done = 0
    private var appContext: Context? = null
    private var workerRunning = false

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (!workerRunning) resumeIfNeeded(ctx)
    }

    /**
     * A game still needs a scan while any metadata field is missing. Fully
     * resolved games are never queued, so no progress notification is shown
     * for a scan that would not actually download anything.
     */
    private val Game.needsScanning: Boolean
        get() = fullTitle == null || coverPath == null || screenshotPath == null

    /** Queue the given games for scanning (used after adding games). */
    fun enqueue(games: List<Game>) {
        val pending = games.filter { it.needsScanning }
        if (pending.isEmpty()) return
        synchronized(this) {
            pending.forEach { game ->
                if (runIdSet.add(game.id)) runIds.add(game.id)
            }
            persist()
        }
        startIfNeeded()
    }

    /** Queue every game currently in the library (manual scan). */
    fun enqueueAll() {
        val ctx = appContext ?: return
        _hint.value = emptyList()
        scope.launch {
            val games = runCatching { GameLibrary(ctx).games.first() }.getOrDefault(emptyList())
            enqueue(games)
        }
    }

    fun consumeHint() {
        _hint.value = emptyList()
    }

    private fun startIfNeeded() {
        val ctx = appContext ?: return
        synchronized(this) {
            if (workerRunning) return
            // Never start the foreground scan notification when there is no
            // actual metadata left to download.
            if (runIds.isEmpty()) return
            workerRunning = true
        }
        _scanning.value = true
        ScanService.start(ctx)
        scope.launch { runWorker(ctx) }
    }

    private suspend fun runWorker(ctx: Context) {
        try {
            val settings = ScraperRepository(ctx).load()
            val library = GameLibrary(ctx)
            val scraper = GameMetadataScraper(library, ctx.filesDir)

            while (true) {
                val snapshot = synchronized(this) {
                    if (done >= runIds.size) null
                    else runIds[done] to runIds.size
                } ?: break
                val (id, total) = snapshot

                val game = runCatching { library.games.first() }
                    .getOrDefault(emptyList())
                    .firstOrNull { it.id == id }

                if (game != null) {
                    _progress.value = ScraperProgress(done + 1, total, game.displayTitle)
                    runCatching { scraper.scrapeSingle(settings, game) }
                }

                synchronized(this) {
                    done++
                    persist()
                }
            }

            finish(ctx, library)
        } catch (error: Throwable) {
            // A failed worker must never leave an endless "scanning" notification.
            Log.e(TAG, "Metadata scan failed", error)
            abort(ctx)
        }
    }

    private suspend fun finish(ctx: Context, library: GameLibrary) {
        val ids = synchronized(this) { runIds.toSet() }
        val after = runCatching { library.games.first() }.getOrDefault(emptyList())
        val missing = after.filter { it.id in ids && it.coverPath == null }
        if (missing.isNotEmpty()) _hint.value = missing.map { it.displayTitle }

        _progress.value = null
        val more = synchronized(this) {
            workerRunning = false
            if (done >= runIds.size) {
                clearState()
                false
            } else {
                true
            }
        }
        if (more) {
            startIfNeeded()
        } else {
            _scanning.value = false
            ScanService.stop(ctx)
        }
    }

    /** Clears the queue and removes the progress notification after a failure. */
    private fun abort(ctx: Context) {
        synchronized(this) {
            workerRunning = false
            clearState()
        }
        _progress.value = null
        _scanning.value = false
        runCatching { ScanService.stop(ctx) }
    }

    private fun resumeIfNeeded(ctx: Context) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return
        val ids = decodeIds(prefs.getString(KEY_IDS, "[]").orEmpty())
        if (ids.isEmpty()) {
            prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
            return
        }
        synchronized(this) {
            runIds.clear()
            runIdSet.clear()
            ids.forEach { runIds.add(it); runIdSet.add(it) }
            done = prefs.getInt(KEY_DONE, 0).coerceIn(0, runIds.size)
        }
        startIfNeeded()
    }

    private fun persist() {
        val ctx = appContext ?: return
        val array = JSONArray()
        runIds.forEach { array.put(it) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, done < runIds.size)
            .putString(KEY_IDS, array.toString())
            .putInt(KEY_DONE, done)
            .apply()
    }

    private fun clearState() {
        runIds.clear()
        runIdSet.clear()
        done = 0
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putBoolean(KEY_ACTIVE, false)
            ?.putString(KEY_IDS, "[]")
            ?.putInt(KEY_DONE, 0)
            ?.apply()
    }

    private fun decodeIds(json: String): List<String> = runCatching {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }.getOrDefault(emptyList())
}
