package ru.nekostul.horizonos.ui.settings.system

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.LanguageManager
import androidx.compose.foundation.layout.Column

@Composable
fun LanguageSettingsScreen(
    language: String,
    onLanguageSelected: (String) -> Unit,
    highlightedIndex: Int? = null
) {
    val effectiveLanguage = LanguageManager.effectiveLanguage(
        androidx.compose.ui.platform.LocalContext.current,
        language
    )
    Column {
        HorizonOverlayChoice(
            title = stringResource(R.string.language_russian),
            selected = (highlightedIndex ?: if (effectiveLanguage == LanguageManager.RUSSIAN) 0 else 1) == 0,
            onClick = { onLanguageSelected(LanguageManager.RUSSIAN) }
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.language_english),
            selected = (highlightedIndex ?: if (effectiveLanguage == LanguageManager.RUSSIAN) 0 else 1) == 1,
            onClick = { onLanguageSelected(LanguageManager.ENGLISH) }
        )
    }
}
