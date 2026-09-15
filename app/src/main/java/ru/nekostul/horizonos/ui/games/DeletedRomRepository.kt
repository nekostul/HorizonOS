package ru.nekostul.horizonos.ui.games

import android.content.Context

class DeletedRomRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs
        get() = appContext.getSharedPreferences("deleted_roms", Context.MODE_PRIVATE)

    fun all(): Set<String> = prefs.getStringSet(KEY, emptySet())?.toSet().orEmpty()

    fun isDeleted(romUri: String): Boolean = romUri in all()

    fun markDeleted(romUri: String) {
        if (romUri.isBlank()) return
        prefs.edit().putStringSet(KEY, all() + romUri).apply()
    }

    fun clearDeleted(romUri: String) {
        if (romUri.isBlank()) return
        prefs.edit().putStringSet(KEY, all() - romUri).apply()
    }

    private companion object {
        const val KEY = "rom_uris"
    }
}
