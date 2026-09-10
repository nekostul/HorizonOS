package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

class GameScanner(private val context: Context) {
    data class ScanResult(
        val games: List<Game>,
        val playlistMemberUris: Set<String>
    )

    fun scan(folderUri: String, platform: Platform, emulator: Emulator): List<Game> =
        scanWithDetails(folderUri, platform, emulator).games

    fun scanWithDetails(folderUri: String, platform: Platform, emulator: Emulator): ScanResult {
        val tree = DocumentFile.fromTreeUri(context, android.net.Uri.parse(folderUri))
            ?: return ScanResult(emptyList(), emptySet())

        val files = collectFiles(tree)
        val playlists = files.filter { it.file.name.extensionLowercase() == "m3u" }
        val playlistMembers = playlists
            .flatMap { playlist -> readPlaylistMembers(playlist, files) }
            .toSet()

        val games = files
            .asSequence()
            .filter { it.file.isFile && platform.supportsFileName(it.file.name) }
            .filter { scanned ->
                // The playlist is the game entry. Its referenced discs are
                // implementation details and must not become separate cards.
                scanned.relativePath !in playlistMembers ||
                    scanned.file.name.extensionLowercase() == "m3u"
            }
            .map { scanned ->
                val name = scanned.file.name ?: scanned.relativePath
                Game.fromRom(
                    title = name.substringBeforeLast('.', name),
                    platform = platform,
                    emulator = emulator,
                    romUri = scanned.file.uri.toString(),
                    romName = name
                )
            }
            .distinctBy { it.identityKey }
            .toList()
        val playlistMemberUris = files
            .filter { it.relativePath in playlistMembers }
            .mapTo(mutableSetOf()) { it.file.uri.toString() }
        return ScanResult(games, playlistMemberUris)
    }

    private data class ScannedFile(
        val file: DocumentFile,
        val relativePath: String
    )

    private fun collectFiles(
        directory: DocumentFile,
        parentPath: String = ""
    ): List<ScannedFile> = buildList {
        val children = runCatching { directory.listFiles().toList() }.getOrDefault(emptyList())
        children.forEach { child ->
            val name = child.name ?: return@forEach
            val relativePath = joinPath(parentPath, name)
            if (child.isDirectory) {
                addAll(collectFiles(child, relativePath))
            } else if (child.isFile) {
                add(ScannedFile(child, relativePath))
            }
        }
    }

    private fun readPlaylistMembers(
        playlist: ScannedFile,
        files: List<ScannedFile>
    ): Set<String> {
        val playlistParent = playlist.relativePath.substringBeforeLast('/', "")
        val lines = runCatching {
            context.contentResolver.openInputStream(playlist.file.uri)
                ?.bufferedReader()
                ?.useLines { sequence -> sequence.toList() }
                .orEmpty()
        }.getOrDefault(emptyList())

        val members = mutableSetOf<String>()
        lines.forEach { rawLine ->
            val entry = rawLine.trim()
            if (entry.isBlank() || entry.startsWith("#")) return@forEach

            val normalizedEntry = normalizePlaylistPath(entry)
            val resolvedPath = normalizePlaylistPath(
                if (normalizedEntry.startsWith('/')) {
                    normalizedEntry.removePrefix("/")
                } else {
                    joinPath(playlistParent, normalizedEntry)
                }
            )

            files.firstOrNull { file ->
                file.relativePath.equals(resolvedPath, ignoreCase = true)
            }?.let { members += it.relativePath }

            // Some playlists contain only a filename while the referenced
            // disc sits in a nested folder. Keep a safe filename fallback.
            if (members.none { it.equals(resolvedPath, ignoreCase = true) }) {
                val filename = normalizedEntry.substringAfterLast('/')
                files.filter { file ->
                    file.file.isFile &&
                        file.file.name.equals(filename, ignoreCase = true)
                }.forEach { members += it.relativePath }
            }
        }
        return members
    }

    private fun normalizePlaylistPath(path: String): String {
        val decoded = Uri.decode(path)
            .removePrefix("file://")
            .replace('\\', '/')
        val parts = decoded.split('/').filter { it.isNotBlank() && it != "." }
        val normalized = ArrayDeque<String>()
        parts.forEach { part ->
            if (part == "..") {
                if (normalized.isNotEmpty()) normalized.removeLast()
            } else {
                normalized.addLast(part)
            }
        }
        return normalized.joinToString("/")
    }

    private fun joinPath(parent: String, child: String): String = listOf(parent, child)
        .filter { it.isNotBlank() }
        .joinToString("/")

    private fun String?.extensionLowercase(): String = this
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase(Locale.ROOT)
        .orEmpty()

}
