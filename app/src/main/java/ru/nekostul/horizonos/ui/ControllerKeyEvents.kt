package ru.nekostul.horizonos.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal fun isHorizonConfirmKey(event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    val nativeKeyCode = event.nativeKeyEvent.keyCode
    return event.key == Key.ButtonA ||
        event.key == Key.DirectionCenter ||
        event.key == Key.Enter ||
        event.key == Key.NumPadEnter ||
        nativeKeyCode == AndroidKeyEvent.KEYCODE_BUTTON_A ||
        nativeKeyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        nativeKeyCode == AndroidKeyEvent.KEYCODE_ENTER ||
        nativeKeyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
}
