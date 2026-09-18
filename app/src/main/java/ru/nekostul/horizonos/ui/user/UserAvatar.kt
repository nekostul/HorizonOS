package ru.nekostul.horizonos.ui.user

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.nekostul.horizonos.R
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

    var bitmap by remember(avatarPath) {
        mutableStateOf(avatarPath?.let { bitmapCache[it] })
    }
    LaunchedEffect(avatarPath) {
        val path = avatarPath ?: return@LaunchedEffect
        bitmapCache[path]?.let {
            bitmap = it
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            val file = File(path)
            if (!file.exists()) null
            else BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }
        bitmapCache[path] = loaded
        bitmap = loaded
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
            Image(
                painter = painterResource(R.drawable.default_avatar),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private val bitmapCache = mutableMapOf<String, ImageBitmap?>()
