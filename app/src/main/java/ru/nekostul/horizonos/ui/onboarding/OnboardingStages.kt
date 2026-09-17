package ru.nekostul.horizonos.ui.onboarding

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.games.Platform
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.user.UserAvatar
import ru.nekostul.horizonos.ui.user.UserProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingIntroStage() {
    val palette = LocalHorizonColors.current
    val assembly = remember { Animatable(0f) }
    val loadingReveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        assembly.animateTo(
            targetValue = 1f,
            animationSpec = tween(2_900, delayMillis = 260, easing = FastOutSlowInEasing)
        )
        loadingReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(1_450, delayMillis = 90, easing = FastOutSlowInEasing)
        )
    }
    val transition = rememberInfiniteTransition(label = "onboardingIntroOrbit")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingIntroOrbitValue"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingIntroPulse"
    )
    val sweep by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingIntroSweep"
    )
    val word = "HORIZONOS"
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxHeight < 320.dp
        val wordAreaHeight = if (compact) 58.dp else 76.dp
        val coreSize = if (compact) 112.dp else 160.dp
        val wordFontSize = if (compact) 32.sp else 42.sp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(wordAreaHeight),
                contentAlignment = Alignment.Center
            ) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = (assembly.value * 1.4f).coerceIn(0f, 1f) }
            ) {
                val scanProgress = (assembly.value * 1.35f).coerceIn(0f, 1f)
                val beamX = size.width * (-0.16f + scanProgress * 1.32f)
                drawLine(
                    color = palette.accent.copy(alpha = 0.22f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.64f),
                    start = Offset(beamX - 38.dp.toPx(), size.height / 2f),
                    end = Offset(beamX + 38.dp.toPx(), size.height / 2f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                word.forEachIndexed { index, character ->
                    val letterProgress =
                        ((assembly.value - index * 0.052f) / 0.48f).coerceIn(0f, 1f)
                    val normalized = index.toFloat() / (word.length - 1).toFloat()
                    val launchAngle = (-0.90f + normalized * 1.80f) * PI.toFloat()
                    val launchDistance = with(LocalDensity.current) {
                        (120 + (index % 3) * 26).dp.toPx()
                    }
                    val launchX = cos(launchAngle) * launchDistance
                    val launchY = sin(launchAngle) * launchDistance * 0.58f
                    val spinDirection = if (index % 2 == 0) -1f else 1f
                    Text(
                        text = character.toString(),
                        color = palette.text,
                        fontSize = wordFontSize,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.graphicsLayer {
                            alpha = letterProgress
                            translationX = launchX * (1f - letterProgress)
                            translationY = launchY * (1f - letterProgress)
                            rotationZ = spinDirection * 22f * (1f - letterProgress)
                            scaleX = 0.48f + letterProgress * 0.52f
                            scaleY = 0.48f + letterProgress * 0.52f
                        }
                    )
                }
            }
        }
            Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
            Canvas(
                Modifier
                    .size(coreSize)
                    .graphicsLayer {
                        alpha = loadingReveal.value
                        scaleX = 0.72f + loadingReveal.value * 0.28f
                        scaleY = 0.72f + loadingReveal.value * 0.28f
                    }
            ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.32f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.30f + pulse * 0.10f),
                        palette.accent.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * (0.46f + pulse * 0.06f)
                ),
                radius = size.minDimension * (0.46f + pulse * 0.06f),
                center = center
            )
            drawCircle(
                color = palette.panel.copy(alpha = 0.84f),
                radius = radius * 0.86f,
                center = center
            )
            drawCircle(
                color = palette.accent.copy(alpha = 0.34f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f + pulse * 0.08f),
                radius = radius * 1.42f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawArc(
                color = palette.accent,
                topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.25f),
                size = Size(radius * 2.5f, radius * 2.5f),
                startAngle = -90f,
                sweepAngle = (loadingReveal.value * 330f).coerceAtLeast(3f),
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            val dotAngle = (-90f + loadingReveal.value * 330f) * PI.toFloat() / 180f
            val dotDistance = radius * 1.25f
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(
                    center.x + cos(dotAngle) * dotDistance,
                    center.y + sin(dotAngle) * dotDistance
                )
            )
            repeat(10) { index ->
                val angle = (orbit + index * 36f) * PI.toFloat() / 180f
                val distance = radius * (1.52f + (index % 2) * 0.12f)
                val point = Offset(
                    center.x + cos(angle) * distance,
                    center.y + sin(angle) * distance
                )
                drawCircle(
                    color = palette.accent.copy(alpha = 0.32f + pulse * 0.25f),
                    radius = if (index % 2 == 0) 3.5.dp.toPx() else 2.dp.toPx(),
                    center = point
                )
            }
            val logoHalfHeight = radius * 0.55f
            val logoHalfWidth = radius * 0.58f
            drawLine(
                color = Color.White.copy(alpha = 0.94f),
                start = Offset(center.x - logoHalfWidth, center.y - logoHalfHeight),
                end = Offset(center.x - logoHalfWidth, center.y + logoHalfHeight),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.94f),
                start = Offset(center.x + logoHalfWidth, center.y - logoHalfHeight),
                end = Offset(center.x + logoHalfWidth, center.y + logoHalfHeight),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.94f),
                start = Offset(center.x - logoHalfWidth, center.y),
                end = Offset(center.x + logoHalfWidth, center.y),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = palette.accent.copy(alpha = 0.82f),
                start = Offset(center.x - logoHalfWidth * 0.6f, center.y + radius * 0.78f),
                end = Offset(center.x + logoHalfWidth * 0.6f, center.y + radius * 0.78f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            }
            Spacer(Modifier.height(if (compact) 2.dp else 4.dp))
            Text(
                text = stringResource(R.string.onboarding_intro_subtitle),
                color = palette.mutedText,
                fontSize = if (compact) 12.sp else 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = loadingReveal.value }
            )
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
            Text(
                text = stringResource(R.string.onboarding_intro_loading),
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { alpha = loadingReveal.value }
            )
            Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
            Box(
                modifier = Modifier
                    .width(if (compact) 250.dp else 340.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.panel.copy(alpha = 0.70f))
                    .graphicsLayer { alpha = loadingReveal.value }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((0.10f + loadingReveal.value * 0.90f).coerceIn(0.01f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    palette.accent.copy(alpha = 0.35f),
                                    Color.White,
                                    palette.accent
                                )
                            )
                        )
                )
            }
            Spacer(Modifier.height(if (compact) 3.dp else 5.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer { alpha = loadingReveal.value }
            ) {
                repeat(4) { index ->
                    val active = ((sweep * 4f).toInt() % 4) == index
                    Box(
                        Modifier
                            .size(if (active) 7.dp else 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) Color.White else palette.accent.copy(alpha = 0.38f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingLanguageStage(
    focusIndex: Int,
    onSelect: (Int) -> Unit,
    onFocus: (Int) -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 380.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingTitle(
            title = stringResource(R.string.onboarding_language_title),
            subtitle = stringResource(R.string.onboarding_language_description)
        )
        Spacer(Modifier.height(26.dp))
        OnboardingChoice(
            title = "Русский",
            focused = focusIndex == 0,
            onClick = { onSelect(0) },
            onFocus = { onFocus(0) },
            minHeight = 54.dp,
            emphasizeSelection = true
        )
        Spacer(Modifier.height(12.dp))
        OnboardingChoice(
            title = "English",
            focused = focusIndex == 1,
            onClick = { onSelect(1) },
            onFocus = { onFocus(1) },
            minHeight = 54.dp,
            emphasizeSelection = true
        )
    }
}

@Composable
fun OnboardingProfileStage(
    profile: UserProfile,
    nick: String,
    focusIndex: Int,
    onEditNick: () -> Unit,
    onContinue: () -> Unit,
    onFocus: (Int) -> Unit
) {
    val palette = LocalHorizonColors.current
    val compactScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 900
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (compactScreen) 132.dp else 166.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(palette.panel.copy(alpha = 0.80f))
                    .border(
                        width = 1.dp,
                        color = palette.divider.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                UserAvatar(
                    avatarPath = profile.avatarPath,
                    size = if (compactScreen) 114.dp else 144.dp,
                    shape = RoundedCornerShape(2.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_profile_avatar_hint),
                color = palette.mutedText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 220.dp)
            )
        }
        Spacer(Modifier.width(if (compactScreen) 18.dp else 30.dp))
        Column(Modifier.widthIn(max = if (compactScreen) 350.dp else 430.dp)) {
            OnboardingTitle(
                title = stringResource(R.string.onboarding_profile_title),
                subtitle = stringResource(R.string.onboarding_profile_description),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))
            OnboardingChoice(
                title = stringResource(R.string.onboarding_profile_nick),
                subtitle = nick.ifBlank { stringResource(R.string.onboarding_profile_nick_empty) },
                focused = focusIndex == 0,
                onClick = onEditNick,
                onFocus = { onFocus(0) }
            )
            if (nick.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                OnboardingPrimaryButton(
                    title = stringResource(R.string.onboarding_continue),
                    focused = focusIndex == 1,
                    onClick = onContinue,
                    onFocus = { onFocus(1) },
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
fun OnboardingThemeStage(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onFocus: (Int) -> Unit
) {
    val palette = LocalHorizonColors.current
    val compactScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 900
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OnboardingTitle(
            title = stringResource(R.string.onboarding_theme_title),
            subtitle = stringResource(R.string.onboarding_theme_description)
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (compactScreen) 10.dp else 22.dp)) {
            OnboardingThemePreview(
                title = stringResource(R.string.settings_theme_dark),
                dark = true,
                selected = selectedIndex == 0,
                onClick = { onSelect(0) },
                onFocus = { onFocus(0) }
            )
            OnboardingThemePreview(
                title = stringResource(R.string.settings_theme_light),
                dark = false,
                selected = selectedIndex == 1,
                onClick = { onSelect(1) },
                onFocus = { onFocus(1) }
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_theme_current_hint),
            color = palette.mutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnboardingThemePreview(
    title: String,
    dark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit
) {
    val palette = LocalHorizonColors.current
    val animatedBorderWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (selected) 3.dp else 1.dp,
        animationSpec = tween(280),
        label = "onboardingThemeBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) palette.accent else palette.divider.copy(alpha = 0.55f),
        animationSpec = tween(280),
        label = "onboardingThemeBorderColor"
    )
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1f else 0.94f,
        animationSpec = tween(280),
        label = "onboardingThemeScale"
    )
    val background = if (dark) Color(0xFF1B2027) else Color(0xFFF4F5F6)
    val panel = if (dark) Color(0xFF313A44) else Color.White
    val text = if (dark) Color.White else Color(0xFF20242A)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 900) 170.dp else 230.dp, height = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 900) 112.dp else 150.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(background)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(3.dp)
                )
                .clickable {
                    if (selected) onClick() else onFocus()
                }
                .padding(14.dp)
        ) {
            Column {
                Box(Modifier.fillMaxWidth(0.38f).height(12.dp).background(palette.accent))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(48.dp).background(panel))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(3) {
                        Box(Modifier.size(28.dp, 12.dp).background(if (it == 0) palette.accent else panel))
                    }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(title, color = text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OnboardingPermissionStage(
    step: OnboardingPermissionStep,
    currentIndex: Int,
    total: Int,
    focusIndex: Int,
    onGrant: () -> Unit,
) {
    val palette = LocalHorizonColors.current
    val context = LocalContext.current
    val compactScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 400
    val alreadyGranted = step.isGranted(context)
    val (title, description, detail) = permissionCopy(step)
    Column(
        modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_counter, currentIndex + 1, total),
            color = palette.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(if (compactScreen) 4.dp else 8.dp))
        OnboardingTitle(title = title, subtitle = description, compact = compactScreen)
        Spacer(Modifier.height(if (compactScreen) 7.dp else 12.dp))
        OnboardingPanel(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = if (compactScreen) 14.dp else 24.dp
        ) {
            Text(
                text = detail,
                color = palette.text,
                fontSize = if (compactScreen) 14.sp else 16.sp,
                lineHeight = if (compactScreen) 20.sp else 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(if (compactScreen) 10.dp else 18.dp))
        OnboardingPrimaryButton(
            title = stringResource(
                if (alreadyGranted) R.string.onboarding_continue
                else R.string.onboarding_permissions_grant
            ),
            focused = focusIndex == 0,
            onClick = onGrant,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        if (step.allFilesAccess) {
            Spacer(Modifier.height(if (compactScreen) 6.dp else 10.dp))
            Text(
                text = stringResource(R.string.onboarding_permissions_android_screen),
                color = palette.mutedText,
                fontSize = if (compactScreen) 11.sp else 12.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(if (compactScreen) 6.dp else 10.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_root_note),
            color = palette.mutedText,
            fontSize = if (compactScreen) 10.sp else 12.sp,
            lineHeight = if (compactScreen) 14.sp else 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 660.dp)
        )
    }
}

private data class PermissionCopy(
    val title: String,
    val description: String,
    val detail: String
)

@Composable
private fun permissionCopy(step: OnboardingPermissionStep): PermissionCopy = when {
    step.allFilesAccess -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_all_files_title),
        description = stringResource(R.string.onboarding_permission_all_files_description),
        detail = stringResource(R.string.onboarding_permission_all_files_detail)
    )
    step.permission == Manifest.permission.READ_MEDIA_IMAGES -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_images_title),
        description = stringResource(R.string.onboarding_permission_images_description),
        detail = stringResource(R.string.onboarding_permission_images_detail)
    )
    step.permission == Manifest.permission.READ_MEDIA_VIDEO -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_video_title),
        description = stringResource(R.string.onboarding_permission_video_description),
        detail = stringResource(R.string.onboarding_permission_video_detail)
    )
    step.permission == Manifest.permission.READ_EXTERNAL_STORAGE -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_storage_title),
        description = stringResource(R.string.onboarding_permission_storage_description),
        detail = stringResource(R.string.onboarding_permission_storage_detail)
    )
    step.permission == Manifest.permission.WRITE_EXTERNAL_STORAGE -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_storage_write_title),
        description = stringResource(R.string.onboarding_permission_storage_write_description),
        detail = stringResource(R.string.onboarding_permission_storage_write_detail)
    )
    step.id == "nearby_devices" -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_bluetooth_title),
        description = stringResource(R.string.onboarding_permission_bluetooth_description),
        detail = stringResource(R.string.onboarding_permission_bluetooth_detail)
    )
    step.id == "wifi_location" -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_wifi_location_title),
        description = stringResource(R.string.onboarding_permission_wifi_location_description),
        detail = stringResource(R.string.onboarding_permission_wifi_location_detail)
    )
    else -> PermissionCopy(
        title = stringResource(R.string.onboarding_permission_notifications_title),
        description = stringResource(R.string.onboarding_permission_notifications_description),
        detail = stringResource(R.string.onboarding_permission_notifications_detail)
    )
}

