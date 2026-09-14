package ru.nekostul.horizonos.ui.lockscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide lock state for the HorizonOS lock screen.
 *
 * The launcher always starts locked ([locked] begins as true) and is locked
 * again whenever the device screen turns off. [unlock] is called once the user
 * completes the unlock screen. It is intentionally not persisted.
 */
object HorizonLock {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun lock() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }
}
