package ru.nekostul.horizonos.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HorizonNavigation {

    private val _requests = MutableStateFlow(0)
    val requests: StateFlow<Int> = _requests.asStateFlow()

    /**
     * True while the main home screen is the visible top-level surface. The
     * gamepad B button is intentionally inert there, so global key handling
     * (sound/haptics) is suppressed.
     */
    @Volatile
    var homeScreenActive: Boolean = false

    fun requestHome() {
        _requests.value += 1
    }

    fun isHomeKeyCode(keyCode: Int): Boolean =
        keyCode == android.view.KeyEvent.KEYCODE_BUTTON_MODE ||
            keyCode == android.view.KeyEvent.KEYCODE_HOME
}
