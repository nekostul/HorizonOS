package ru.nekostul.horizonos.ui.user

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.HorizonXboxGlyph
import ru.nekostul.horizonos.ui.files.FileEntry
import ru.nekostul.horizonos.ui.games.ImagePickerOverlay
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.settings.HorizonOverlay
import ru.nekostul.horizonos.ui.settings.HorizonOverlayTextField
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.SelectionFrameBlue
import ru.nekostul.horizonos.ui.settings.SelectionPulseDurationMillis
import ru.nekostul.horizonos.ui.settings.SettingsBackground
import ru.nekostul.horizonos.ui.settings.SettingsDivider
import ru.nekostul.horizonos.ui.settings.SettingsGray
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.SettingsPanel
import ru.nekostul.horizonos.ui.settings.SettingsWhite
import ru.nekostul.horizonos.ui.keyboard.HorizonKeyboardDialog
import java.io.File

private val OnlineGreen = Color(0xFF34C759)

private fun isOnline(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}.getOrDefault(false)

@Composable
fun UserPageScreen(
    repository: UserProfileRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by repository.profile.collectAsState()
    val inputMode = remember {
        mutableStateOf(
            if (ru.nekostul.horizonos.ui.isExternalGamepadConnected()) SettingsInputMode.GAMEPAD
            else SettingsInputMode.TOUCH
        )
    }
    val focusRequester = remember { FocusRequester() }

    var rightFocus by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(0) }
    var editingNick by remember { mutableStateOf(false) }
    var pickingAvatar by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(isOnline(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            online = isOnline(context)
            delay(3000)
        }
    }

    val nick = profile.nick.ifBlank { stringResource(R.string.user_page_title) }

    fun activateOption() {
        when (selectedOption) {
            0 -> editingNick = true
            1 -> pickingAvatar = true
        }
    }

    CompositionLocalProvider(LocalSettingsInputMode provides inputMode) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SettingsBackground)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    inputMode.value = SettingsInputMode.GAMEPAD
                    if (HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode)) {
                        HorizonNavigation.requestHome()
                        return@onPreviewKeyEvent true
                    }
                    when {
                        isHorizonConfirmKey(event) -> {
                            if (rightFocus) activateOption() else rightFocus = true
                            true
                        }
                        event.key == Key.DirectionUp -> {
                            if (rightFocus) selectedOption = (selectedOption - 1).coerceAtLeast(0)
                            true
                        }
                        event.key == Key.DirectionDown -> {
                            if (rightFocus) selectedOption = (selectedOption + 1).coerceAtMost(1)
                            true
                        }
                        event.key == Key.DirectionRight -> { rightFocus = true; true }
                        event.key == Key.DirectionLeft -> { rightFocus = false; true }
                        event.key == Key.ButtonB || event.key == Key.Back -> { onBack(); true }
                        else -> false
                    }
                }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(avatarPath = profile.avatarPath, size = 34.dp)
                    Spacer(Modifier.width(15.dp))
                    Text(
                        text = stringResource(R.string.user_page_title),
                        color = SettingsWhite,
                        fontSize = 25.sp
                    )
                }
                Spacer(Modifier.height(11.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        Modifier
                            .fillMaxHeight()
                            .weight(0.34f)
                            .background(SettingsPanel)
                            .padding(vertical = 6.dp)
                    ) {
                        UserNavRow(
                            text = stringResource(R.string.user_page_profile),
                            focused = !rightFocus && inputMode.value == SettingsInputMode.GAMEPAD
                        ) { rightFocus = false }
                    }
                    Spacer(Modifier.width(24.dp))

                    Row(
                        Modifier.fillMaxHeight().weight(0.66f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserFocusFrame(
                            active = rightFocus && selectedOption == 0 &&
                                inputMode.value == SettingsInputMode.GAMEPAD,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(nick, color = SettingsWhite, fontSize = 24.sp)
                                    Spacer(Modifier.width(10.dp))
                                    PencilButton(
                                        focused = rightFocus && selectedOption == 0,
                                        onClick = { selectedOption = 0; rightFocus = true; editingNick = true }
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = stringResource(
                                        if (online) R.string.user_page_online
                                        else R.string.user_page_offline
                                    ),
                                    color = if (online) OnlineGreen else SettingsGray,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(28.dp))
                        UserFocusFrame(
                            active = rightFocus && selectedOption == 1 &&
                                inputMode.value == SettingsInputMode.GAMEPAD
                        ) {
                            Box {
                                UserAvatar(
                                    avatarPath = profile.avatarPath,
                                    size = 170.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                PencilButton(
                                    focused = rightFocus && selectedOption == 1,
                                    onClick = { selectedOption = 1; rightFocus = true; pickingAvatar = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(SettingsGray))
                Row(
                    Modifier.fillMaxWidth().height(42.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserFooterButton(
                        glyph = { HorizonButtonGlyph("B", size = 28.dp) },
                        label = stringResource(R.string.settings_action_back),
                        onClick = { onBack() }
                    )
                    Spacer(Modifier.width(25.dp))
                    UserFooterButton(
                        glyph = { HorizonButtonGlyph("A", size = 28.dp) },
                        label = stringResource(R.string.settings_action_select),
                        onClick = { if (rightFocus) activateOption() else rightFocus = true }
                    )
                    Spacer(Modifier.width(25.dp))
                    UserFooterButton(
                        glyph = { HorizonXboxGlyph(size = 28.dp) },
                        label = stringResource(R.string.files_home_hint),
                        onClick = { HorizonNavigation.requestHome() }
                    )
                }
            }
        }

        if (editingNick) {
            NickEditorOverlay(
                current = nick,
                onSave = { value ->
                    repository.setNick(value)
                    editingNick = false
                },
                onDismiss = { editingNick = false }
            )
        }

        if (pickingAvatar) {
            ImagePickerOverlay(
                title = stringResource(R.string.user_page_avatar_pick),
                cropSquare = true,
                onPick = { entry: FileEntry ->
                    pickingAvatar = false
                    scope.launch {
                        val target = File(context.filesDir, "user_avatar.png")
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { File(entry.path).copyTo(target, overwrite = true) }.isSuccess
                        }
                        if (ok) repository.setAvatar(target.absolutePath)
                    }
                },
                onDismiss = { pickingAvatar = false }
            )
        }
    }
}

@Composable
private fun NickEditorOverlay(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
      var value by remember { mutableStateOf(current) }
      var showKeyboard by remember { mutableStateOf(false) }
      var autoOpenField by remember { mutableStateOf(true) }
      if (!showKeyboard) {
          HorizonOverlay(
              title = stringResource(R.string.user_page_edit_nick),
              onDismiss = onDismiss
          ) {
              Spacer(Modifier.height(8.dp))
              HorizonOverlayTextField(
                  value = value,
                  onValueChange = { value = it },
                  placeholder = stringResource(R.string.user_page_nick_hint),
                  selected = true,
                  autoEditOnSelection = autoOpenField,
                  onEdit = {
                      autoOpenField = false
                      showKeyboard = true
                  }
              )
              Spacer(Modifier.height(12.dp))
              ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice(
                  title = stringResource(R.string.settings_action_save),
                  selected = true,
                  enabled = value.isNotBlank(),
                  onClick = { onSave(value.trim()) }
              )
              ru.nekostul.horizonos.ui.settings.HorizonOverlayChoice(
                  title = stringResource(R.string.settings_action_cancel),
                  selected = false,
                  onClick = onDismiss
              )
          }
      }
      if (showKeyboard) {
          HorizonKeyboardDialog(
              title = stringResource(R.string.user_page_nick_prompt),
              initialValue = value,
              maxLength = 10,
              onConfirm = { value = it; showKeyboard = false },
              onCancel = {
                  autoOpenField = false
                  showKeyboard = false
              }
          )
      }
}

@Composable
private fun UserNavRow(text: String, focused: Boolean, onClick: () -> Unit) {
    val inputMode = LocalSettingsInputMode.current
    val pulse = rememberInfiniteTransition(label = "userNavPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "userNavPulseValue"
    )
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .then(
                    if (focused) Modifier.border(
                        width = 3.dp,
                        color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
                    ) else Modifier
                )
                .clickable {
                    inputMode?.value = SettingsInputMode.TOUCH
                    onClick()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(if (focused) 4.dp else 0.dp)
                    .height(38.dp)
                    .background(if (focused) ru.nekostul.horizonos.ui.settings.SettingsBlue else Color.Transparent)
            )
            Spacer(Modifier.width(if (focused) 13.dp else 17.dp))
            Text(text, color = SettingsWhite, fontSize = 18.sp)
        }
    }
}

@Composable
private fun UserFocusFrame(
    active: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "userOptionPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "userOptionPulseValue"
    )
    Box(
        modifier = modifier.then(
            if (active) Modifier.border(
                width = 3.dp,
                color = SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f)
            ) else Modifier
        ).padding(10.dp)
    ) {
        content()
    }
}

