package ru.nekostul.horizonos.ui.files

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp as colorLerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A single selectable row in the file list with an icon and details. */
@Composable
internal fun FileRow(
    entry: FileEntry,
    selected: Boolean,
    focused: Boolean,
    inputMode: SettingsInputMode,
    subtitle: String?,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val active = inputMode == SettingsInputMode.GAMEPAD && focused

    var preview by remember(entry.path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(entry.path) {
        if (entry.kind == FileKind.IMAGE) {
            val bmp = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(entry.path)?.asImageBitmap()
            }
            preview = bmp
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    selected -> FileTheme.accent.copy(alpha = 0.28f)
                    active -> FileTheme.divider.copy(alpha = 0.5f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(6.dp)
            )
            .then(
                if (active) Modifier.border(
                    width = 3.dp,
                    color = colorLerp(FileTheme.accent.copy(alpha = 0.35f), FileTheme.accent, 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) else Modifier
            )
            .clickable {
                onFocus()
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileEntryIcon(entry = entry, preview = preview, iconSize = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.name,
                color = FileTheme.text,
                fontSize = 15.sp,
                maxLines = 1
            )
            if (subtitle != null && subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = FileTheme.muted, fontSize = 12.sp, maxLines = 1)
            }
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            val checkColor = FileTheme.accent
            Canvas(Modifier.size(20.dp)) {
                val stroke = 2.dp.toPx()
                drawLine(
                    color = checkColor,
                    start = Offset(size.width * 0.20f, size.height * 0.52f),
                    end = Offset(size.width * 0.42f, size.height * 0.72f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = checkColor,
                    start = Offset(size.width * 0.42f, size.height * 0.72f),
                    end = Offset(size.width * 0.80f, size.height * 0.26f),
                    strokeWidth = stroke
                )
            }
        }
    }
}
