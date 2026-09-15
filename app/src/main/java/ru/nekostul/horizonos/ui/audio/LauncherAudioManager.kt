package ru.nekostul.horizonos.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSoundMode
import kotlin.math.max

enum class LauncherInputSource {
    TOUCH,
    GAMEPAD
}

enum class LauncherSound(val resourceId: Int) {
    CLICK(R.raw.click),
    CLICK_CONFIRM(R.raw.click_confirm),
    OPEN_GAME(R.raw.open_game),
    HINT(R.raw.podskazka),
    BACK(R.raw.back),
    ERROR_GAME_OPEN(R.raw.error_game_open)
}

/** Centralized launcher audio and native-style haptic feedback. */
object LauncherAudioManager {
    private const val CROSSFADE_MILLIS = 8_000L
    private const val CONFIRM_DELAY_MILLIS = 90L
    private val LAUNCH_VIBRATION_TIMINGS = longArrayOf(
        0L, 26L, 55L, 30L, 90L, 34L, 125L, 42L, 150L, 55L, 180L, 80L, 200L, 120L
    )
    private val LAUNCH_VIBRATION_AMPLITUDES = intArrayOf(
        0, 45, 0, 55, 0, 65, 0, 80, 0, 95, 0, 120, 0, 160
    )

    private val handler = Handler(Looper.getMainLooper())
    private val soundIds = mutableMapOf<LauncherSound, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val pendingSounds = ArrayDeque<LauncherSound>()