@Composable
fun OnboardingGamesStage(
    hasAddedGames: Boolean,
    focusIndex: Int,
    onOpenGames: () -> Unit,
    onContinue: () -> Unit,
    onFocus: (Int) -> Unit
) {
    val compactScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 400
    val title = if (hasAddedGames) {
        stringResource(R.string.onboarding_games_added_title)
    } else {
        stringResource(R.string.onboarding_games_title)
    }
    val subtitle = if (hasAddedGames) {
        stringResource(R.string.onboarding_games_added_description)
    } else {
        stringResource(R.string.onboarding_games_description)
    }
    Column(
        modifier = Modifier.widthIn(max = 520.dp).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingTitle(title = title, subtitle = subtitle)
        Spacer(Modifier.height(if (compactScreen) 12.dp else 24.dp))
        if (hasAddedGames) {
            OnboardingChoice(
                title = stringResource(R.string.onboarding_games_add_another),
                subtitle = stringResource(R.string.onboarding_games_add_another_hint),
                focused = focusIndex == 0,
                onClick = onOpenGames,
                onFocus = { onFocus(0) },
                minHeight = 54.dp
            )
            Spacer(Modifier.height(12.dp))
            OnboardingPrimaryButton(
                title = stringResource(R.string.onboarding_continue),
                focused = focusIndex == 1,
                onClick = onContinue,
                onFocus = { onFocus(1) },
                modifier = Modifier
            )
        } else {
            OnboardingPrimaryButton(
                title = stringResource(R.string.onboarding_games_add_button),
                focused = focusIndex == 0,
                onClick = onOpenGames,
                onFocus = { onFocus(0) },
                modifier = Modifier
            )
            Spacer(Modifier.height(12.dp))
            OnboardingChoice(
                title = stringResource(R.string.onboarding_games_skip),
                subtitle = stringResource(R.string.onboarding_games_skip_hint),
                focused = focusIndex == 1,
                onClick = onContinue,
                onFocus = { onFocus(1) },
                minHeight = 54.dp
            )
        }
        Spacer(Modifier.height(if (compactScreen) 10.dp else 18.dp))
        Text(
            text = stringResource(R.string.onboarding_games_existing_manager),
            color = LocalHorizonColors.current.mutedText,
            fontSize = if (compactScreen) 10.sp else 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OnboardingTutorialStage(
    index: Int,
    onNext: () -> Unit
) {
    val palette = LocalHorizonColors.current
    val title = stringResource(
        when (index) {
            0 -> R.string.onboarding_tutorial_add_title
            1 -> R.string.onboarding_tutorial_files_title
            2 -> R.string.onboarding_tutorial_avatar_title
            else -> R.string.onboarding_tutorial_nick_title
        }
    )
    val description = stringResource(
        when (index) {
            0 -> R.string.onboarding_tutorial_add_description
            1 -> R.string.onboarding_tutorial_files_description
            2 -> R.string.onboarding_tutorial_avatar_description
            else -> R.string.onboarding_tutorial_nick_description
        }
    )
    Column(
        modifier = Modifier.widthIn(max = 700.dp).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_tutorial_counter, index + 1, 4),
            color = palette.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        OnboardingPanel(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TutorialVisual(index, containerSize = 112.dp, canvasSize = 82.dp)
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        color = palette.text,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = description,
                        color = palette.mutedText,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    OnboardingPrimaryButton(
                        title = stringResource(if (index == 3) R.string.onboarding_continue else R.string.onboarding_next),
                        focused = true,
                        onClick = onNext
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(4) { item ->
                Box(
                    Modifier
                        .size(if (item == index) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (item == index) palette.accent else palette.mutedText.copy(alpha = 0.50f))
                )
            }
        }
    }
}

@Composable
private fun TutorialVisual(index: Int, containerSize: Dp, canvasSize: Dp) {
    val palette = LocalHorizonColors.current
    Box(
        modifier = Modifier
            .size(containerSize)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panel.copy(alpha = 0.78f))
            .border(1.dp, palette.divider.copy(alpha = 0.50f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(canvasSize)) {
            val stroke = 4.dp.toPx()
            when (index) {
                0 -> {
                    drawRoundRect(
                        color = palette.accent,
                        topLeft = Offset(size.width * 0.12f, size.height * 0.22f),
                        size = Size(size.width * 0.76f, size.height * 0.56f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                        style = Stroke(stroke)
                    )
                    drawLine(
                        color = palette.text,
                        start = Offset(size.width * 0.50f, size.height * 0.36f),
                        end = Offset(size.width * 0.50f, size.height * 0.64f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = palette.text,
                        start = Offset(size.width * 0.36f, size.height * 0.50f),
                        end = Offset(size.width * 0.64f, size.height * 0.50f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
                1 -> {
                    drawRoundRect(
                        color = palette.accent,
                        topLeft = Offset(size.width * 0.14f, size.height * 0.26f),
                        size = Size(size.width * 0.72f, size.height * 0.48f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = Stroke(stroke)
                    )
                    repeat(3) { row ->
                        drawLine(
                            color = palette.text,
                            start = Offset(size.width * 0.27f, size.height * (0.38f + row * 0.12f)),
                            end = Offset(size.width * 0.73f, size.height * (0.38f + row * 0.12f)),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                2 -> {
                    drawCircle(color = palette.accent, radius = size.width * 0.24f, center = Offset(size.width / 2f, size.height * 0.34f))
                    drawArc(
                        color = palette.text,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.20f, size.height * 0.42f),
                        size = Size(size.width * 0.60f, size.height * 0.42f),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                else -> {
                    drawRoundRect(
                        color = palette.accent,
                        topLeft = Offset(size.width * 0.12f, size.height * 0.25f),
                        size = Size(size.width * 0.76f, size.height * 0.50f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = Stroke(stroke)
                    )
                    drawLine(
                        color = palette.text,
                        start = Offset(size.width * 0.28f, size.height * 0.50f),
                        end = Offset(size.width * 0.72f, size.height * 0.50f),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingOpenSourceStage(
    onContinue: () -> Unit
) {
    val palette = LocalHorizonColors.current
    Column(
        modifier = Modifier.widthIn(max = 720.dp).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onboarding_opensource_title),
            color = palette.text,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_opensource_description),
            color = palette.mutedText,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.onboarding_opensource_open_source_statement),
            color = palette.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier.widthIn(max = 680.dp).padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_opensource_refund),
                color = palette.text,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.onboarding_opensource_updates),
                color = palette.mutedText,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_opensource_repository),
            color = palette.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "https://github.com/nekostul/HorizonOS",
            color = palette.accent,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(8.dp))
        OnboardingPrimaryButton(
            title = stringResource(R.string.onboarding_continue),
            focused = true,
            onClick = onContinue,
            modifier = Modifier.widthIn(max = 300.dp)
        )
    }
}

@Composable
fun OnboardingAudioStage(
    settingsSoundEnabled: Boolean,
    ambientEnabled: Boolean,
    vibrationEnabled: Boolean,
    focusIndex: Int,
    onFocus: (Int) -> Unit,
    onSoundToggle: () -> Unit,
    onAmbientToggle: () -> Unit,
    onVibrationToggle: () -> Unit,
    onFinish: () -> Unit
) {
    val palette = LocalHorizonColors.current
    Column(
        modifier = Modifier.widthIn(max = 760.dp).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingTitle(
            title = stringResource(R.string.onboarding_audio_title),
            subtitle = stringResource(R.string.onboarding_audio_description)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnboardingChoice(
                title = stringResource(R.string.onboarding_audio_vibration),
                subtitle = toggleLabel(vibrationEnabled),
                focused = focusIndex == 0,
                onClick = onVibrationToggle,
                onFocus = { onFocus(0) },
                modifier = Modifier.weight(1f),
                minHeight = 54.dp
            )
            OnboardingChoice(
                title = stringResource(R.string.onboarding_audio_sounds),
                subtitle = toggleLabel(settingsSoundEnabled),
                focused = focusIndex == 1,
                onClick = onSoundToggle,
                onFocus = { onFocus(1) },
                modifier = Modifier.weight(1f),
                minHeight = 54.dp
            )
            OnboardingChoice(
                title = stringResource(R.string.onboarding_audio_ambient),
                subtitle = toggleLabel(ambientEnabled),
                focused = focusIndex == 2,
                onClick = onAmbientToggle,
                onFocus = { onFocus(2) },
                modifier = Modifier.weight(1f),
                minHeight = 54.dp
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onboarding_audio_custom_hint),
            color = palette.mutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_audio_settings_hint),
            color = palette.mutedText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        OnboardingPrimaryButton(
            title = stringResource(R.string.onboarding_start),
            focused = focusIndex == 3,
            onClick = onFinish,
            onFocus = { onFocus(3) },
            modifier = Modifier.widthIn(max = 300.dp)
        )
    }
}

@Composable
private fun toggleLabel(enabled: Boolean): String = stringResource(
    if (enabled) R.string.onboarding_toggle_on else R.string.onboarding_toggle_off
)

@Composable
fun OnboardingFinalStage() {
    val palette = LocalHorizonColors.current
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(1_050, easing = FastOutSlowInEasing)
        )
    }
    val transition = rememberInfiniteTransition(label = "onboardingFinalOrbit")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "onboardingFinalOrbitValue"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingFinalPulse"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            alpha = reveal.value
            scaleX = 0.86f + reveal.value * 0.14f
            scaleY = 0.86f + reveal.value * 0.14f
        }
    ) {
        Canvas(Modifier.size(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        palette.accent.copy(alpha = 0.25f + pulse * 0.10f),
                        palette.accent.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension * (0.46f + pulse * 0.04f),
                center = center
            )
            drawCircle(
                color = palette.accent,
                radius = radius,
                center = center,
                style = Stroke(3.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f + pulse * 0.10f),
                radius = radius * 1.42f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
            drawArc(
                color = Color.White,
                topLeft = Offset(center.x - radius * 1.26f, center.y - radius * 1.26f),
                size = Size(radius * 2.52f, radius * 2.52f),
                startAngle = orbit,
                sweepAngle = 100f,
                useCenter = false,
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = palette.accent.copy(alpha = 0.75f),
                topLeft = Offset(center.x - radius * 1.62f, center.y - radius * 1.62f),
                size = Size(radius * 3.24f, radius * 3.24f),
                startAngle = orbit + 180f,
                sweepAngle = 72f,
                useCenter = false,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
            drawLine(
                color = Color.White,
                start = Offset(center.x - radius * 0.48f, center.y + radius * 0.04f),
                end = Offset(center.x - radius * 0.08f, center.y + radius * 0.42f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(center.x - radius * 0.08f, center.y + radius * 0.42f),
                end = Offset(center.x + radius * 0.58f, center.y - radius * 0.42f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            repeat(10) { index ->
                val angle = (orbit * 0.7f + index * 36f) * PI.toFloat() / 180f
                val distance = radius * (1.72f + (index % 3) * 0.12f)
                drawCircle(
                    color = palette.accent.copy(alpha = 0.35f + pulse * 0.22f),
                    radius = if (index % 2 == 0) 2.8.dp.toPx() else 1.8.dp.toPx(),
                    center = Offset(
                        center.x + cos(angle) * distance,
                        center.y + sin(angle) * distance
                    )
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onboarding_final_title),
            color = palette.text,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.onboarding_final_description),
            color = palette.mutedText,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}
