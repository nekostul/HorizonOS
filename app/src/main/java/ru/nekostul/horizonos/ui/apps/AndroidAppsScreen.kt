package ru.nekostul.horizonos.ui.apps

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound
import ru.nekostul.horizonos.ui.games.AndroidAppRepository
import ru.nekostul.horizonos.ui.games.InstalledAppInfo
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

@Composable
fun AndroidAppsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val palette = LocalHorizonColors.current
    val repository = remember { AndroidAppRepository(context) }
    val focusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var focusedIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { repository.launchableApps() }
        loading = false
    }

    LaunchedEffect(loading, apps.isNotEmpty()) {
        if (!loading && apps.isNotEmpty()) {
            kotlinx.coroutines.delay(40)
            focusRequester.requestFocus()
        }
    }

    fun launchApp(app: InstalledAppInfo, source: LauncherInputSource) {
        val intent = runCatching {
            context.packageManager.getLaunchIntentForPackage(app.packageName)
        }.getOrNull()
        if (intent == null) {
            LauncherAudioManager.playGameError(source)
            return
        }
        LauncherAudioManager.playOpenGame(source)
        LauncherAudioManager.performHapticFeedback(view)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    BackHandler(onBack = {
        LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.GAMEPAD)
        onDismiss()
    })

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        val cellMinWidth = 92.dp
        val columns = (maxWidth / cellMinWidth).toInt().coerceIn(3, 6)

        fun moveFocus(delta: Int) {
            if (apps.isEmpty()) return
            focusedIndex = (focusedIndex + delta).coerceIn(0, apps.lastIndex)
            LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        }

        fun moveGrid(deltaColumns: Int, deltaRows: Int) {
            if (apps.isEmpty()) return
            val next = focusedIndex + deltaColumns + deltaRows * columns
            if (next < 0 || next > apps.lastIndex) return
            focusedIndex = next
            LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        }

        LaunchedEffect(focusedIndex) {
            if (apps.isNotEmpty()) {
                gridState.animateScrollToItem(focusedIndex.coerceIn(0, apps.lastIndex))
            }
        }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.apps_title),
                    color = palette.text,
                    fontSize = 25.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (loading) "" else apps.size.toString(),
                    color = palette.mutedText,
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(10.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.apps_loading),
                        color = palette.mutedText,
                        fontSize = 15.sp
                    )
                }
            } else if (apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.apps_empty),
                        color = palette.mutedText,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .focusable()
                        .onFocusChanged { focusState ->
                            if (!focusState.hasFocus) {
                                scope.launch {
                                    kotlinx.coroutines.delay(60)
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when {
                                isHorizonConfirmKey(event) -> {
                                    apps.getOrNull(focusedIndex)?.let {
                                        launchApp(it, LauncherInputSource.GAMEPAD)
                                    }
                                    true
                                }
                                event.key == Key.DirectionLeft -> {
                                    moveFocus(-1); true
                                }
                                event.key == Key.DirectionRight -> {
                                    moveFocus(1); true
                                }
                                event.key == Key.DirectionUp -> {
                                    moveGrid(0, -1); true
                                }
                                event.key == Key.DirectionDown -> {
                                    moveGrid(0, 1); true
                                }
                                event.key == Key.ButtonB || event.key == Key.Back -> {
                                    LauncherAudioManager.play(
                                        LauncherSound.BACK,
                                        LauncherInputSource.GAMEPAD
                                    )
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        AppGridTile(
                            app = app,
                            focused = focusedIndex == index,
                            onClick = {
                                launchApp(app, LauncherInputSource.TOUCH)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                AppFooterAction(
                    label = "B",
                    text = stringResource(R.string.settings_action_back),
                    onClick = {
                        LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.TOUCH)
                        onDismiss()
                    }
                )
                Spacer(Modifier.width(16.dp))
                AppFooterAction(
                    label = "A",
                    text = stringResource(R.string.action_ok),
                    onClick = {
                        apps.getOrNull(focusedIndex)?.let {
                            launchApp(it, LauncherInputSource.TOUCH)
                        }
                    }
                )
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}

@Composable
private fun AppFooterAction(
    label: String,
    text: String,
    onClick: () -> Unit
) {
    val palette = LocalHorizonColors.current
    Row(
        modifier = Modifier
            .height(40.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizonButtonGlyph(
            label = label,
            size = 20.dp,
            fill = palette.mutedText,
            contentColor = palette.background
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = palette.mutedText,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun AppGridTile(
    app: InstalledAppInfo,
    focused: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalHorizonColors.current
    val icon = rememberAppIcon(app.icon)
    val pulse = rememberInfiniteTransition(label = "appsTilePulse")
    val pulseValue by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "appsTilePulseValue"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) palette.panel else Color.Transparent)
            .then(
                if (focused) {
                    Modifier.border(
                        width = 2.dp,
                        color = palette.accent.copy(alpha = 0.35f + pulseValue * 0.65f),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            color = palette.text,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun rememberAppIcon(drawable: Drawable?): ImageBitmap? {
    return remember(drawable) {
        drawable?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
}
