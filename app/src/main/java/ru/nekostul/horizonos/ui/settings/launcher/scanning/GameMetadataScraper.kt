package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.util.Log
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLibrary
import java.io.File

private const val TAG = "HorizonScraper"

/** Progress emitted while scraping games. */
data class ScraperProgress(
    val index: Int,
    val total: Int,
    val gameTitle: String
)

/** Final summary of a scan run. */
data class ScanSummary(
    val total: Int,
    val updated: Int,
    val failed: Int,
    val networkUnavailable: Boolean,
    val diagnostics: List<String> = emptyList()
)

/**
 * Orchestrates scraping. Sources are consulted strictly in priority order.
 * The order is fixed by [ScraperSourceId.entries]; each field is filled once
 * and never overwritten by a lower-priority source.
 */
class GameMetadataScraper(
    private val gameLibrary: GameLibrary,
    mediaDir: File
) {

    private val imageProcessor = ImageProcessor(mediaDir)

    private val sources = ScraperSources.ordered

    suspend fun scrape(
        settings: ScraperSettings,
        games: List<Game>,
        onProgress: (ScraperProgress) -> Unit
    ): ScanSummary {
        var updated = 0
        var failed = 0
        val diagnostics = mutableListOf<String>()
        fun diag(message: String) {
            Log.d(TAG, message)
            if (diagnostics.size < 400) diagnostics += message
        }

        val networkUnavailable = !HttpClient.isNetworkReachable()
        diag("Scan started. enabled sources=${ScraperSourceId.entries.filter { settings.isEnabled(it) }.map { it.storageKey }}")
        if (networkUnavailable) diag("Network check failed — scanning skipped.")

        games.forEachIndexed { index, game ->
            onProgress(ScraperProgress(index = index + 1, total = games.size, gameTitle = game.displayTitle))
            if (networkUnavailable) {
                failed++
                return@forEachIndexed
            }
            if (scrapeSingle(settings, game, ::diag)) updated++ else failed++
        }

        diag("Scan finished. updated=$updated noData=$failed")
        return ScanSummary(
            total = games.size,
            updated = updated,
            failed = failed,
            networkUnavailable = networkUnavailable,
            diagnostics = diagnostics
        )
    }

    /**
     * Scrapes a single game and writes the result back to the library. Returns
     * true when something changed. Exposed so the scan queue can persist its
     * position after every game and resume after a restart.
     */
    suspend fun scrapeSingle(
        settings: ScraperSettings,
        game: Game,
        diag: (String) -> Unit = {}
    ): Boolean {
        // A field is settled when it already has a value. Android apps created
        // before scraping support may carry a stale "manual cover" flag with no
        // actual cover; value-based checks let them be scraped.
        val titleSettled = game.fullTitle != null
        val coverSettled = game.coverPath != null
        val screenshotSettled = game.screenshotPath != null
        if (titleSettled && coverSettled && screenshotSettled) {
            diag("[${game.displayTitle}] skipped: all fields already resolved")
            return false
        }
        val result = scrapeGame(settings, game, diag) ?: run {
            diag("[${game.displayTitle}] no new metadata found")
            return false
        }
        if (!result.changed) return false
        gameLibrary.update(
            game.copy(
                fullTitle = result.fullTitle,
                coverPath = result.coverPath,
                screenshotPath = result.screenshotPath
            )
        )
        diag("[${game.displayTitle}] updated: title=${result.fullTitle} cover=${result.coverPath != null} screenshot=${result.screenshotPath != null}")
        return true
    }

    private suspend fun scrapeGame(
        settings: ScraperSettings,
        game: Game,
        diag: (String) -> Unit
    ): GameScrapeResult? {
        // Cache: start from values already stored on the game and only look
        // for the fields that are still missing. A field is locked only when
        // it was set manually AND actually has a value, so a stale manual flag
        // without a value (older Android app records) does not block scraping.
        var fullTitle: String? = game.fullTitle
        var coverPath: String? = game.coverPath
        var screenshotPath: String? = game.screenshotPath
        val titleLocked = game.isTitleManuallySet && fullTitle != null
        val coverLocked = game.isCoverManuallySet && coverPath != null
        val screenshotLocked = game.isScreenshotManuallySet && screenshotPath != null

        for (enabledId in ScraperSourceId.entries) {
            if (!settings.isEnabled(enabledId)) {
                diag("[${game.displayTitle}] ${enabledId.storageKey}: skipped (disabled)")
                continue
            }
            val source = sources.firstOrNull { it.id == enabledId } ?: continue
            if (!source.isAvailable(settings)) {
                diag("[${game.displayTitle}] ${enabledId.storageKey}: skipped (no credentials)")
                continue
            }

            // Skip sources when every desired field is already filled.
            val titleDone = titleLocked || fullTitle != null
            val coverDone = coverLocked || coverPath != null
            val screenshotDone = screenshotLocked || screenshotPath != null
            if (titleDone && coverDone && screenshotDone) break

            val query = game.searchName()
            val result = runCatching { source.searchMetadata(game, settings) }
                .onFailure { diag("[${game.displayTitle}] ${enabledId.storageKey}: error ${it.javaClass.simpleName}") }
                .getOrNull()
            if (result == null) {
                diag("[${game.displayTitle}] ${enabledId.storageKey}: query='$query' -> no response")
                continue
            }
            diag(
                "[${game.displayTitle}] ${enabledId.storageKey}: query='$query' -> " +
                    "title=${result.fullTitle != null} cover=${result.coverUrl != null} screenshot=${result.screenshotUrl != null}"
            )

            if (!titleLocked && fullTitle == null) {
                result.fullTitle?.takeIf { it.isNotBlank() }?.let {
                    fullTitle = it
                    diag("[${game.displayTitle}] title <- ${enabledId.storageKey}: '$it'")
                }
            }
            if (!coverLocked && coverPath == null) {
                result.coverUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    coverPath = imageProcessor.storeCover(game.id, url)
                    diag("[${game.displayTitle}] cover <- ${enabledId.storageKey}: ${if (coverPath != null) "saved" else "download failed"}")
                }
            }
            if (!screenshotLocked && screenshotPath == null) {
                result.screenshotUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    screenshotPath = imageProcessor.storeScreenshot(game.id, url)
                    diag("[${game.displayTitle}] screenshot <- ${enabledId.storageKey}: ${if (screenshotPath != null) "saved" else "download failed"}")
                }
            }
        }

        val changed = fullTitle != game.fullTitle ||
            coverPath != game.coverPath ||
            screenshotPath != game.screenshotPath
        if (!changed) return null
        return GameScrapeResult(fullTitle, coverPath, screenshotPath, changed)
    }

    private data class GameScrapeResult(
        val fullTitle: String?,
        val coverPath: String?,
        val screenshotPath: String?,
        val changed: Boolean
    )
}