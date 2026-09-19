package ru.nekostul.horizonos.ui.settings

import android.os.SystemClock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object PrivilegedSystemAccess {
    private const val COMMAND_TIMEOUT_SECONDS = 6L
    private const val PROBE_TIMEOUT_SECONDS = 3L
    private const val NEGATIVE_CACHE_MILLIS = 10_000L

    @Volatile
    private var cachedRootAccess: Boolean? = null

    @Volatile
    private var lastProbeAt = 0L

    data class Result(val exitCode: Int, val output: String) {
        val succeeded: Boolean get() = exitCode == 0
    }

    /**
     * Returns the last known root state. A positive result is cached forever,
     * a negative result only for a short window so a freshly granted root
     * permission is picked up without restarting the app.
     */
    fun hasRootAccess(): Boolean {
        val cached = cachedRootAccess
        if (cached == true) return true
        val now = SystemClock.elapsedRealtime()
        if (cached == false && now - lastProbeAt < NEGATIVE_CACHE_MILLIS) return false
        synchronized(this) {
            val current = cachedRootAccess
            val currentTime = SystemClock.elapsedRealtime()
            if (current == true) return true
            if (current == false && currentTime - lastProbeAt < NEGATIVE_CACHE_MILLIS) return false
            val result = probeRootAccess()
            cachedRootAccess = result
            lastProbeAt = SystemClock.elapsedRealtime()
            return result
        }
    }

    fun resetRootCache() {
        synchronized(this) {
            cachedRootAccess = null
            lastProbeAt = 0L
        }
    }

    fun run(command: String): Result? = runCommand(command, asRoot = true)

    private fun probeRootAccess(): Boolean = runCommand(
        "id",
        asRoot = true,
        timeoutSeconds = PROBE_TIMEOUT_SECONDS
    )?.let { it.succeeded && (it.output.contains("uid=0") || it.output.contains("uid: 0")) } == true

    private fun runCommand(
        command: String,
        asRoot: Boolean,
        timeoutSeconds: Long = COMMAND_TIMEOUT_SECONDS
    ): Result? {
        val process = try {
            val builder = if (asRoot) {
                ProcessBuilder("su", "-c", command)
            } else {
                ProcessBuilder("sh", "-c", command)
            }
            builder.redirectErrorStream(true).start()
        } catch (_: Exception) {
            return null
        }

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

        val finished = try {
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            false
        }
        if (!finished) {
            process.destroyForcibly()
            return null
        }
        return try {
            drain.join(500)
            Result(process.exitValue(), output.toString().trim())
        } catch (_: Exception) {
            null
        }
    }
}