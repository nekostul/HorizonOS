package ru.nekostul.horizonos.ui.files

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.hideDialogSystemBars

/**
 * Full-screen ROM folder picker built on the internal HorizonOS file manager.
 * Used by the add-games flow instead of the native Android document picker.
 */
@Composable
fun FolderPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            if (window == null) {
                onDispose { }
            } else {
                window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                window.setDimAmount(0f)
                window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
                hideDialogSystemBars(window)
                onDispose { }
            }
        }
        FilesScreen(
            onDismiss = onDismiss,
            mode = FilesMode.PICK_FOLDER,
            onFolderPicked = onPick,
            titleOverride = stringResource(R.string.files_pick_folder)
        )
    }
}
