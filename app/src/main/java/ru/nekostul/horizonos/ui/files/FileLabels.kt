package ru.nekostul.horizonos.ui.files

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun FileEntry.sizeLabel(): String {
    if (isDirectory) return when (kind) {
        FileKind.FOLDER -> ""
        else -> ""
    }
    return formatSize(size)
}

fun formatSize(size: Long): String {
    if (size < 0) return "—"
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0) return "—"
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

fun isSystemRoot(path: String): Boolean =
    path == "/system" || path == "/vendor" || path == "/data" ||
        path == "/apex" || path == "/product" || path == "/system_ext" ||
        path.startsWith("/data/") || path == "/" && path != "/storage"
