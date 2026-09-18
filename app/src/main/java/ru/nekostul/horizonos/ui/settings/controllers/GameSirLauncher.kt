package ru.nekostul.horizonos.ui.settings.controllers

import android.content.Context
import android.content.Intent
import android.net.Uri

object GameSirLauncher {
    private val packages = listOf(
        "com.xiaoji.xtouch.google",
        "com.xiaoji.gamemiracle",
        "com.gamesir.virtualtouchutil",
        "com.gamesir"
    )
    private const val playPackage = "com.xiaoji.xtouch.google"

    fun open(context: Context): Boolean {
        val launchIntent = packages.firstNotNullOfOrNull { packageName ->
            runCatching { context.packageManager.getLaunchIntentForPackage(packageName) }.getOrNull()
        } ?: return false
        return runCatching { context.startActivity(launchIntent) }.isSuccess
    }

    fun openStore(context: Context) {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$playPackage")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$playPackage")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(market) }
            .recoverCatching { context.startActivity(web) }
    }
}