@Composable
private fun UserFooterButton(
    glyph: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    val inputMode = LocalSettingsInputMode.current
    Row(
        modifier = Modifier
            .height(44.dp)
            .clickable {
                inputMode?.value = SettingsInputMode.TOUCH
                onClick()
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        glyph()
        Spacer(Modifier.width(7.dp))
        Text(label, color = SettingsWhite, fontSize = 16.sp)
    }
}

@Composable
private fun PencilButton(
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "userPencilPulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SelectionPulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "userPencilPulseValue"
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SettingsPanel)
            .then(
                if (focused) Modifier.border(
                    3.dp,
                    SelectionFrameBlue.copy(alpha = 0.35f + pulseValue * 0.65f),
                    CircleShape
                ) else Modifier.border(1.dp, SettingsDivider, CircleShape)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        PencilGlyph(color = SettingsWhite, size = 18.dp)
    }
}

@Composable
private fun PencilGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.16f
        drawLine(
            color = color,
            start = Offset(w * 0.20f, h * 0.80f),
            end = Offset(w * 0.74f, h * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val tip = Path().apply {
            moveTo(w * 0.14f, h * 0.86f)
            lineTo(w * 0.30f, h * 0.82f)
            lineTo(w * 0.18f, h * 0.70f)
            close()
        }
        drawPath(path = tip, color = color)
    }
}
