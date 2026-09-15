package ru.nekostul.horizonos.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.user.UserAvatar

@Composable
fun HomeProfileButton(
    avatarPath: String?,
    size: Dp,
    focused: Boolean,
    onClick: () -> Unit
) {
    val accent = LocalHorizonColors.current.accent
    val divider = LocalHorizonColors.current.divider
    val pulse = rememberInfiniteTransition(label = "profilePulse")
    val selectionAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profileSelectionAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (focused) {
                    Modifier.border(
                        width = 2.dp,
                        color = accent.copy(alpha = selectionAlpha),
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(2.dp, divider, CircleShape)
                }
            )
            .clickable { onClick() },
    ) {
        UserAvatar(
            avatarPath = avatarPath,
            size = size,
            modifier = Modifier.clip(CircleShape)
        )
    }
}
