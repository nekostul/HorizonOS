package ru.nekostul.horizonos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

data class HorizonColorPalette(
    val background: Color,
    val panel: Color,
    val selected: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val divider: Color,
    val card: Color,
    val overlayPanel: Color
)

val DarkHorizonColors = HorizonColorPalette(
    background = Color(0xFF2B2B2B),
    panel = Color(0xFF303030),
    selected = Color(0xFF3A3A3A),
    text = Color(0xFFF2F2F2),
    mutedText = Color(0xFFAAAAAA),
    accent = Color(0xFF00E8C8),
    divider = Color(0xFF858585),
    card = Color(0xFF333333),
    overlayPanel = Color(0xFF464646)
)

val LightHorizonColors = HorizonColorPalette(
    background = Color(0xFFF4F4F4),
    panel = Color(0xFFFFFFFF),
    selected = Color(0xFFE5E5E5),
    text = Color(0xFF222222),
    mutedText = Color(0xFF666666),
    accent = Color(0xFF00A98F),
    divider = Color(0xFF9B9B9B),
    card = Color(0xFFE8E8E8),
    overlayPanel = Color(0xFFFFFFFF)
)

val LocalHorizonColors = staticCompositionLocalOf { DarkHorizonColors }

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

)

@Composable
fun HorizonOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val horizonColors = if (darkTheme) DarkHorizonColors else LightHorizonColors

    CompositionLocalProvider(LocalHorizonColors provides horizonColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
