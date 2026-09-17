package ru.nekostul.horizonos.ui.files

import java.io.File

object RootHelper {

    var cachedRoot: Boolean? = null

    fun isRootAvailable(): Boolean {
        cachedRoot?.let { return it }
        val result = runBlockingShell(500, "id")
        val available = result?.contains("uid=0") == true
        cachedRoot = available
        return available
    }

    fun hasSuBinary(): Boolean {
        listOf("su", "/system/bin/su", "/system/xbin/su").forEach { su ->
            if (File(su).exists() || isExecutableInPath(su)) return true
        }
        return false
    }

    private fun isExecutableInPath(name: String): Boolean =
        System.getenv("PATH")?.split(':')?.any { File(it, name).canExecute() } == true

    fun runRoot(command: String, timeoutMs: Int = 8000): Pair<Int, String> {
        return if (cachedRoot ?: isRootAvailable()) {
            runShellCommand(command, timeoutMs)
        } else {
            Int.MIN_VALUE to ""
        }
    }

    private fun runShellCommand(command: String, timeoutMs: Int): Pair<Int, String> {
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            val code = if (process.isAlive) {
                process.destroyForcibly(); -1
            } else {
                process.exitValue()
            }
            code to output
        } catch (_: Exception) {
            -1 to ""
        }
    }

    private fun runBlockingShell(timeoutMs: Int, cmd: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output
        } catch (_: Exception) {
            null
        }
    }

    fun readRootBytes(path: String, timeoutMs: Int = 8000): ByteArray? {
        return if (cachedRoot ?: isRootAvailable()) {
            runCatching {
                val process = ProcessBuilder("su", "-c", "cat \"$path\"")
                    .redirectErrorStream(true)
                    .start()
                val bytes = process.inputStream.readBytes()
                process.waitFor()
                if (process.exitValue() == 0) bytes else null
            }.getOrNull()
        } else {
            null
        }
    }

    fun listRoot(path: String): List<FileEntry> {
        val (code, out) = runRoot(
            "ls -laH \"$path\""
        )
        if (code != 0 && code != Int.MIN_VALUE) return emptyList()
        val entries = mutableListOf<FileEntry>()
        out.lineSequence().forEach { line ->
            if (line.startsWith("total ") || line.isBlank()) return@forEach
            val entry = parseLsLine(path, line) ?: return@forEach
            entries += entry
        }
        return entries
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
        val name = fields.drop(nameStart).joinToString(" ")
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
