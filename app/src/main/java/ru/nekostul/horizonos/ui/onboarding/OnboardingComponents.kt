package ru.nekostul.horizonos.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors

@Composable
fun OnboardingTopBar(page: OnboardingPage) {
    val palette = LocalHorizonColors.current
    val current = when (page) {
        OnboardingPage.INTRO -> 0
        OnboardingPage.FINISHING -> 9
        else -> page.ordinal
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "HORIZONOS",
            color = palette.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.weight(1f))
        if (current > 0) {
            Text(
                text = "$current / 9",
                color = palette.mutedText,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun OnboardingTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    compact: Boolean = false
) {
    val palette = LocalHorizonColors.current
    Column(
        modifier = modifier.widthIn(max = 720.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = palette.text,
            fontSize = if (compact) 26.sp else 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        subtitle?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                color = palette.mutedText,
                fontSize = if (compact) 14.sp else 16.sp,
                lineHeight = if (compact) 19.sp else 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OnboardingChoice(
    title: String,
    focused: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    minHeight: Dp = 52.dp,
    emphasizeSelection: Boolean = false
) {
    val palette = LocalHorizonColors.current
    val view = LocalView.current
    val shape = RoundedCornerShape(0.dp)
    val compactScreen = LocalConfiguration.current.screenHeightDp < 400
    val hasSubtitle = !subtitle.isNullOrBlank()
    val expandable = hasSubtitle || emphasizeSelection
    val effectiveMinHeight = if (compactScreen) {
        (minHeight - 10.dp).coerceAtLeast(44.dp)
    } else {
        minHeight
    }
    val targetHeight = when {
        expandable && focused -> effectiveMinHeight + 10.dp
        expandable -> (effectiveMinHeight - 7.dp).coerceAtLeast(40.dp)
        else -> effectiveMinHeight
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(300),
        label = "onboardingChoiceHeight"
    )
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "onboardingChoiceScale"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (focused) 2.dp else 1.dp,
        animationSpec = tween(240),
        label = "onboardingChoiceBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (focused) palette.accent else palette.divider.copy(alpha = 0.52f),
        animationSpec = tween(240),
        label = "onboardingChoiceBorderColor"
    )
    val animatedBackground by animateColorAsState(
        targetValue = palette.panel.copy(alpha = if (focused) 0.92f else 0.72f),
        animationSpec = tween(240),
        label = "onboardingChoiceBackground"
    )
    val animatedTitleSize by animateFloatAsState(
        targetValue = if (!emphasizeSelection) 19f else if (focused) 22f else 18f,
        animationSpec = tween(300),
        label = "onboardingChoiceTitleSize"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .background(animatedBackground)
            .border(
                width = animatedBorderWidth,
                color = animatedBorderColor,
                shape = shape
            )
            .clickable {
                if (focused) {
                    LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                } else {
                    LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.TOUCH)
                }
                LauncherAudioManager.performHapticFeedback(view)
                if (focused) {
                    onClick()
                } else {
                    onFocus()
                }
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .height(animatedHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(11.dp)
                .border(2.dp, if (focused) palette.accent else palette.mutedText, CircleShape)
                .background(if (focused) palette.accent else Color.Transparent, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = palette.text,
                fontSize = animatedTitleSize.sp,
                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal
            )
            subtitle?.let { description ->
                AnimatedVisibility(
                    visible = focused,
                    enter = fadeIn(tween(160)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
                ) {
                    Column {
                        Spacer(Modifier.height(3.dp))
                        Text(description, color = palette.mutedText, fontSize = 13.sp, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPrimaryButton(
    title: String,
    focused: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalHorizonColors.current
    val view = LocalView.current
    val shape = RoundedCornerShape(0.dp)
    val compactScreen = LocalConfiguration.current.screenHeightDp < 400
    val buttonVerticalPadding = if (compactScreen) 5.dp else 7.dp
    val glyphSize = if (compactScreen) 18.dp else 20.dp
    val glyphGap = if (compactScreen) 6.dp else 8.dp
    Row(
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(shape)
            .background(
                if (enabled) palette.accent.copy(alpha = if (focused) 1f else 0.82f)
                else palette.mutedText.copy(alpha = 0.30f)
            )
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.White.copy(alpha = 0.82f) else palette.accent,
                shape = shape
            )
            .clickable(enabled = enabled) {
                if (focused) {
                    LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                } else {
                    LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.TOUCH)
                }
                LauncherAudioManager.performHapticFeedback(view)
                if (focused) {
                    onClick()
                } else {
                    onFocus()
                }
            }
            .padding(horizontal = 12.dp, vertical = buttonVerticalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizonButtonGlyph(
            label = "A",
            size = glyphSize,
            fill = Color.White,
            contentColor = palette.accent
        )
        Spacer(Modifier.width(glyphGap))
        Text(
            text = title,
            color = if (enabled) Color.White else palette.mutedText,
            fontSize = if (compactScreen) 15.sp else 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun OnboardingControlHints(
    backLabel: String,
    confirmLabel: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true
) {
    val palette = LocalHorizonColors.current
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 56.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBack) {
            Row(
                modifier = Modifier.clickable {
                    LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.TOUCH)
                    LauncherAudioManager.performHapticFeedback(view)
                    onBack()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizonButtonGlyph("B", size = 20.dp, fill = palette.text, contentColor = palette.background)
                Spacer(Modifier.width(6.dp))
                Text(backLabel, color = palette.mutedText, fontSize = 12.sp)
            }
            Spacer(Modifier.width(22.dp))
        }
        Row(
            modifier = Modifier.clickable {
                LauncherAudioManager.playConfirm(LauncherInputSource.TOUCH)
                LauncherAudioManager.performHapticFeedback(view)
                onConfirm()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizonButtonGlyph("A", size = 20.dp, fill = palette.text, contentColor = palette.background)
            Spacer(Modifier.width(6.dp))
            Text(confirmLabel, color = palette.mutedText, fontSize = 12.sp)
        }
    }
}

@Composable
fun OnboardingPanel(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val palette = LocalHorizonColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(palette.panel.copy(alpha = 0.68f))
            .border(1.dp, palette.divider.copy(alpha = 0.36f), RoundedCornerShape(3.dp))
            .padding(contentPadding)
    ) {
        content()
    }
}
