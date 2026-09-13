package ru.nekostul.horizonos.ui.games

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Remembers the ROM folders the user added, together with the platform and
 * emulator they were added for. Used both to reject duplicate folders and to
 * silently rescan for newly added games on every launch.
 */
class GameFolderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val key = "folders_json"

    data class Folder(
        val path: String,
        val name: String,
        val platform: Platform,
        val emulator: Emulator
    )

    private val prefs
        get() = appContext.getSharedPreferences("game_folders", Context.MODE_PRIVATE)

    fun load(): List<Folder> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val platform = item.optString("platform")
                        .let { value -> runCatching { Platform.valueOf(value) }.getOrNull() } ?: continue
                    val emulator = item.optString("emulator")
                        .let { value -> runCatching { Emulator.valueOf(value) }.getOrNull() } ?: continue
                    val path = item.optString("path").takeIf { it.isNotBlank() } ?: continue
                    add(Folder(path, item.optString("name").ifBlank { path }, platform, emulator))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun remember(folder: Folder) {
        val current = load()
        if (current.any { it.path == folder.path }) return
        save(current + folder)
    }

    fun clear() = prefs.edit().remove(key).apply()

    private fun save(folders: List<Folder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject().apply {
                    put("path", folder.path)
                    put("name", folder.name)
                    put("platform", folder.platform.name)
                    put("emulator", folder.emulator.name)
                }
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }
}
