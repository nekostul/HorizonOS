package ru.nekostul.horizonos.ui.games.emulators

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult
import java.io.File
import java.nio.charset.StandardCharsets

class DuckStationLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.DUCKSTATION

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val extraUris = mutableListOf<String>()
        val preparedGame = if (!game.romUri.startsWith("content://") &&
            game.romUri.endsWith(".m3u", ignoreCase = true)
        ) {
            preparePlaylist(context, game, extraUris)
                ?: return GameLaunchResult.Failed(
                    context.getString(ru.nekostul.horizonos.R.string.games_error_file_unavailable)
                )
        } else {
            game
        }

        // DuckStation читает bootPath нативно. На Android 16 обычный путь
        // /storage/... для другого приложения недоступен, поэтому передаём
        // ему content:// URI, на который можно выдать разрешение.
        val gameForLaunch = if (preparedGame.romUri.startsWith("content://")) {
            preparedGame
        } else {
            val file = File(preparedGame.romUri)
            if (!file.canRead()) {
                return GameLaunchResult.Failed(
                    context.getString(ru.nekostul.horizonos.R.string.games_error_file_unavailable)
                )
            }
            val uri = if (file.extension.equals("m3u", ignoreCase = true)) {
                RomContentProvider.uriForFile(file)
            } else {
                FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            }
            preparedGame.copy(romUri = uri.toString())
        }

        extraUris.forEach { uri ->
            runCatching {
                context.grantUriPermission(
                    PACKAGE_NAME,
                    android.net.Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

        return launchRomIntent(
            context = context,
            game = gameForLaunch,
            packageName = PACKAGE_NAME,
            activityName = "$PACKAGE_NAME.EmulationActivity",
            intent = Intent(Intent.ACTION_MAIN).apply {
                // DuckStation запускает конкретную игру через bootPath.
                // Одного ACTION_VIEW с URI недостаточно: activity открывается,
                // но игра не передаётся в эмулятор.
                putExtra("bootPath", gameForLaunch.romUri)
                putExtra("resumeState", false)
            }
        )
    }

    private fun preparePlaylist(
        context: Context,
        game: Game,
        grantedUris: MutableList<String>
    ): Game? {
        val playlist = File(game.romUri)
        if (!playlist.isFile || !playlist.canRead()) return null
        val parent = playlist.parentFile ?: return null
        val output = File(
            context.cacheDir,
            "duckstation-${playlist.nameWithoutExtension}-${playlist.lastModified()}.m3u"
        )
        val lines = runCatching {
            playlist.readLines(StandardCharsets.UTF_8)
        }.getOrNull() ?: return null

        val rewritten = lines.map { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#")) {
                rawLine
            } else {
                val entry = line.removeSurrounding("\"")
                val file = File(entry).let { candidate ->
                    if (candidate.isAbsolute) candidate else File(parent, entry)
                }.canonicalFile
                if (!file.isFile || !file.canRead()) return null
                val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                grantedUris += uri.toString()
                uri.toString()
            }
        }

        return runCatching {
            output.writeText(rewritten.joinToString("\n"), StandardCharsets.UTF_8)
            game.copy(romUri = RomContentProvider.uriForFile(output).toString())
        }.getOrNull()
    }

    private companion object {
        const val PACKAGE_NAME = "com.github.stenzek.duckstation"
        const val FILE_PROVIDER_AUTHORITY = "ru.nekostul.horizonos.fileprovider"
    }
}
