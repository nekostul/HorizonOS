package ru.nekostul.horizonos.ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/** Runs [onLongPress] after a stationary three-second touch and consumes the rest of that gesture. */
internal fun Modifier.horizonLongPress(
    durationMillis: Long = 3_000L,
    onLongPress: () -> Unit
): Modifier = pointerInput(durationMillis) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val timedOut = withTimeoutOrNull(durationMillis) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                if (change.changedToUpIgnoreConsumed()) return@withTimeoutOrNull false
                if (change.positionChange().getDistance() > 24f) {
                    return@withTimeoutOrNull false
                }
            }
            false
        } == null

        if (timedOut) {
            onLongPress()
            while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
                if (event.changes.any { it.changedToUpIgnoreConsumed() }) break
            }
        }
    }
}
