package ru.nekostul.horizonos.ui.home.status

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reference status-bar clock: always uses the 24-hour HH:mm format. */
@Composable
fun StatusClock(
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    val pattern = "HH:mm"
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat(pattern, Locale.getDefault()).format(Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    Text(
        text = currentTime,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}
