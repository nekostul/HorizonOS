package ru.nekostul.horizonos.ui.files

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import ru.nekostul.horizonos.ui.settings.PrivilegedSystemAccess

object RootHelper {

    private const val COMMAND_TIMEOUT_MS = 8_000L

    @Volatile
    var cachedRoot: Boolean? = null

    /**
     * Delegates to [PrivilegedSystemAccess], which bounds the probe and never
     * caches a negative result forever. Call this off the main thread: a freshly
     * granted root permission may show the root manager prompt and block.
     */
    fun isRootAvailable(): Boolean {
        cachedRoot?.let { return it }
        val available = PrivilegedSystemAccess.hasRootAccess()
        if (available) cachedRoot = true
        return available
    }

    fun setRootAvailable() {
        cachedRoot = true
    }

    fun hasSuBinary(): Boolean {
        listOf("su", "/system/bin/su", "/system/xbin/su").forEach { su ->
            if (File(su).exists() || isExecutableInPath(su)) return true
        }
        return false
    }

    private fun isExecutableInPath(name: String): Boolean =
        System.getenv("PATH")?.split(':')?.any { File(it, name).canExecute() } == true

    fun runRoot(command: String, timeoutMs: Int = COMMAND_TIMEOUT_MS.toInt()): Pair<Int, String> {
        return if (cachedRoot ?: isRootAvailable()) {
            runShellCommand(command, timeoutMs.toLong())
        } else {
            Int.MIN_VALUE to ""
        }
    }

    private fun runShellCommand(command: String, timeoutMs: Long): Pair<Int, String> {
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = StringBuilder()
            val drain = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        reader.forEachLine { line ->
                            synchronized(output) { output.appendLine(line) }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            drain.isDaemon = true
            drain.start()
            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return -1 to ""
            }
            drain.join(300)
            process.exitValue() to output.toString().trim()
        } catch (_: Exception) {
            -1 to ""
        }
    }

    fun readRootBytes(path: String, timeoutMs: Int = COMMAND_TIMEOUT_MS.toInt()): ByteArray? {
        if (!(cachedRoot ?: isRootAvailable())) return null
        return runCatching {
            val process = ProcessBuilder("su", "-c", "cat \"$path\"")
                .redirectErrorStream(true)
                .start()
            val bytes = process.inputStream.readBytes()
            if (!process.waitFor(timeoutMs.toLong(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() == 0) bytes else null
        }.getOrNull()
    }

    fun listRoot(path: String): List<FileEntry> {
        val (code, out) = runRoot("ls -laH \"$path\"")
        if (code != 0 && code != Int.MIN_VALUE) return emptyList()
        val entries = mutableListOf<FileEntry>()
        out.lineSequence().forEach { line ->
            if (line.startsWith("total ") || line.isBlank()) return@forEach
            val entry = parseLsLine(path, line) ?: return@forEach
            entries += entry
        }
        return entries.distinctBy { it.path }
    }

    private fun parseLsLine(parent: String, line: String): FileEntry? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val fields = trimmed.split(Regex("\\s+"))
        if (fields.size < 8) return null
        val perms = fields[0]
        if (perms.isEmpty() || perms[0] !in "dl-") return null
        val isDir = perms[0] == 'd'
        val size = fields.getOrNull(4)?.toLongOrNull() ?: -1L
        val nameStart = if (fields.size >= 9) 8 else 7
        val name = fields.drop(nameStart).joinToString(" ").substringBefore(" -> ")
        if (name.isBlank() || name == "." || name == "..") return null
        val full = if (parent == "/") "/$name" else "$parent/$name"
        return FileEntry(
            name = name,
            path = full,
            isDirectory = isDir,
            size = if (isDir) -1 else size,
            hidden = name.startsWith('.'),
            executable = perms.length > 3 && perms[3] == 'x'
        )
    }
}