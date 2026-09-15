package ru.nekostul.horizonos.ui.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileOpenHandler {

    private const val PROVIDER_AUTHORITY = "ru.nekostul.horizonos.fileprovider"

    fun open(context: Context, file: File, kind: FileKind): FileOpenResult {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file)
        } catch (_: Exception) {
            return FileOpenResult.Failed("Could not build a shareable URI")
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeOf(kind))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (kind == FileKind.APK) {
            return try {
                context.startActivity(intent)
                FileOpenResult.Success
            } catch (_: ActivityNotFoundException) {
                FileOpenResult.Failed("No package installer found")
            } catch (_: Exception) {
                FileOpenResult.Failed("Could not launch the package installer")
            }
        }

        return try {
            context.startActivity(intent)
            FileOpenResult.Success
        } catch (_: ActivityNotFoundException) {
            FileOpenResult.Failed("No application found to open this file")
        } catch (_: SecurityException) {
            FileOpenResult.Failed("Not allowed to open this file")
        } catch (_: Exception) {
            FileOpenResult.Failed("Could not open this file")
        }
    }

    fun mimeOf(kind: FileKind): String = when (kind) {
        FileKind.APK -> "application/vnd.android.package-archive"
        FileKind.IMAGE -> "image/*"
        FileKind.VIDEO -> "video/*"
        FileKind.AUDIO -> "audio/*"
        FileKind.TEXT -> "text/plain"
        FileKind.PDF -> "application/pdf"
        FileKind.RAR -> "application/x-rar-compressed"
        FileKind.ZIP -> "application/zip"
        FileKind.SEVEN_ZIP -> "application/x-7z-compressed"
        FileKind.TAR, FileKind.GZIP, FileKind.BZIP -> "application/gzip"
        FileKind.ISO -> "application/octet-stream"
        FileKind.ROM -> "application/octet-stream"
        FileKind.AZW -> "application/octet-stream"
        FileKind.DB -> "application/octet-stream"
        else -> "*/*"
    }
}

sealed interface FileOpenResult {
    data object Success : FileOpenResult
    data class Failed(val message: String) : FileOpenResult
}
