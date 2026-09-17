package ru.nekostul.horizonos.ui.games

import ru.nekostul.horizonos.ui.files.RootHelper

class VitaGameScanner {

    fun scan(emulator: Emulator): List<Game> {
        if (emulator.platform != Platform.PSVITA) return emptyList()
        val appDirs = locateAppDirs(emulator)
        val games = mutableListOf<Game>()
        val seenTitleIds = mutableSetOf<String>()

        appDirs.forEach { appDir ->
            RootHelper.listRoot(appDir)
                .filter { it.isDirectory }
                .forEach { entry ->
                    val titleId = entry.name
                    if (titleId.isBlank() || titleId in seenTitleIds) return@forEach
                    seenTitleIds += titleId
                    val title = RootHelper.readRootBytes("$appDir/$titleId/sce_sys/param.sfo")
                        ?.let { ParamSfo.readTitle(it) }
                        ?.takeIf { it.isNotBlank() }
                        ?: titleId
                    games += Game.fromRom(
                        title = title,
                        platform = Platform.PSVITA,
                        emulator = emulator,
                        romUri = titleId,
                        romName = title
                    )
                }
        }
        return games
    }

    private fun locateAppDirs(emulator: Emulator): List<String> {
        val packageName = packageName(emulator)
        val relativePaths = relativeAppPaths(emulator)
        val candidates = mutableListOf<String>()

        relativePaths.forEach { relative ->
            candidates += "/data/data/$packageName/$relative"
            candidates += "/storage/emulated/0/Android/data/$packageName/$relative"
        }

        val volumes = RootHelper.listRoot("/storage").filter { it.isDirectory }
        volumes.forEach { volume ->
            if (volume.name == "emulated" || volume.name == "self") return@forEach
            relativePaths.forEach { relative ->
                candidates += "/storage/${volume.name}/Android/data/$packageName/$relative"
            }
        }

        return candidates.distinct().filter { dir ->
            RootHelper.runRoot("test -d \"$dir\"").first == 0
        }
    }

    private fun packageName(emulator: Emulator): String = when (emulator) {
        Emulator.VITA3K -> "org.vita3k.emulator"
        Emulator.EMUCOREV -> "com.sbro.emucorev"
        else -> ""
    }

    private fun relativeAppPaths(emulator: Emulator): List<String> = when (emulator) {
        Emulator.VITA3K -> listOf("files/ux0/app", "files/vita/ux0/app")
        Emulator.EMUCOREV -> listOf("files/vita/ux0/app")
        else -> emptyList()
    }
}

private object ParamSfo {
    private val MAGIC = byteArrayOf(0x00, 0x50.toByte(), 0x53.toByte(), 0x46.toByte())

    fun readTitle(bytes: ByteArray): String? {
        if (bytes.size < 20) return null
        for (i in 0 until 4) {
            if (bytes[i] != MAGIC[i]) return null
        }
        val keyTableOffset = readIntLE(bytes, 8)
        val dataTableOffset = readIntLE(bytes, 12)
        val count = readIntLE(bytes, 16)
        if (count <= 0 || count > 256) return null

        for (index in 0 until count) {
            val entry = 20 + index * 16
            if (entry + 16 > bytes.size) return null
            val keyOffset = readUShortLE(bytes, entry)
            val format = readUShortLE(bytes, entry + 2)
            val length = readIntLE(bytes, entry + 4)
            val dataOffset = readIntLE(bytes, entry + 12)

            val key = readCString(bytes, keyTableOffset + keyOffset)
            if (key != "TITLE") continue

            val start = dataTableOffset + dataOffset
            if (start < 0 || start + length > bytes.size) return null
            val data = bytes.copyOfRange(start, start + length)
            return when (format) {
                0x0404, 0x0204 -> data.toString(Charsets.UTF_8).trimEnd('\u0000')
                0x0004 -> data.toString(Charsets.UTF_8).trimEnd('\u0000')
                else -> String(data).trimEnd('\u0000')
            }
        }
        return null
    }

    private fun readIntLE(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readUShortLE(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 2 > bytes.size) return 0
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readCString(bytes: ByteArray, offset: Int): String {
        if (offset < 0 || offset >= bytes.size) return ""
        var end = offset
        while (end < bytes.size && bytes[end] != 0.toByte()) end++
        return bytes.copyOfRange(offset, end).toString(Charsets.UTF_8)
    }
}
