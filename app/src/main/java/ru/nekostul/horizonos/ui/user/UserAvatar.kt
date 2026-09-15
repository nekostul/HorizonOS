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
            PersonGlyph(color = glyphColor, size = size * 0.52f)
        }
    }
}

@Composable
private fun PersonGlyph(color: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w / 2f, h * 0.30f)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.12f, h * 0.52f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.68f)
        )
    }
}
