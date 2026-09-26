package ru.nekostul.horizonos.ui.games

import android.app.Activity
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log

/**
 * Receives Android "pin shortcut" requests from GameHub / GameNative / Winlator
 * and accepts them. Without this the system has no default launcher to hand the
 * request to, and those apps report that the shortcut could not be created.
 *
 * The activity has no UI: it accepts the request, adds the resulting shortcut to
 * the library and finishes.
 */
class ShortcutPinActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        val launcherApps = getSystemService(LauncherApps::class.java)
        val request = launcherApps?.getPinItemRequest(intent)
        if (request == null ||
            !request.isValid ||
            request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        ) {
            finish()
            return
        }
        val accepted = runCatching { request.accept() }.getOrDefault(false)
        Log.d(TAG, "pin shortcut accepted=$accepted")
        if (accepted) {
            request.shortcutInfo?.let { WindowsShortcutSync.addAsync(applicationContext, it) }
        }
        finish()
    }

    private companion object {
        const val TAG = "ShortcutPin"
    }
}