package ru.nekostul.horizonos.ui.settings.launcher.scanning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads and normalizes scraped images.
 * - Covers are forced to 1:1 via center crop (never stretched).
 * - Screenshots keep their native aspect ratio.
 */
class ImageProcessor(
    private val cacheDir: File
) {
    private val coverDir = File(cacheDir, "covers").apply { mkdirs() }
    private val screenshotDir = File(cacheDir, "screenshots").apply { mkdirs() }

    /**
     * Downloads [url] and stores a 1:1 cover for [gameId]. Returns the local
     * path, or null when the download or the decode fails.
     */
    suspend fun storeCover(gameId: String, url: String): String? {
        val bytes = HttpClient.getBytes(url) ?: return null
        val square = toSquare(bytes, maxSize = 512) ?: return null
        val target = File(coverDir, "$gameId.png")
        writePng(target, square)
        return target.absolutePath
    }

    /**
     * Downloads [url] and stores a screenshot preserving aspect ratio.
     * Returns the local path, or null on failure.
     */
    suspend fun storeScreenshot(gameId: String, url: String): String? {
        val bytes = HttpClient.getBytes(url) ?: return null
        val bitmap = decode(bytes) ?: return null
        val target = File(screenshotDir, "$gameId.jpg")
        writeJpeg(target, bitmap)
        return target.absolutePath
    }

    private suspend fun decode(bytes: ByteArray): Bitmap? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

    /**
     * Downloads a small preview for the manual picker. Returns null when the
     * image cannot be downloaded or decoded (e.g. a dead Libretro URL).
     */
    suspend fun loadThumbnail(url: String, maxSize: Int = 256): Bitmap? {
        val bytes = HttpClient.getBytes(url) ?: return null
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (bounds.outWidth / sample > maxSize * 2 && bounds.outHeight / sample > maxSize * 2) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: return@withContext null
            val scale = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
            if (scale >= 1f) {
                bitmap
            } else {
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                if (scaled !== bitmap) bitmap.recycle()
                scaled
            }
        }
    }

    /**
     * Center-crops the bitmap to a square and downscales to [maxSize].
     * Proportions are preserved — nothing is stretched.
     */
    private suspend fun toSquare(bytes: ByteArray, maxSize: Int): Bitmap? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
            val side = minOf(source.width, source.height)
            val x = (source.width - side) / 2
            val y = (source.height - side) / 2
            val cropped = Bitmap.createBitmap(source, x, y, side, side)
            if (side <= maxSize) {
                return@withContext cropped
            }
            val scaled = Bitmap.createScaledBitmap(cropped, maxSize, maxSize, true)
            if (scaled != cropped) cropped.recycle()
            scaled
        }

    private fun writePng(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun writeJpeg(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
        }
    }
}
