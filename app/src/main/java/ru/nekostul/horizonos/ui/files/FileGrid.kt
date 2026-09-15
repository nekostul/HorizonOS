package ru.nekostul.horizonos.ui.files

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.ui.settings.SettingsInputMode

@Composable
internal fun FileGridItem(
    entry: FileEntry,
    selected: Boolean,
    focused: Boolean,
    inputMode: SettingsInputMode,
    iconSize: Dp = 62.dp,
    onClick: () -> Unit,
    onFocus: () -> Unit = {}
) {
    val active = inputMode == SettingsInputMode.GAMEPAD && focused

    var preview by remember(entry.path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.path) {
        if (entry.kind == FileKind.IMAGE) {
            preview = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(entry.path)?.asImageBitmap()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    selected -> FileTheme.accent.copy(alpha = 0.26f)
                    active -> FileTheme.divider.copy(alpha = 0.45f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (active) Modifier.border(
                    width = 3.dp,
                    color = colorLerp(FileTheme.accent.copy(alpha = 0.35f), FileTheme.accent, 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable {
                onFocus()
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FileEntryIcon(entry = entry, preview = preview, iconSize = iconSize)
        Spacer(Modifier.height(8.dp))
        Text(
            text = entry.name,
            color = if (selected || active) FileTheme.text else FileTheme.muted,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FileEntryIcon(
    entry: FileEntry,
    preview: ImageBitmap?,
    iconSize: Dp = 62.dp
) {
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        when {
            preview != null -> Image(
                bitmap = preview,
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            entry.isDirectory -> FolderGlyph(
                modifier = Modifier.size(iconSize * 0.92f),
                color = FileTheme.accent
            )
            else -> FileSheetGlyph(
                modifier = Modifier.size(iconSize * 0.78f),
                color = FileTheme.muted,
                label = entry.extension.uppercase().take(4).ifBlank { "FILE" }
            )
        }
    }
}

@Composable
fun FolderGlyph(
    modifier: Modifier = Modifier,
    color: Color = FileTheme.accent
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.10f, h * 0.14f),
            size = Size(w * 0.80f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.06f, h * 0.28f),
            size = Size(w * 0.88f, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
        )
    }
}

@Composable
private fun FileSheetGlyph(
    modifier: Modifier = Modifier,
    color: Color,
    label: String
) {
    val foldColor = FileTheme.pathBar
    val labelColor = FileTheme.panel
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val fold = w * 0.30f
            val body = Path().apply {
                moveTo(w * 0.12f, h * 0.06f)
                lineTo(w - fold, h * 0.06f)
                lineTo(w - w * 0.10f, h * 0.06f + fold)
                lineTo(w - w * 0.10f, h * 0.94f)
                lineTo(w * 0.12f, h * 0.94f)
                close()
            }
            drawPath(path = body, color = color.copy(alpha = 0.9f))
            val corner = Path().apply {
                moveTo(w - fold, h * 0.06f)
                lineTo(w - w * 0.10f, h * 0.06f + fold)
                lineTo(w - fold, h * 0.06f + fold)
                close()
            }
            drawPath(path = corner, color = foldColor)
        }
        if (label.isNotBlank()) {
            Text(
                text = label,
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
