package ru.nekostul.horizonos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HorizonButtonGlyph(
    label: String,
    size: Dp = 22.dp,
    fill: Color = Color.White,
    contentColor: Color = Color(0xFF2B2B2B)
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(fill, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = (size.value * 0.58f).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (size.value * 0.58f).sp
        )
    }
}
