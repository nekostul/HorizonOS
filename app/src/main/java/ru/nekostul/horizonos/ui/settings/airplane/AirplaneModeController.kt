package ru.nekostul.horizonos.ui.settings.airplane

import android.content.Context
import ru.nekostul.horizonos.ui.settings.AirplaneModeController

class AirplaneModeStateController(context: Context) {
    private val systemController = AirplaneModeController(context)

    val canControlSystemMode: Boolean
        get() = systemController.capability

    fun systemState(): Boolean? = systemController.currentState()

    fun requestSystemMode(enabled: Boolean): Boolean = systemController.setEnabled(enabled)
}
