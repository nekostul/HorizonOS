package ru.nekostul.horizonos.ui.user

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val nick: String,
    val avatarPath: String?
) {
    val configuredNick: String?
        get() = nick.takeIf { it.isNotBlank() && it != UserProfileRepository.DEFAULT_NICK }
}

class UserProfileRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    val profile: StateFlow<UserProfile> = profileStore.asStateFlow()

    init {
        profileStore.value = read()
    }

    fun setNick(value: String) {
        prefs.edit().putString(KEY_NICK, value.trim()).apply()
        profileStore.value = read()
    }

    fun setAvatar(path: String) {
        prefs.edit().putString(KEY_AVATAR, path).apply()
        profileStore.value = read()
    }

    private fun read(): UserProfile = UserProfile(
        nick = prefs.getString(KEY_NICK, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_NICK,
        avatarPath = prefs.getString(KEY_AVATAR, null)?.takeIf { it.isNotBlank() }
    )

    companion object {
        const val DEFAULT_NICK = "HorizonOS"
        private const val KEY_NICK = "nick"
        private const val KEY_AVATAR = "avatar"
        private val profileStore = MutableStateFlow(UserProfile(DEFAULT_NICK, null))
    }
}
