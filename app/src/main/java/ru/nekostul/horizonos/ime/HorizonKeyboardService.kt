package ru.nekostul.horizonos.ime

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.ui.keyboard.KeyboardLanguage
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository

class HorizonKeyboardService : InputMethodService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var darkTheme = true
    private var language = KeyboardLanguage.EN
    private var keyboardView: HorizonKeyboardImeView? = null

    override fun onCreate() {
        super.onCreate()
        window?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        serviceScope.launch {
            LauncherSettingsRepository(applicationContext).settings.collectLatest { settings ->
                darkTheme = settings.theme != "light"
                language = when (settings.language) {
                    "ru" -> KeyboardLanguage.RU
                    "en" -> KeyboardLanguage.EN
                    else -> if (resources.configuration.locales[0].language == "ru") {
                        KeyboardLanguage.RU
                    } else {
                        KeyboardLanguage.EN
                    }
                }
                keyboardView?.setDarkTheme(darkTheme)
                keyboardView?.setLanguage(language)
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyboardView?.onKeyDown(keyCode, event) == true || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyboardView?.onKeyUp(keyCode, event) == true || super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        return keyboardView?.onGenericMotionEvent(event) == true || super.onGenericMotionEvent(event)
    }

    override fun onCreateInputView(): View {
        return HorizonKeyboardImeView(this) { action ->
            when (action) {
                HorizonKeyboardImeView.Action.TEXT -> {
                    keyboardView?.consumeTextAction()?.let { currentInputConnection?.commitText(it, 1) }
                }
                HorizonKeyboardImeView.Action.BACKSPACE -> currentInputConnection?.deleteSurroundingText(1, 0)
                HorizonKeyboardImeView.Action.SPACE -> currentInputConnection?.commitText(" ", 1)
                HorizonKeyboardImeView.Action.LANGUAGE -> Unit
                HorizonKeyboardImeView.Action.MODE -> Unit
                      HorizonKeyboardImeView.Action.SHIFT -> Unit
                      HorizonKeyboardImeView.Action.MOVE_LEFT -> sendCursorKey(KeyEvent.KEYCODE_DPAD_LEFT)
                      HorizonKeyboardImeView.Action.MOVE_RIGHT -> sendCursorKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                      HorizonKeyboardImeView.Action.RETURN -> sendEditorAction()
                HorizonKeyboardImeView.Action.CONFIRM -> sendEditorAction()
                HorizonKeyboardImeView.Action.CANCEL -> requestHideSelf(0)
            }
        }.also {
            keyboardView = it
            it.setDarkTheme(darkTheme)
            it.setLanguage(language)
        }
    }

    private fun sendEditorAction() {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        val connection = currentInputConnection ?: return
        if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.performEditorAction(action)
        } else {
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        requestHideSelf(0)
    }

    private fun sendCursorKey(keyCode: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    override fun onDestroy() {
        keyboardView = null
        serviceScope.cancel()
        super.onDestroy()
    }
}
