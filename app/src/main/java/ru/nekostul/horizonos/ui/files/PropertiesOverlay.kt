package ru.nekostul.horizonos.ui.files

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice

/** Shows properties (name, type, size, path, modified, etc.) for an entry. */
@Composable
fun PropertiesOverlay(
    entry: FileEntry,
    size: Long,
    count: Int,
    onDismiss: () -> Unit
) {
    HorizonOverlay(title = "Свойства", onDismiss = onDismiss) {
        PropRow("Имя", entry.name)
        PropRow("Тип", if (entry.isDirectory) "Папка" else entry.extension.uppercase().takeIf { it.isNotEmpty() } ?: "Файл")
        Spacer(Modifier.height(4.dp))
        if (entry.isDirectory) {
            PropRow("Элементов", if (count >= 0) "$count" else "…")
            PropRow("Размер", if (size >= 0) formatSize(size) else "…")
        } else {
            PropRow("Размер", formatSize(entry.size))
        }
        Spacer(Modifier.height(4.dp))
        PropRow("Путь", entry.path)
        Spacer(Modifier.height(4.dp))
        PropRow("Изменён", formatDate(entry.lastModified))
        Spacer(Modifier.height(12.dp))
        HorizonOverlayChoice(title = "OK", selected = true, onClick = onDismiss)
    }
}

@Composable
private fun PropRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Text(label, color = FileTheme.muted, fontSize = 13.sp, modifier = Modifier.weight(0.4f))
        Text(value, color = FileTheme.text, fontSize = 13.sp, modifier = Modifier.weight(0.6f))
    }
}
