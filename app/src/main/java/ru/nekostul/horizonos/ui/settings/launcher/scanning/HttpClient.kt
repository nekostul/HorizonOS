package ru.nekostul.horizonos.ui.settings.launcher.scanning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal object HttpClient {
    private const val CONNECT_TIMEOUT = 10_000
    private const val READ_TIMEOUT = 15_000

    suspend fun getText(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String? = request(url, headers = headers)?.let { String(it, Charsets.UTF_8) }

    suspend fun getBytes(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): ByteArray? = request(url, headers = headers)

    suspend fun postText(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): String? = request(url, method = "POST", body = body.toByteArray(Charsets.UTF_8), headers = headers)
        ?.let { String(it, Charsets.UTF_8) }

    private suspend fun request(
        url: String,
        method: String = "GET",
        body: ByteArray? = null,
        headers: Map<String, String> = emptyMap()
    ): ByteArray? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "HorizonOS/1.0")
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.use { it.write(body) }
                }
            }
            val code = connection.responseCode
            if (code !in 200..299) return@withContext null
            connection.inputStream.use { it.readBytes() }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    suspend fun isNetworkReachable(): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.screenscraper.fr/api2/systemesListe.php").openConnection() as HttpURLConnection).apply {
                connectTimeout = 6_000
                readTimeout = 6_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "HorizonOS/1.0")
            }
            connection.responseCode in 100..599
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }
}
