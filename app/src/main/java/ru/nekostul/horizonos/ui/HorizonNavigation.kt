package ru.nekostul.horizonos.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central entry point for the controller HOME/Xbox button.
 *
 * The button is captured once at the activity level (and inside the shared
 * [ru.nekostul.horizonos.ui.settings.HorizonOverlay] dialog used by the game
 * screens). Every request bumps [requests]; the root [HorizonHome] observes it
 * and closes whatever internal screen is open, returning to the Home Screen.
 *
 * This keeps the HOME behaviour in one place instead of reimplementing it in
 * every screen.
 */
object HorizonNavigation {

    private val _requests = MutableStateFlow(0)
    val requests: StateFlow<Int> = _requests.asStateFlow()

    /** Ask the launcher to close any internal screen and return Home. */
    fun requestHome() {
        _requests.value += 1
    }

    /** True when [keyCode] is the controller HOME/Xbox button. */
    fun isHomeKeyCode(keyCode: Int): Boolean =
        keyCode == android.view.KeyEvent.KEYCODE_BUTTON_MODE ||
            keyCode == android.view.KeyEvent.KEYCODE_HOME
}
