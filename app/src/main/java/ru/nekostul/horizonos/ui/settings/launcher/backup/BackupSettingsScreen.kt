package ru.nekostul.horizonos.ui.settings.launcher.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice
import ru.nekostul.horizonos.ui.settings.HorizonSettingRow
import ru.nekostul.horizonos.ui.settings.SettingRow
import ru.nekostul.horizonos.ui.settings.SettingsAccentTeal
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsWhite

private enum class BackupMode { CREATE, RESTORE }

@Composable
fun BackupSettingsScreen(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    openOverlayIndex: Int? = null,
    onOverlayRequestConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCreate by remember { mutableStateOf(false) }
    var showRestore by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(BackupSection.entries.toSet()) }
    var resultMessage by remember { mutableStateOf("") }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                resultMessage = if (BackupManager.createBackup(context, uri, checked)) {
                    context.getString(R.string.backup_created)
                } else {
                    context.getString(R.string.backup_failed)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                resultMessage = if (BackupManager.restoreBackup(context, uri, checked)) {
                    context.getString(R.string.backup_restored)
                } else {
                    context.getString(R.string.backup_failed)
                }
            }
        }
    }

    LaunchedEffect(openOverlayIndex) {
        openOverlayIndex?.let {
            when (it) {
                0 -> showCreate = true
                1 -> showRestore = true
            }
            onOverlayRequestConsumed()
        }
    }

    Column {
        Text(stringResource(R.string.settings_category_backup), color = SettingsWhite, fontSize = 25.sp)
        Spacer(Modifier.height(12.dp))
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.backup_create),
                description = stringResource(R.string.backup_create_description)
            ),
            selected = selectedIndex == 0,
            onClick = {
                onSelect(0)
                showCreate = true
            }
        )
        HorizonSettingRow(
            SettingRow(
                title = stringResource(R.string.backup_restore),
                description = stringResource(R.string.backup_restore_description)
            ),
            selected = selectedIndex == 1,
            onClick = {
                onSelect(1)
                showRestore = true
            }
        )
    }

    if (showCreate) {
        BackupSelectionOverlay(
            mode = BackupMode.CREATE,
            checked = checked,
            onCheckedChange = { checked = it },
            resultMessage = resultMessage,
            onLaunchPicker = {
                if (checked.isEmpty()) {
                    resultMessage = context.getString(R.string.backup_nothing_selected)
                } else {
                    resultMessage = ""
                    createLauncher.launch(
                        "HorizonOS_" + java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                            .format(java.util.Date()) + ".hos"
                    )
                }
            },
            onDismiss = { showCreate = false }
        )
    }
    if (showRestore) {
        BackupSelectionOverlay(
            mode = BackupMode.RESTORE,
            checked = checked,
            onCheckedChange = { checked = it },
            resultMessage = resultMessage,
            onLaunchPicker = {
                if (checked.isEmpty()) {
                    resultMessage = context.getString(R.string.backup_nothing_selected)
                } else {
                    resultMessage = ""
                    restoreLauncher.launch(
                        arrayOf("application/octet-stream", "application/json", "text/plain", "*/*")
                    )
                }
            },
            onDismiss = { showRestore = false }
        )
    }
}

@Composable
private fun BackupSelectionOverlay(
    mode: BackupMode,
    checked: Set<BackupSection>,
    onCheckedChange: (Set<BackupSection>) -> Unit,
    resultMessage: String,
    onLaunchPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val sections = remember {
        listOf(
            BackupSection.HOME_SCREEN,
            BackupSection.SETTINGS,
            BackupSection.API_KEYS,
            BackupSection.PROFILE,
            BackupSection.GAME_FOLDERS
        )
    }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val actionIndex = sections.size
    val isCreate = mode == BackupMode.CREATE

    fun toggle(index: Int) {
        val section = sections[index]
        onCheckedChange(if (section in checked) checked - section else checked + section)
    }

    HorizonOverlay(
        title = stringResource(if (isCreate) R.string.backup_create else R.string.backup_restore),
        onDismiss = onDismiss,
        onDirectionalKey = { key ->
            when (key) {
                Key.DirectionDown, Key.DirectionRight -> {
                    selectedIndex = (selectedIndex + 1).coerceAtMost(actionIndex)
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
        Text(
            stringResource(R.string.backup_select_hint),
            color = SettingsGray,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(6.dp))

        sections.forEachIndexed { index, section ->
            HorizonOverlayChoice(
                title = sectionTitle(section),
                value = if (section in checked) "✓" else "",
                selected = selectedIndex == index,
                onClick = { toggle(index) }
            )
            if (section == BackupSection.API_KEYS) {
                Text(
                    stringResource(R.string.backup_api_keys_warning),
                    color = SettingsGray,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizonOverlayChoice(
            title = stringResource(if (isCreate) R.string.backup_action_create else R.string.backup_action_restore),
            selected = selectedIndex == actionIndex,
            onClick = onLaunchPicker
        )

        if (resultMessage.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                resultMessage,
                color = SettingsAccentTeal,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun sectionTitle(section: BackupSection): String = when (section) {
    BackupSection.HOME_SCREEN -> stringResource(R.string.backup_section_home_screen)
    BackupSection.SETTINGS -> stringResource(R.string.backup_section_settings)
    BackupSection.API_KEYS -> stringResource(R.string.backup_section_api_keys)
    BackupSection.PROFILE -> stringResource(R.string.backup_section_profile)
    BackupSection.GAME_FOLDERS -> stringResource(R.string.backup_section_game_folders)
}
