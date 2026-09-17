package ru.nekostul.horizonos.ui.user

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import java.io.File

@Composable
fun UserAvatar(
    avatarPath: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    val placeholderColor = LocalHorizonColors.current.panel
    val glyphColor = LocalHorizonColors.current.mutedText

    var bitmap by remember(avatarPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(avatarPath) {
        bitmap = withContext(Dispatchers.IO) {
            val path = avatarPath ?: return@withContext null
            val file = File(path)
            if (!file.exists()) return@withContext null
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(placeholderColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CartoonAvatar(
                accent = LocalHorizonColors.current.accent,
                lineColor = glyphColor,
                size = size * 0.78f
            )
        }
    }
}

@Composable
private fun CartoonAvatar(accent: Color, lineColor: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val face = Color(0xFFFFC857)
        val hair = Color(0xFF263238)
        val eye = Color(0xFF182026)

        drawCircle(
            color = accent.copy(alpha = 0.20f),
            radius = w * 0.50f,
            center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = face,
            radius = w * 0.31f,
            center = Offset(w / 2f, h * 0.40f)
        )
        drawArc(
            color = hair,
            startAngle = 180f,
            sweepAngle = -180f,
            useCenter = true,
            topLeft = Offset(w * 0.18f, h * 0.13f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.49f)
        )
        drawCircle(eye, w * 0.035f, Offset(w * 0.40f, h * 0.40f))
        drawCircle(eye, w * 0.035f, Offset(w * 0.60f, h * 0.40f))
        drawArc(
            color = lineColor,
            startAngle = 15f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * 0.39f, h * 0.43f),
            size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.13f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(w * 0.22f, h * 0.66f),
            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.30f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(w * 0.33f, h * 0.76f),
            end = Offset(w * 0.67f, h * 0.76f),
            strokeWidth = 2.dp.toPx()
        )
    }
}
