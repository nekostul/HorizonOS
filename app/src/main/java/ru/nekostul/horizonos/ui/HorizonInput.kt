package ru.nekostul.horizonos.ui

import android.view.InputDevice

/**
 * True when a real (external, non-virtual) gamepad or joystick is connected.
 * Used to decide whether a screen should open in gamepad input mode so the
 * selection frame is visible immediately, without a first directional press.
 */
fun isExternalGamepadConnected(): Boolean {
    return InputDevice.getDeviceIds().any { deviceId ->
        val device = InputDevice.getDevice(deviceId) ?: return@any false
        val sources = device.sources
        device.isExternal &&
            !device.isVirtual &&
            (sources and InputDevice.SOURCE_GAMEPAD != 0 ||
                sources and InputDevice.SOURCE_JOYSTICK != 0)
    }
}
