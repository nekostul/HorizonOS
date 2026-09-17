package ru.nekostul.horizonos.ui.onboarding

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import ru.nekostul.horizonos.R

object OnboardingAudioManager {
    private const val TRANSITION_FADE_MILLIS = 700L
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var fadeRunnable: Runnable? = null
    private var playedForSession = false

    @Synchronized
    fun playOnce(context: Context) {
        if (playedForSession || player != null) return
        playedForSession = true
        player = createOneShot(context, R.raw.onboarding_boot)
    }

    @Synchronized
    fun playAfterSettings(context: Context): Long {
        fadeRunnable?.let(handler::removeCallbacks)
        fadeRunnable = null
        val previous = player
        val created = createOneShot(context, R.raw.after_settings, startImmediately = false)
        player = created
        val duration = created?.duration?.toLong() ?: 0L
        if (previous == null) {
            created?.start()
            return duration
        }

        val startedAt = android.os.SystemClock.uptimeMillis()
        val fade = object : Runnable {
            override fun run() {
                val progress = ((android.os.SystemClock.uptimeMillis() - startedAt).toFloat() /
                    TRANSITION_FADE_MILLIS).coerceIn(0f, 1f)
                val volume = 1f - progress
                runCatching { previous.setVolume(volume, volume) }
                if (progress < 1f) {
                    handler.postDelayed(this, 32L)
                } else {
                    runCatching { previous.stop() }
                    runCatching { previous.release() }
                    synchronized(this@OnboardingAudioManager) {
                        fadeRunnable = null
                        if (player === created) created?.start()
                    }
                }
            }
        }
        fadeRunnable = fade
        handler.post(fade)
        return duration + TRANSITION_FADE_MILLIS
    }

    @Synchronized
    fun stop() {
        fadeRunnable?.let(handler::removeCallbacks)
        fadeRunnable = null
        player?.let { current ->
            runCatching { current.stop() }
            runCatching { current.release() }
        }
        player = null
    }

    private fun createOneShot(
        context: Context,
        resourceId: Int,
        startImmediately: Boolean = true
    ): MediaPlayer? = runCatching {
        MediaPlayer.create(context.applicationContext, resourceId)?.apply {
            isLooping = false
            setOnCompletionListener { completed ->
                runCatching { completed.release() }
                synchronized(this@OnboardingAudioManager) {
                    if (player === completed) player = null
                }
            }
            if (startImmediately) start()
        }
    }.getOrNull()
}
