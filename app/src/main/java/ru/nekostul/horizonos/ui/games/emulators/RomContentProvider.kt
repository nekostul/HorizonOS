package ru.nekostul.horizonos.ui.games.emulators

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.util.Base64
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

class RomContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = fileForUri(uri)?.let { MIME_TYPE }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val file = fileForUri(uri)?.takeIf { it.isFile && it.canRead() } ?: return null
        val columns = projection ?: DEFAULT_COLUMNS
        val cursor = MatrixCursor(columns)
        cursor.addRow(columns.map { column ->
            when (column) {
                Document.COLUMN_DISPLAY_NAME -> file.name
                Document.COLUMN_MIME_TYPE -> MIME_TYPE
                Document.COLUMN_SIZE -> file.length()
                else -> null
            }
        }.toTypedArray())
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") {
            throw FileNotFoundException("Read-only ROM provider")
        }
        val file = fileForUri(uri)?.takeIf { it.isFile && it.canRead() }
            ?: throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openFile(
        uri: Uri,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor = openFile(uri, mode)

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("ROM provider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("ROM provider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("ROM provider is read-only")

    private fun fileForUri(uri: Uri): File? {
        if (uri.authority != AUTHORITY || uri.pathSegments.size < 2 ||
            uri.pathSegments[0] != PATH_PREFIX
        ) {
            return null
        }
        return runCatching {
            val encoded = uri.pathSegments[1]
            val padding = "=".repeat((4 - encoded.length % 4) % 4)
            val bytes = Base64.decode(encoded + padding, Base64.URL_SAFE or Base64.NO_WRAP)
            val baseFile = File(String(bytes, StandardCharsets.UTF_8)).canonicalFile
            if (uri.pathSegments.size == 2) return@runCatching baseFile

            // DuckStation resolves relative M3U entries by appending them to
            // the playlist URI. Resolve those virtual path segments beside
            // the playlist's real file.
            val parent = baseFile.parentFile?.canonicalFile ?: return@runCatching null
            val relative = uri.pathSegments.drop(2).joinToString(File.separator)
            val child = File(parent, relative).canonicalFile
            val parentPath = parent.path + File.separator
            child.takeIf { it.path.startsWith(parentPath) }
        }.getOrNull()
    }

    companion object {
        const val AUTHORITY = "ru.nekostul.horizonos.romprovider"
        private const val PATH_PREFIX = "rom"
        private const val MIME_TYPE = "application/octet-stream"
        private val DEFAULT_COLUMNS = arrayOf(
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE
        )

        fun uriForFile(file: File): Uri {
            val encoded = Base64.encodeToString(
                file.absolutePath.toByteArray(StandardCharsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
            return Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .appendPath(PATH_PREFIX)
                .appendPath(encoded)
                .build()
        }
    }
}
