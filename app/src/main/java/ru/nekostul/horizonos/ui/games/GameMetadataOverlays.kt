package ru.nekostul.horizonos.ui.games

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonOverlayTextField
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SelectionFrameBlue
import ru.nekostul.horizonos.ui.settings.SelectionPulseDurationMillis
import ru.nekostul.horizonos.ui.settings.SettingsBlue
import ru.nekostul.horizonos.ui.settings.SettingsDivider
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsOverlayPanel
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.settings.launcher.scanning.GameMetadataEditor
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaType
import ru.nekostul.horizonos.ui.settings.launcher.scanning.MediaVariant
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId

private const val MAX_VARIANTS = 24

/** Manual title override dialog. The ROM file is never modified. */
@Composable
fun GameTitleEditorOverlay(
    game: Game,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(game.displayTitle) }
    HorizonOverlay(
        title = stringResource(R.string.games_edit_title),
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(R.string.games_title_current, game.displayTitle),
            color = SettingsGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizonOverlayTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = stringResource(R.string.games_title_hint),
            selected = true
        )
        Spacer(Modifier.height(12.dp))
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_save),
            selected = true,
            enabled = value.isNotBlank(),
            onClick = { onSave(value.trim()) }
        )
        HorizonOverlayChoice(
            title = stringResource(R.string.settings_action_cancel),
            selected = false,
            onClick = onDismiss
        )
    }
}

/** Cover / screenshot picker. Shows previews of every available variant. */
@Composable
fun GameMediaPickerOverlay(
    game: Game,
    type: MediaType,
    editor: GameMetadataEditor,
    settings: ScraperSettings,
    onSelect: (MediaVariant) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<LoadedVariant>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(game.id, type) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            editor.loadVariants(settings, game, type)
                .take(MAX_VARIANTS)
                .mapNotNull { variant ->
                    editor.loadThumbnail(variant.url)?.let { LoadedVariant(variant, it) }
                }
        }
        items = result
        selectedIndex = 0
        loading = false
    }

    val title = if (type == MediaType.COVER) {
        stringResource(R.string.games_pick_cover)
    } else {
        stringResource(R.string.games_pick_screenshot)
    }

    HorizonOverlay(
        title = title,
        onDismiss = onDismiss,
        onDirectionalKey = { key ->
            when (key) {
                Key.DirectionDown, Key.DirectionRight -> {
                    if (items.isNotEmpty()) selectedIndex = (selectedIndex + 1).coerceAtMost(items.lastIndex)
                    true
                }
                Key.DirectionUp, Key.DirectionLeft -> {
                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                    true
                }
                else -> false
            }
        }
    ) {
        when {
            loading -> Text(
                text = stringResource(R.string.games_variants_loading),
                color = SettingsGray,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            items.isEmpty() -> Text(
                text = stringResource(R.string.games_variants_empty),
                color = SettingsGray,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            else -> items.forEachIndexed { index, loaded ->
                MediaVariantRow(
                    loaded = loaded,
                    isCover = type == MediaType.COVER,
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index; onSelect(loaded.variant) }
                )
            }
        }
    }
}

private data class LoadedVariant(val variant: MediaVariant, val bitmap: Bitmap)

@Composable
private fun MediaVariantRow(
    loaded: LoadedVariant,
    isCover: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val pulse = rememberInfiniteTransition(label = "mediaVariantPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mediaVariantPulseValue"
    )
    val active = inputMode?.value == SettingsInputMode.GAMEPAD && selected
    LaunchedEffect(hasFocus) {
        if (hasFocus) bringIntoViewRequester.bringIntoView()
    }
    LaunchedEffect(inputMode?.value, selected) {
        if (inputMode?.value == SettingsInputMode.GAMEPAD && selected) {
            focusRequester.requestFocus()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) SettingsDivider.copy(alpha = 0.42f) else Color.Transparent)
            .then(
                if (active) {
                    Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f),
                        shape = RoundedCornerShape(6.dp)
                    )
                } else Modifier
            )
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .onKeyEvent { event ->
                if (isHorizonConfirmKey(event)) {
                    inputMode?.value = SettingsInputMode.GAMEPAD
                    onClick()
                    true
                } else false
            }
            .onFocusChanged { hasFocus = it.hasFocus }
            .bringIntoViewRequester(bringIntoViewRequester)
            .focusRequester(focusRequester)
            .focusable()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCover) 64.dp else 96.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SettingsOverlayPanel),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = loaded.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = if (isCover) ContentScale.Crop else ContentScale.Fit,
                modifier = if (isCover) Modifier.size(64.dp) else Modifier.fillMaxWidth().height(54.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = sourceLabel(loaded.variant.source),
                color = SettingsWhite,
                fontSize = 15.sp
            )
            val details = buildList {
                if (loaded.variant.isSquare) add(stringResource(R.string.games_variant_square))
                if (loaded.variant.hasDimensions) {
                    add("${loaded.variant.width}×${loaded.variant.height}")
                }
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    color = SettingsGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun sourceLabel(source: ScraperSourceId): String = when (source) {
    ScraperSourceId.SCREEN_SCRAPER -> stringResource(R.string.settings_scraper_screen_scraper)
    ScraperSourceId.LIBRETRO -> stringResource(R.string.settings_scraper_libretro)
    ScraperSourceId.THE_GAMES_DB -> stringResource(R.string.settings_scraper_thegamesdb)
    ScraperSourceId.IGDB -> stringResource(R.string.settings_scraper_igdb)
    ScraperSourceId.STEAM_GRID_DB -> stringResource(R.string.settings_scraper_steam_grid_db)
}
