package ru.nekostul.horizonos.ui.files

import java.io.File

enum class FileKind {
    FOLDER, APK, AZW, RAR, ZIP, SEVEN_ZIP, TAR, GZIP, BZIP, ISO, BIN_CUE,
    CHD, ROM, IMAGE, VIDEO, AUDIO, TEXT, PDF, DB, CONFIG, UNKNOWN
}

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = -1L,
    val lastModified: Long = 0L,
    val hidden: Boolean = false,
    val executable: Boolean = false
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val kind: FileKind
        get() = if (isDirectory) FileKind.FOLDER else FileTypeDetector.detect(extension)

    val isRoot: Boolean get() = path == "/"
}

fun File.toEntry(): FileEntry {
    val nm = name ?: path
    return FileEntry(
        name = nm,
        path = absolutePath,
        isDirectory = isDirectory,
        size = if (isFile) length() else -1L,
        lastModified = lastModified(),
        hidden = nm.startsWith('.'),
        executable = canExecute()
    )
}

object FileTypeDetector {
    fun detect(ext: String): FileKind = when (ext.trim().lowercase()) {
        "apk" -> FileKind.APK
        "azw", "azw3", "azw4", "epub", "mobi" -> FileKind.AZW
        "rar" -> FileKind.RAR
        "zip", "jar", "apks", "xapk" -> FileKind.ZIP
        "7z" -> FileKind.SEVEN_ZIP
        "tar", "tar.gz" -> FileKind.TAR
        "gz", "tgz" -> FileKind.GZIP
        "bz2", "tbz" -> FileKind.BZIP
        "iso" -> FileKind.ISO
        "bin", "cue" -> FileKind.BIN_CUE
        "chd" -> FileKind.CHD
        "gb", "gba", "gbc", "nds", "3ds", "nes", "snes", "smc", "sfc", "n64",
        "z64", "v64", "pce", "gba", "sfc", "n64" -> FileKind.ROM
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg", "tiff" -> FileKind.IMAGE
        "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "m4v", "ts", "m2ts" -> FileKind.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus" -> FileKind.AUDIO
        "txt", "log", "md", "nfo", "srt", "ass" -> FileKind.TEXT
        "pdf" -> FileKind.PDF
        "db", "sqlite", "sqlite3" -> FileKind.DB
        "ini", "cfg", "conf", "xml", "json", "yml", "yaml", "properties", "sh" -> FileKind.CONFIG
        else -> FileKind.UNKNOWN
    }
}