    private var appContext: Context? = null
    private var soundPool: SoundPool? = null
    private var soundMode = LauncherSoundMode.ALL
    private var hapticEnabled = true
    private var musicEnabled = true
    private var musicVolume = 0.65f
    private var settingsReady = false
    private var foreground = false
    private var currentMusic: MediaPlayer? = null
    private var nextMusic: MediaPlayer? = null
    private var pendingConfirm: Runnable? = null
    private var musicGeneration = 0L
    private var lastPlayedSound: LauncherSound? = null
    private var lastPlayedSoundAt = 0L
    private var lastHapticAt = 0L

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setAudioAttributes(attributes)
            .setMaxStreams(6)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status != 0) return@setOnLoadCompleteListener
                    loadedSoundIds += sampleId
                    val queued = pendingSounds.toList()
                    pendingSounds.clear()
                    queued.forEach { queuedSound ->
                        if (soundIds[queuedSound] == sampleId) {
                            playLoaded(queuedSound)
                        } else {
                            pendingSounds.addLast(queuedSound)
                        }
                    }
                }
                LauncherSound.entries.forEach { sound ->
                    soundIds[sound] = pool.load(appContext!!, sound.resourceId, 1)
                }
            }
    }

    fun updateSettings(context: Context, settings: LauncherSettings) {
        initialize(context)
        val wasMusicEnabled = musicEnabled
        soundMode = settings.soundMode
        hapticEnabled = settings.hapticFeedbackEnabled
        musicEnabled = settings.backgroundMusicEnabled
        musicVolume = settings.backgroundMusicVolume.coerceIn(0f, 1f)
        settingsReady = true

        if (!foreground) return
        if (!musicEnabled) {
            stopMusic()
        } else if (!wasMusicEnabled || currentMusic == null) {
            startMusicFromBeginning()
        } else {
            currentMusic?.setVolume(musicVolume, musicVolume)
        }
    }

    fun onForeground(context: Context) {
        initialize(context)
        foreground = true
        if (settingsReady && musicEnabled && currentMusic == null) {
            startMusicFromBeginning()
        }
    }

    fun onBackground() {
        foreground = false
        stopMusic()
    }

    fun play(sound: LauncherSound, source: LauncherInputSource = LauncherInputSource.GAMEPAD) {
        if (soundMode == LauncherSoundMode.OFF ||
            (soundMode == LauncherSoundMode.GAMEPAD_ONLY && source != LauncherInputSource.GAMEPAD)
        ) {
            return
        }
        val now = SystemClock.uptimeMillis()
        if (lastPlayedSound == sound && now - lastPlayedSoundAt < 35L) return
        lastPlayedSound = sound
        lastPlayedSoundAt = now
        val soundId = soundIds[sound] ?: return
        if (soundId !in loadedSoundIds) {
            if (pendingSounds.size >= 8) pendingSounds.removeFirst()
            pendingSounds.remove(sound)
            pendingSounds.addLast(sound)
            return
        }
        playLoaded(sound)
    }

    /** Delays confirmation very slightly so a game launch can replace it with open_game. */
    fun playConfirm(source: LauncherInputSource) {
        pendingConfirm?.let(handler::removeCallbacks)
        val action = Runnable {
            pendingConfirm = null
            play(LauncherSound.CLICK_CONFIRM, source)
        }
        pendingConfirm = action
        handler.postDelayed(action, CONFIRM_DELAY_MILLIS)
    }

    fun playOpenGame(source: LauncherInputSource) {
        cancelPendingConfirm()
        play(LauncherSound.OPEN_GAME, source)
    }

    fun playGameError(source: LauncherInputSource) {
        cancelPendingConfirm()
        play(LauncherSound.ERROR_GAME_OPEN, source)
    }

    fun playHint(source: LauncherInputSource) {
        cancelPendingConfirm()
        play(LauncherSound.HINT, source)
    }

    fun performHapticFeedback(view: View) {
        if (hapticEnabled) {
            val now = SystemClock.uptimeMillis()
            if (now - lastHapticAt < 35L) return
            lastHapticAt = now
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** Rising native vibration pattern aligned with the 1.4 s game-launch zoom animation. */
    fun playLaunchVibration() {
        if (!hapticEnabled) return
        runCatching {
            val vibrator = appContext?.getSystemService(Vibrator::class.java) ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    LAUNCH_VIBRATION_TIMINGS,
                    LAUNCH_VIBRATION_AMPLITUDES,
                    -1
                )
            )
        }
    }

    private fun playLoaded(sound: LauncherSound) {
        val soundId = soundIds[sound] ?: return
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun cancelPendingConfirm() {
        pendingConfirm?.let(handler::removeCallbacks)
        pendingConfirm = null
    }

    private fun startMusicFromBeginning() {
        val context = appContext ?: return
        stopMusic()
        if (!foreground || !settingsReady || !musicEnabled) return

        val player = createMusicPlayer(context) ?: return
        val generation = musicGeneration
        currentMusic = player
        player.setVolume(0f, 0f)
        player.start()
        ramp(player, 0f, musicVolume, CROSSFADE_MILLIS, generation)
        scheduleCrossfade(player, generation)
    }

    private fun createMusicPlayer(context: Context): MediaPlayer? = runCatching {
        MediaPlayer.create(context, R.raw.horizon_background)?.apply {
            setVolume(0f, 0f)
        }
    }.getOrNull()

    private fun scheduleCrossfade(player: MediaPlayer, generation: Long) {
        val delay = max(0L, player.duration.toLong() - CROSSFADE_MILLIS)
        handler.postDelayed({ beginCrossfade(player, generation) }, delay)
    }

    private fun beginCrossfade(oldPlayer: MediaPlayer, generation: Long) {
        if (generation != musicGeneration || currentMusic !== oldPlayer || !foreground || !musicEnabled) {
            return
        }
        val context = appContext ?: return
        val newPlayer = createMusicPlayer(context) ?: run {
            oldPlayer.setOnCompletionListener {
                if (generation == musicGeneration && foreground && musicEnabled) {
                    it.seekTo(0)
                    it.start()
                    scheduleCrossfade(it, generation)
                }
            }
            return
        }

        nextMusic = newPlayer
        newPlayer.setVolume(0f, 0f)
        newPlayer.start()
        ramp(oldPlayer, musicVolume, 0f, CROSSFADE_MILLIS, generation)
        ramp(newPlayer, 0f, musicVolume, CROSSFADE_MILLIS, generation) {
            if (generation != musicGeneration || currentMusic !== oldPlayer) return@ramp
            runCatching { oldPlayer.stop() }
            oldPlayer.release()
            currentMusic = newPlayer
            nextMusic = null
            scheduleCrossfade(newPlayer, generation)
        }
    }

    private fun ramp(
        player: MediaPlayer,
        from: Float,
        to: Float,
        duration: Long,
        generation: Long,
        onFinished: () -> Unit = {}
    ) {
        val startedAt = SystemClock.uptimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                if (generation != musicGeneration) return
                val progress = ((SystemClock.uptimeMillis() - startedAt).toFloat() / duration)
                    .coerceIn(0f, 1f)
                val value = from + (to - from) * progress
                runCatching { player.setVolume(value, value) }
                if (progress < 1f) {
                    handler.postDelayed(this, 40L)
                } else {
                    onFinished()
                }
            }
        }
        handler.post(runnable)
    }

    private fun stopMusic() {
        musicGeneration++
        handler.removeCallbacksAndMessages(null)
        cancelPendingConfirm()
        listOf(currentMusic, nextMusic).forEach { player ->
            player ?: return@forEach
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        currentMusic = null
        nextMusic = null
    }
}
