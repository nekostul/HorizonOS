package ru.nekostul.horizonos.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

/**
 * Theme accessors for the file manager, derived from the global HorizonOS
 * theme so it always matches the launcher colors.
 */
internal object FileTheme {
    val background: Color
        @Composable get() = LocalHorizonColors.current.background
    val text: Color
        @Composable get() = LocalHorizonColors.current.text
    val muted: Color
        @Composable get() = LocalHorizonColors.current.mutedText
    val accent: Color
        @Composable get() = LocalHorizonColors.current.accent
    val divider: Color
        @Composable get() = LocalHorizonColors.current.divider
    val panel: Color
        @Composable get() = LocalHorizonColors.current.panel
    val pathBar: Color
        @Composable get() = LocalHorizonColors.current.selected
}
