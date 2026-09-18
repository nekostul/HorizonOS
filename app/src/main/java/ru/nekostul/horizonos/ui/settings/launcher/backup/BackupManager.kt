package ru.nekostul.horizonos.ui.settings.launcher.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.nekostul.horizonos.ui.games.GameFolderRepository
import ru.nekostul.horizonos.ui.games.GameRepository
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperRepository
import ru.nekostul.horizonos.ui.user.UserProfileRepository

enum class BackupSection {
    HOME_SCREEN,
    SETTINGS,
    API_KEYS,
    PROFILE,
    GAME_FOLDERS
}

object BackupManager {

    private const val FORMAT_VERSION = 1

    suspend fun createBackup(
        context: Context,
        uri: Uri,
        sections: Set<BackupSection>
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", FORMAT_VERSION)
            root.put("timestamp", System.currentTimeMillis())

            val launcherRepository = LauncherSettingsRepository(context)
            val scraperRepository = ScraperRepository(context)
            val gameRepository = GameRepository(context)
            val profileRepository = UserProfileRepository(context)
            val folderRepository = GameFolderRepository(context)

            val data = JSONObject()

            if (BackupSection.SETTINGS in sections) {
                data.put("launcher_settings", launcherRepository.settings.first().toJson())
            }
            if (BackupSection.API_KEYS in sections) {
                data.put("scraper_settings", scraperRepository.load().toJson())
            }
            if (BackupSection.HOME_SCREEN in sections) {
                data.put("games", gameRepository.games.first().gamesToJson())
            }
            if (BackupSection.PROFILE in sections) {
                val profile = profileRepository.profile.value
                data.put(
                    "user_profile",
                    JSONObject()
                        .put("nick", profile.nick)
                        .put("avatar", profile.avatarPath ?: "")
                )
            }
            if (BackupSection.GAME_FOLDERS in sections) {
                data.put("game_folders", folderRepository.load().foldersToJson())
            }

            root.put("data", data)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(root.toString().toByteArray(Charsets.UTF_8))
                stream.flush()
            } != null
        }.getOrDefault(false)
    }

    suspend fun restoreBackup(
        context: Context,
        uri: Uri,
        sections: Set<BackupSection>
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext false

            val root = JSONObject(content)
            val data = root.optJSONObject("data") ?: return@withContext false

            val launcherRepository = LauncherSettingsRepository(context)
            val scraperRepository = ScraperRepository(context)
            val gameRepository = GameRepository(context)
            val profileRepository = UserProfileRepository(context)
            val folderRepository = GameFolderRepository(context)

            if (BackupSection.SETTINGS in sections) {
                data.optJSONObject("launcher_settings")?.let { json ->
                    launcherRepository.restore(json)
                }
            }
            if (BackupSection.API_KEYS in sections) {
                data.optJSONObject("scraper_settings")?.let { json ->
                    scraperRepository.restore(json)
                }
            }
            if (BackupSection.HOME_SCREEN in sections) {
                data.optJSONArray("games")?.let { json ->
                    gameRepository.restore(json)
                }
            }
            if (BackupSection.PROFILE in sections) {
                data.optJSONObject("user_profile")?.let { json ->
                    profileRepository.restore(json)
                }
            }
            if (BackupSection.GAME_FOLDERS in sections) {
                data.optJSONArray("game_folders")?.let { json ->
                    folderRepository.restore(json)
                }
            }

            if (BackupSection.HOME_SCREEN in sections) {
                ScanCoordinator.init(context)
                ScanCoordinator.enqueueAll()
            }

            true
        }.getOrDefault(false)
    }

    private fun ru.nekostul.horizonos.ui.settings.LauncherSettings.toJson(): JSONObject =
        JSONObject()
            .put("firstSetupCompleted", firstSetupCompleted)
            .put("animations", animations)
            .put("interfaceScale", interfaceScale)
            .put("showClock", showClock)
            .put("showBattery", showBattery)
            .put("confirmGameLaunch", confirmGameLaunch)
            .put("rememberLastGame", rememberLastGame)
            .put("interfaceSounds", interfaceSounds)
            .put("soundMode", soundMode)
            .put("backgroundMusicEnabled", backgroundMusicEnabled)
            .put("backgroundMusicVolume", backgroundMusicVolume)
            .put("hapticFeedbackEnabled", hapticFeedbackEnabled)
            .put("theme", theme)
            .put("language", language)
            .put("accentColor", accentColor)
            .put("airplaneMode", airplaneMode)
            .put("airplaneWifiAllowed", airplaneWifiAllowed)
            .put("airplaneBluetoothAllowed", airplaneBluetoothAllowed)
            .put("autoBrightness", autoBrightness)
            .put("brightness", brightness)
            .put("lockScreenEnabled", lockScreenEnabled)
            .put("lockScreenTimeoutMinutes", lockScreenTimeoutMinutes)
            .put("wifiEnabled", wifiEnabled)
            .put("notificationsEnabled", notificationsEnabled)
            .put("sleepEnabled", sleepEnabled)
            .put("sleepTimeoutMinutes", sleepTimeoutMinutes)
            .put("sleepMediaEnabled", sleepMediaEnabled)
            .put("vibrationEnabled", vibrationEnabled)
            .put("controllerSensitivity", controllerSensitivity)
            .put("controllerDeadZone", controllerDeadZone)
            .put("screenshotBackgroundEnabled", screenshotBackgroundEnabled)
            .put("rootAccessGranted", rootAccessGranted)

    private fun ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSettings.toJson(): JSONObject =
        JSONObject()
            .put("enabled", JSONObject().apply {
                enabled.forEach { (id, value) -> put(id.storageKey, value) }
            })
            .put("screenScraperDevId", screenScraperDevId)
            .put("screenScraperDevPassword", screenScraperDevPassword)
            .put("screenScraperSoftName", screenScraperSoftName)
            .put("screenScraperSsid", screenScraperSsid)
            .put("screenScraperSspassword", screenScraperSspassword)
            .put("theGamesDbApiKey", theGamesDbApiKey)
            .put("igdbClientId", igdbClientId)
            .put("igdbClientSecret", igdbClientSecret)
            .put("steamGridDbApiKey", steamGridDbApiKey)

    private fun List<ru.nekostul.horizonos.ui.games.Game>.gamesToJson(): JSONArray {
        val games = this
        return JSONArray().apply {
            games.forEach { game ->
                put(
                    JSONObject().apply {
                        put("id", game.id)
                        put("title", game.title)
                        put("platform", game.platform.name)
                        put("emulator", game.emulator.name)
                        put("romUri", game.romUri)
                        put("romName", game.romName)
                        put("hidden", game.hidden)
                        game.fullTitle?.let { put("fullTitle", it) }
                        game.coverPath?.let { put("coverPath", it) }
                        game.screenshotPath?.let { put("screenshotPath", it) }
                        put("isTitleManuallySet", game.isTitleManuallySet)
                        put("isCoverManuallySet", game.isCoverManuallySet)
                        put("isScreenshotManuallySet", game.isScreenshotManuallySet)
                        game.packageName?.let { put("packageName", it) }
                        game.launchActivity?.let { put("launchActivity", it) }
                        game.iconPath?.let { put("iconPath", it) }
                    }
                )
            }
        }
    }

    private fun List<GameFolderRepository.Folder>.foldersToJson(): JSONArray {
        val folders = this
        return JSONArray().apply {
            folders.forEach { folder ->
                put(
                    JSONObject().apply {
                        put("path", folder.path)
                        put("name", folder.name)
                        put("platform", folder.platform.name)
                        put("emulator", folder.emulator.name)
                    }
                )
            }
        }
    }

    private suspend fun LauncherSettingsRepository.restore(json: JSONObject) {
        setAnimations(json.optBoolean("animations", true))
        setInterfaceScale(json.optDouble("interfaceScale", 1.0).toFloat())
        setShowClock(json.optBoolean("showClock", true))
        setShowBattery(json.optBoolean("showBattery", true))
        setConfirmGameLaunch(json.optBoolean("confirmGameLaunch", true))
        setRememberLastGame(json.optBoolean("rememberLastGame", true))
        setInterfaceSounds(json.optBoolean("interfaceSounds", true))
        setSoundMode(json.optString("soundMode", "all"))
        setBackgroundMusicEnabled(json.optBoolean("backgroundMusicEnabled", true))
        setBackgroundMusicVolume(json.optDouble("backgroundMusicVolume", 0.65).toFloat())
        setHapticFeedbackEnabled(json.optBoolean("hapticFeedbackEnabled", true))
        setTheme(json.optString("theme", "dark"))
        setLanguage(json.optString("language", "system"))
        setAccentColor(json.optString("accentColor", "cyan"))
        setAirplaneMode(json.optBoolean("airplaneMode", false))
        setAirplaneWifiAllowed(json.optBoolean("airplaneWifiAllowed", false))
        setAirplaneBluetoothAllowed(json.optBoolean("airplaneBluetoothAllowed", false))
        setAutoBrightness(json.optBoolean("autoBrightness", false))
        setBrightness(json.optDouble("brightness", 0.7).toFloat())
        setLockScreenEnabled(json.optBoolean("lockScreenEnabled", true))
        setLockScreenTimeoutMinutes(json.optInt("lockScreenTimeoutMinutes", 5))
        setWifiEnabled(json.optBoolean("wifiEnabled", true))
        setNotificationsEnabled(json.optBoolean("notificationsEnabled", true))
        setSleepEnabled(json.optBoolean("sleepEnabled", true))
        setSleepTimeoutMinutes(json.optInt("sleepTimeoutMinutes", 10))
        setSleepMediaEnabled(json.optBoolean("sleepMediaEnabled", true))
        setVibrationEnabled(json.optBoolean("vibrationEnabled", true))
        setControllerSensitivity(json.optDouble("controllerSensitivity", 1.0).toFloat())
        setControllerDeadZone(json.optDouble("controllerDeadZone", 0.15).toFloat())
        setScreenshotBackgroundEnabled(json.optBoolean("screenshotBackgroundEnabled", true))
        setRootAccessGranted(json.optBoolean("rootAccessGranted", false))
    }

    private fun ScraperRepository.restore(json: JSONObject) {
        json.optJSONObject("enabled")?.let { enabledJson ->
            ru.nekostul.horizonos.ui.settings.launcher.scanning.ScraperSourceId.entries.forEach { id ->
                if (enabledJson.has(id.storageKey)) {
                    setEnabled(id, enabledJson.optBoolean(id.storageKey, true))
                }
            }
        }
        setScreenScraperCredentials(
            json.optString("screenScraperDevId", ""),
            json.optString("screenScraperDevPassword", ""),
            json.optString("screenScraperSoftName", "HorizonOS")
        )
        setScreenScraperSession(
            json.optString("screenScraperSsid", ""),
            json.optString("screenScraperSspassword", "")
        )
        setTheGamesDbApiKey(json.optString("theGamesDbApiKey", ""))
        setIgdbCredentials(
            json.optString("igdbClientId", ""),
            json.optString("igdbClientSecret", "")
        )
        setSteamGridDbApiKey(json.optString("steamGridDbApiKey", ""))
    }

    private suspend fun GameRepository.restore(json: JSONArray) {
        val games = buildList {
            for (index in 0 until json.length()) {
                val item = json.optJSONObject(index) ?: continue
                val platform = item.optString("platform")
                    .let { value -> runCatching { ru.nekostul.horizonos.ui.games.Platform.valueOf(value) }.getOrNull() }
                    ?: continue
                val emulator = item.optString("emulator")
                    .let { value -> runCatching { ru.nekostul.horizonos.ui.games.Emulator.valueOf(value) }.getOrNull() }
                    ?: continue
                val uri = item.optString("romUri")
                if (uri.isBlank()) continue
                add(
                    ru.nekostul.horizonos.ui.games.Game(
                        id = item.optString("id").ifBlank {
                            ru.nekostul.horizonos.ui.games.Game.fromRom(
                                title = item.optString("title").ifBlank { uri },
                                platform = platform,
                                emulator = emulator,
                                romUri = uri,
                                romName = item.optString("romName").ifBlank { uri }
                            ).id
                        },
                        title = item.optString("title").ifBlank { item.optString("romName").ifBlank { uri } },
                        platform = platform,
                        emulator = emulator,
                        romUri = uri,
                        romName = item.optString("romName").ifBlank { uri },
                        hidden = item.optBoolean("hidden", false),
                        fullTitle = item.optString("fullTitle").takeIf { it.isNotBlank() },
                        coverPath = null,
                        screenshotPath = null,
                        isTitleManuallySet = item.optBoolean("isTitleManuallySet", false),
                        isCoverManuallySet = false,
                        isScreenshotManuallySet = false,
                        packageName = item.optString("packageName").takeIf { it.isNotBlank() },
                        launchActivity = item.optString("launchActivity").takeIf { it.isNotBlank() },
                        iconPath = item.optString("iconPath").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
        if (games.isNotEmpty()) addAll(games)
    }

    private fun UserProfileRepository.restore(json: JSONObject) {
        json.optString("nick").takeIf { it.isNotBlank() }?.let { setNick(it) }
        json.optString("avatar").takeIf { it.isNotBlank() }?.let { setAvatar(it) }
    }

    private fun GameFolderRepository.restore(json: JSONArray) {
        clear()
        for (index in 0 until json.length()) {
            val item = json.optJSONObject(index) ?: continue
            val platform = item.optString("platform")
                .let { value -> runCatching { ru.nekostul.horizonos.ui.games.Platform.valueOf(value) }.getOrNull() }
                ?: continue
            val emulator = item.optString("emulator")
                .let { value -> runCatching { ru.nekostul.horizonos.ui.games.Emulator.valueOf(value) }.getOrNull() }
                ?: continue
            val path = item.optString("path").takeIf { it.isNotBlank() } ?: continue
            remember(
                GameFolderRepository.Folder(
                    path = path,
                    name = item.optString("name").ifBlank { path },
                    platform = platform,
                    emulator = emulator
                )
            )
        }
    }
}
