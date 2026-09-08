package ru.nekostul.horizonos.ui.settings

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Optional bridge for a rooted/system HorizonOS build.
 *
 * This is deliberately isolated from Compose. A normal APK simply gets null
 * results; no fake state is exposed to the UI. A system build may replace this
 * implementation with framework Binder calls without changing screens.
 */
object PrivilegedSystemAccess {
    @Volatile
    private var cachedRootAccess: Boolean? = null

    data class Result(val exitCode: Int, val output: String) {
        val succeeded: Boolean get() = exitCode == 0
    }

    fun hasRootAccess(): Boolean = cachedRootAccess ?: synchronized(this) {
        cachedRootAccess ?: (runCommand("id", asRoot = true)?.let {
            it.succeeded && (it.output.contains("uid=0") || it.output.contains("uid: 0"))
        } == true).also { cachedRootAccess = it }
    }

    fun run(command: String): Result? = runCommand(command, asRoot = true)

    private fun runCommand(command: String, asRoot: Boolean): Result? = runCatching {
        val process = if (asRoot) {
            ProcessBuilder("su", "-c", command)
        } else {
            ProcessBuilder("sh", "-c", command)
        }.redirectErrorStream(true).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            buildString { reader.forEachLine { appendLine(it) } }
        }.trim()
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        Result(process.exitValue(), output)
    }.getOrNull()
}
