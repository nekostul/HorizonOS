package ru.nekostul.horizonos.ui.games

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator

object DownloadTracker {

    data class DownloadingApp(
        val packageName: String,
        val label: String,
        val iconPath: String?,
        val progress: Float
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloading = MutableStateFlow<List<DownloadingApp>>(emptyList())
    val downloading: StateFlow<List<DownloadingApp>> = _downloading.asStateFlow()

    private var appContext: Context? = null
    @Volatile
    private var enabled = true
    private var callbackRegistered = false

    private val sessions = mutableMapOf<Int, DownloadingApp>()

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        registerCallback(ctx)
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!value) {
            sessions.clear()
            _downloading.value = emptyList()
        }
    }

    private fun registerCallback(ctx: Context) {
        if (callbackRegistered) return
        callbackRegistered = true
        val installer = runCatching { ctx.packageManager.packageInstaller }.getOrNull() ?: return
        installer.registerSessionCallback(
            object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {
                    handleSessionUpdate(sessionId)
                }

                override fun onBadgingChanged(sessionId: Int) {
                    handleSessionUpdate(sessionId)
                }

                override fun onActiveChanged(sessionId: Int, active: Boolean) = Unit

                override fun onProgressChanged(sessionId: Int, progress: Float) {
                    handleProgress(sessionId, progress)
                }

                override fun onFinished(sessionId: Int, success: Boolean) {
                    handleFinished(sessionId, success)
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

    private fun handleSessionUpdate(sessionId: Int) {
        if (!enabled) return
        val ctx = appContext ?: return
        val info = runCatching {
            ctx.packageManager.packageInstaller.getSessionInfo(sessionId)
        }.getOrNull() ?: return

        val packageName = info.appPackageName ?: return
        if (packageName.isBlank()) return

        val installerPackage = info.installerPackageName
        if (installerPackage != null && installerPackage != "com.android.vending") return

        val existing = sessions[sessionId]
        val iconPath = existing?.iconPath ?: info.appIcon?.let { persistIcon(packageName, it) }
        val label = (info.appLabel?.toString()?.takeIf { it.isNotBlank() })
            ?: existing?.label ?: packageName

        sessions[sessionId] = DownloadingApp(
            packageName = packageName,
            label = label,
            iconPath = iconPath,
            progress = existing?.progress ?: info.progress
        )
        publish()
    }

    private fun handleProgress(sessionId: Int, progress: Float) {
        if (!enabled) return
        val existing = sessions[sessionId] ?: return
        sessions[sessionId] = existing.copy(progress = progress.coerceIn(0f, 1f))
        publish()
    }

    private fun handleFinished(sessionId: Int, success: Boolean) {
        if (!enabled) return
        val app = sessions.remove(sessionId) ?: return
        publish()

        val ctx = appContext ?: return
        if (!success) return

        scope.launch {
            val library = GameLibrary(ctx)
            val existing = runCatching { library.games.first() }
                .getOrDefault(emptyList())
                .firstOrNull { it.packageName == app.packageName }

            val isGame = isGame(ctx, app.packageName)
            if (!isGame) {
                if (existing != null) library.remove(existing)
                return@launch
            }

            if (existing != null) {
                library.update(
                    existing.copy(
                        hidden = false,
                        launchActivity = existing.launchActivity ?: launchActivity(ctx, app.packageName),
                        iconPath = app.iconPath ?: existing.iconPath
                    )
                )
                return@launch
            }

            val label = resolveAppLabel(ctx, app.packageName) ?: app.label
            val game = Game.fromAndroidApp(
                label,
                app.packageName,
                launchActivity(ctx, app.packageName),
                app.iconPath
            ).copy(fromDownload = true)
            library.add(game)
            ScanCoordinator.init(ctx)
            ScanCoordinator.enqueue(listOf(game))
        }
    }

    private fun resolveAppLabel(ctx: Context, packageName: String): String? = runCatching {
        val info = ctx.packageManager.getApplicationInfo(packageName, 0)
        ctx.packageManager.getApplicationLabel(info)?.toString()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun launchActivity(ctx: Context, packageName: String): String? = runCatching {
        ctx.packageManager.getLaunchIntentForPackage(packageName)?.component?.className
    }.getOrNull()

    private fun isGame(ctx: Context, packageName: String): Boolean = runCatching {
        val info = ctx.packageManager.getApplicationInfo(packageName, 0)
        info.category == ApplicationInfo.CATEGORY_GAME
    }.getOrDefault(false)

    private fun persistIcon(packageName: String, bitmap: Bitmap): String? {
        val ctx = appContext ?: return null
        val dir = File(ctx.filesDir, "app_icons").apply { mkdirs() }
        val file = File(dir, "$packageName.png")
        return runCatching {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun publish() {
        _downloading.value = sessions.values.toList()
    }
}