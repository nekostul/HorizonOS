package ru.nekostul.horizonos.ui.files

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsInputMode

/** Small circular icon button in the top toolbar. */
@Composable
fun HorizonFilesIconButton(
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, FileTheme.divider, RoundedCornerShape(8.dp))
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = FileTheme.accent, fontSize = 17.sp)
    }
}

/** Label button in the bottom action bar. */
@Composable
fun HorizonFilesTextButton(
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, FileTheme.divider, RoundedCornerShape(6.dp))
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = FileTheme.accent, fontSize = 14.sp)
    }
}

/** Sorting options overlay. */
@Composable
fun SortOverlay(
    current: FileSortMode,
    onSelect: (FileSortMode) -> Unit,
    onDismiss: () -> Unit
) {
    HorizonOverlay(title = "Сортировка", onDismiss = onDismiss) {
        FileSortMode.values().forEach { mode ->
            HorizonOverlayChoice(
                title = when (mode) {
                    FileSortMode.NAME -> "По имени"
                    FileSortMode.SIZE -> "По размеру"
                    FileSortMode.DATE -> "По дате"
                },
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
        }
    }
}

/** New-item menu overlay (folder or file). */
@Composable
fun NewItemOverlay(
    title: String,
    onFolder: () -> Unit,
    onFile: () -> Unit,
    onCancel: () -> Unit
) {
    HorizonOverlay(title = title, onDismiss = onCancel) {
        HorizonOverlayChoice(title = "Папку", selected = true, onClick = onFolder)
        HorizonOverlayChoice(title = "Файл", selected = false, onClick = onFile)
        HorizonOverlayChoice(title = "Отмена", selected = false, onClick = onCancel)
    }
}

/** Text input overlay used for renaming and creating items. */
@Composable
fun TextInputOverlay(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    HorizonOverlay(title = title, onDismiss = onCancel) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = FileTheme.text, fontSize = 16.sp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .border(1.dp, FileTheme.divider, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizonOverlayChoice(
            title = "OK",
            selected = true,
            enabled = value.isNotBlank(),
            onClick = { onConfirm(value.trim()) }
        )
        HorizonOverlayChoice(title = "Отмена", selected = false, onClick = onCancel)
    }
}
