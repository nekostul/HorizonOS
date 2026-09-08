package ru.nekostul.horizonos.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val RUSSIAN = "ru"

    fun effectiveLanguage(context: Context, preference: String): String {
        if (preference == ENGLISH || preference == RUSSIAN) return preference
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION") context.resources.configuration.locale
        }
        return if (locale.language.equals(RUSSIAN, ignoreCase = true)) RUSSIAN else ENGLISH
    }

    fun localizedContext(context: Context, preference: String): Context {
        val language = effectiveLanguage(context, preference)
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }
}
