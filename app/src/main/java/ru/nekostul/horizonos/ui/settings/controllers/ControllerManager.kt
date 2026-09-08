package ru.nekostul.horizonos.ui.settings.controllers

import android.view.InputDevice

data class ConnectedController(
    val name: String?,
    val connection: ControllerConnection,
    val deviceId: Int
)

enum class ControllerConnection {
    GAMEPAD,
    JOYSTICK_HID
}

object ControllerManager {
    fun connectedControllers(): List<ConnectedController> = InputDevice.getDeviceIds().toList().mapNotNull { id ->
        val device = InputDevice.getDevice(id) ?: return@mapNotNull null
        val source = device.sources
        if (source and InputDevice.SOURCE_GAMEPAD == 0 && source and InputDevice.SOURCE_JOYSTICK == 0) return@mapNotNull null
        ConnectedController(
            name = device.name?.takeIf(String::isNotBlank),
            connection = when {
                source and InputDevice.SOURCE_DPAD != 0 -> ControllerConnection.GAMEPAD
                else -> ControllerConnection.JOYSTICK_HID
            },
            deviceId = id
        )
    }
}
