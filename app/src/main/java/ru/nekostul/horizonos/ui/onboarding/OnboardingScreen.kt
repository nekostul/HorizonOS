package ru.nekostul.horizonos.ui.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.HorizonNavigation
import ru.nekostul.horizonos.ui.HorizonButtonGlyph
import ru.nekostul.horizonos.ui.isHorizonConfirmKey
import ru.nekostul.horizonos.ui.audio.LauncherAudioManager
import ru.nekostul.horizonos.ui.audio.LauncherInputSource
import ru.nekostul.horizonos.ui.audio.LauncherSound
import ru.nekostul.horizonos.ui.games.GamesScreen
import ru.nekostul.horizonos.ui.keyboard.HorizonKeyboardDialog
import ru.nekostul.horizonos.ui.keyboard.KeyboardLanguage
import ru.nekostul.horizonos.ui.settings.LauncherSettings
import ru.nekostul.horizonos.ui.settings.LauncherSettingsRepository
import ru.nekostul.horizonos.ui.settings.LanguageManager
import ru.nekostul.horizonos.ui.settings.SettingsInputMode
import ru.nekostul.horizonos.ui.settings.LocalSettingsInputMode
import ru.nekostul.horizonos.ui.settings.launcher.scanning.ScanCoordinator
import ru.nekostul.horizonos.ui.theme.LocalHorizonColors
import ru.nekostul.horizonos.ui.user.UserProfileRepository

private fun onboardingDirectionalKey(event: androidx.compose.ui.input.key.KeyEvent): Key? {
    val composeKey = event.key
    if (composeKey == Key.DirectionUp || composeKey == Key.DirectionDown ||
        composeKey == Key.DirectionLeft || composeKey == Key.DirectionRight
    ) {
        return composeKey
    }
    return when (event.nativeKeyEvent.keyCode) {
        android.view.KeyEvent.KEYCODE_DPAD_UP -> Key.DirectionUp
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> Key.DirectionDown
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> Key.DirectionLeft
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> Key.DirectionRight
        else -> null
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    settings: LauncherSettings,
    permissionRevision: Int,
    onRequestPermissions: (Array<String>) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { LauncherSettingsRepository(context) }
    val profileRepository = remember { UserProfileRepository(context) }
    val profile by profileRepository.profile.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val permissionSteps = remember { onboardingPermissionSteps() }
    val initialLanguageIndex = remember {
        if (LanguageManager.effectiveLanguage(context, settings.language) == LanguageManager.RUSSIAN) 0 else 1
    }
    val initialThemeIndex = remember { if (settings.theme == "light") 1 else 0 }
    var state by remember {
        mutableStateOf(
            OnboardingUiState(
                languageIndex = initialLanguageIndex,
                themeIndex = initialThemeIndex
            )
        )
    }
    var nick by remember(profile.nick) {
        mutableStateOf(profile.configuredNick.orEmpty())
    }
    var showKeyboard by remember { mutableStateOf(false) }
    var permissionRequestRevision by remember { mutableIntStateOf(permissionRevision) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(state.page) {
        if (state.page == OnboardingPage.INTRO) {
            delay(6_300)
            if (state.page == OnboardingPage.INTRO) {
                state = state.copy(page = OnboardingPage.LANGUAGE)
            }
        }
    }

    LaunchedEffect(state.page) {
        if (state.page == OnboardingPage.PERMISSIONS) {
            permissionRequestRevision = permissionRevision
            if (state.permissionIndex != 0 || state.permissionAwaitingReturn) {
                state = state.copy(
                    permissionIndex = 0,
                    permissionFocus = 0,
                    permissionAwaitingReturn = false
                )
            }
        }
        if (state.page == OnboardingPage.FINISHING) {
            val trackDuration = OnboardingAudioManager.playAfterSettings(context)
            delay(maxOf(1_700L, trackDuration + 120L))
            settingsRepository.setFirstSetupCompleted(true)
        }
    }

    fun currentPermission(): OnboardingPermissionStep? = permissionSteps.getOrNull(state.permissionIndex)

    fun advancePermission() {
        val next = ((state.permissionIndex + 1) until permissionSteps.size)
            .firstOrNull { index -> !permissionSteps[index].isGranted(context) }
        state = if (next == null) {
            state.copy(
                page = OnboardingPage.GAMES,
                gamesFocus = 0,
                permissionAwaitingReturn = false
            )
        } else {
            state.copy(
                permissionIndex = next,
                permissionFocus = 0,
                permissionAwaitingReturn = false
            )
        }
    }

    LaunchedEffect(
        state.page,
        permissionRevision,
        state.permissionIndex,
        state.permissionAwaitingReturn
    ) {
        if (state.page != OnboardingPage.PERMISSIONS ||
            !state.permissionAwaitingReturn ||
            permissionRevision <= permissionRequestRevision
        ) {
            return@LaunchedEffect
        }

        state = state.copy(permissionAwaitingReturn = false)
        if (currentPermission()?.isGranted(context) == true) {
            advancePermission()
        }
    }

    LaunchedEffect(
        state.page,
        state.languageIndex,
        state.profileFocus,
        state.themeIndex,
        state.permissionIndex,
        state.permissionFocus,
        state.gamesFocus,
        state.tutorialIndex,
        state.openSourceFocus,
        state.audioFocus,
        showKeyboard,
        state.gamesDialogOpen
    ) {
        if (showKeyboard || state.gamesDialogOpen) return@LaunchedEffect
        delay(70)
        focusRequester.requestFocus()
    }

    fun playMove() {
        LauncherAudioManager.play(LauncherSound.CLICK, LauncherInputSource.GAMEPAD)
        LauncherAudioManager.performHapticFeedback(view)
    }

    fun selectLanguage(index: Int) {
        state = state.copy(languageIndex = index)
        scope.launch {
            settingsRepository.setLanguage(if (index == 0) LanguageManager.RUSSIAN else LanguageManager.ENGLISH)
        }
    }

    fun selectTheme(index: Int) {
        state = state.copy(themeIndex = index)
        scope.launch { settingsRepository.setTheme(if (index == 0) "dark" else "light") }
    }

    fun continueFromProfile() {
        if (nick.isBlank()) return
        profileRepository.setNick(nick.trim())
        state = state.copy(page = OnboardingPage.THEME, themeIndex = if (settings.theme == "light") 1 else 0)
    }

    fun requestCurrentPermission() {
        val step = currentPermission() ?: return
        if (step.isGranted(context)) {
            advancePermission()
            return
        }

        permissionRequestRevision = permissionRevision
        state = state.copy(permissionAwaitingReturn = true)
        if (step.allFilesAccess) {
            ru.nekostul.horizonos.ui.files.StorageAccess.requestAllFilesAccess(context)
        } else {
            val permissions = step.permissions.ifEmpty { listOfNotNull(step.permission) }
            if (permissions.isNotEmpty()) {
                onRequestPermissions(permissions.toTypedArray())
            }
        }
    }

    fun moveBack(): Boolean {
        when (state.page) {
            OnboardingPage.INTRO -> return true
            OnboardingPage.LANGUAGE -> state = state.copy(page = OnboardingPage.INTRO)
            OnboardingPage.PROFILE -> state = state.copy(page = OnboardingPage.LANGUAGE)
            OnboardingPage.THEME -> state = state.copy(page = OnboardingPage.PROFILE, profileFocus = 0)
            OnboardingPage.PERMISSIONS -> state = state.copy(page = OnboardingPage.THEME)
            OnboardingPage.GAMES -> state = state.copy(page = OnboardingPage.PERMISSIONS, permissionIndex = 0, permissionFocus = 0)
            OnboardingPage.TUTORIAL -> state = state.copy(page = OnboardingPage.GAMES, gamesFocus = if (state.hasAddedGames) 1 else 0)
            OnboardingPage.OPEN_SOURCE -> state = state.copy(page = OnboardingPage.TUTORIAL, tutorialIndex = 3)
            OnboardingPage.AUDIO -> state = state.copy(page = OnboardingPage.OPEN_SOURCE)
            OnboardingPage.FINISHING -> return true
        }
        return true
    }

    fun advanceTutorial() {
        if (state.tutorialIndex < 3) {
            state = state.copy(tutorialIndex = state.tutorialIndex + 1)
        } else {
            state = state.copy(page = OnboardingPage.OPEN_SOURCE, openSourceFocus = 0)
        }
    }

    fun handleConfirm(): Boolean {
        when (state.page) {
            OnboardingPage.INTRO -> Unit
            OnboardingPage.LANGUAGE -> {
                selectLanguage(state.languageIndex)
                state = state.copy(page = OnboardingPage.PROFILE, profileFocus = 0)
            }
            OnboardingPage.PROFILE -> if (state.profileFocus == 0) showKeyboard = true else continueFromProfile()
            OnboardingPage.THEME -> {
                scope.launch { settingsRepository.setTheme(if (state.themeIndex == 0) "dark" else "light") }
                state = state.copy(
                    page = OnboardingPage.PERMISSIONS,
                    permissionIndex = 0,
                    permissionFocus = 0,
                    permissionAwaitingReturn = false
                )
            }
            OnboardingPage.PERMISSIONS -> requestCurrentPermission()
            OnboardingPage.GAMES -> {
                if (!state.hasAddedGames && state.gamesFocus == 1) {
                    state = state.copy(page = OnboardingPage.TUTORIAL, tutorialIndex = 0)
                } else if (state.hasAddedGames && state.gamesFocus == 1) {
                    state = state.copy(page = OnboardingPage.TUTORIAL, tutorialIndex = 0)
                } else {
                    state = state.copy(gamesDialogOpen = true)
                }
            }
            OnboardingPage.TUTORIAL -> advanceTutorial()
            OnboardingPage.OPEN_SOURCE -> state = state.copy(page = OnboardingPage.AUDIO, audioFocus = 0)
            OnboardingPage.AUDIO -> when (state.audioFocus) {
                0 -> scope.launch {
                    val next = !settings.hapticFeedbackEnabled
                    settingsRepository.setHapticFeedbackEnabled(next)
                    settingsRepository.setVibrationEnabled(next)
                }
                1 -> scope.launch { settingsRepository.setInterfaceSounds(!settings.interfaceSounds) }
                2 -> scope.launch { settingsRepository.setBackgroundMusicEnabled(!settings.backgroundMusicEnabled) }
                else -> state = state.copy(page = OnboardingPage.FINISHING)
            }
            OnboardingPage.FINISHING -> Unit
        }
        return true
    }

    fun handleDirectional(key: Key): Boolean {
        when (state.page) {
            OnboardingPage.LANGUAGE -> if (key == Key.DirectionUp || key == Key.DirectionDown) {
                state = state.copy(languageIndex = if (state.languageIndex == 0) 1 else 0)
            } else return false
            OnboardingPage.PROFILE -> when (key) {
                Key.DirectionUp, Key.DirectionLeft -> state = state.copy(profileFocus = (state.profileFocus - 1).coerceAtLeast(0))
                Key.DirectionDown, Key.DirectionRight -> state = state.copy(
                    profileFocus = (state.profileFocus + 1).coerceAtMost(
                        if (nick.isBlank()) 0 else 1
                    )
                )
                else -> return false
            }
            OnboardingPage.THEME -> if (key == Key.DirectionLeft || key == Key.DirectionRight) {
                selectTheme(if (state.themeIndex == 0) 1 else 0)
            } else return false
            OnboardingPage.PERMISSIONS -> if (key == Key.DirectionUp || key == Key.DirectionDown ||
                key == Key.DirectionLeft || key == Key.DirectionRight
            ) {
                state = state.copy(permissionFocus = 0)
            } else return false
            OnboardingPage.GAMES -> when (key) {
                Key.DirectionUp, Key.DirectionLeft -> state = state.copy(gamesFocus = 0)
                Key.DirectionDown, Key.DirectionRight -> state = state.copy(gamesFocus = 1)
                else -> return false
            }
            OnboardingPage.TUTORIAL -> if (key == Key.DirectionLeft) {
                state = state.copy(tutorialIndex = (state.tutorialIndex - 1).coerceAtLeast(0))
            } else if (key == Key.DirectionRight) {
                state = state.copy(tutorialIndex = (state.tutorialIndex + 1).coerceAtMost(3))
            } else return false
            OnboardingPage.OPEN_SOURCE -> if (key != Key.DirectionUp && key != Key.DirectionDown &&
                key != Key.DirectionLeft && key != Key.DirectionRight
            ) return false
            OnboardingPage.AUDIO -> when (key) {
                Key.DirectionUp, Key.DirectionLeft -> state = state.copy(audioFocus = (state.audioFocus - 1).coerceAtLeast(0))
                Key.DirectionDown, Key.DirectionRight -> state = state.copy(audioFocus = (state.audioFocus + 1).coerceAtMost(3))
                else -> return false
            }
            OnboardingPage.INTRO, OnboardingPage.FINISHING -> return true
        }
        playMove()
        return true
    }

    fun handleGamepadKey(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (HorizonNavigation.isHomeKeyCode(event.nativeKeyEvent.keyCode)) return true
        if (event.key == Key.ButtonB || event.key == Key.Back ||
            event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_B
        ) {
            LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.GAMEPAD)
            moveBack()
            return true
        }
        if (isHorizonConfirmKey(event)) {
            LauncherAudioManager.playConfirm(LauncherInputSource.GAMEPAD)
            handleConfirm()
            return true
        }
        return onboardingDirectionalKey(event)?.let(::handleDirectional) ?: false
    }

    fun handleNativeGamepadKey(event: android.view.KeyEvent): Boolean {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return false
        val isBack = event.keyCode == android.view.KeyEvent.KEYCODE_BUTTON_B ||
            event.keyCode == android.view.KeyEvent.KEYCODE_BACK
        if (isBack && showKeyboard) {
            showKeyboard = false
            return true
        }
        if (isBack && state.gamesDialogOpen) {
            state = state.copy(gamesDialogOpen = false)
            return true
        }
        if (showKeyboard || state.gamesDialogOpen) return false
        when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_HOME,
            android.view.KeyEvent.KEYCODE_BUTTON_MODE -> return true
            android.view.KeyEvent.KEYCODE_BUTTON_B,
            android.view.KeyEvent.KEYCODE_BACK -> {
                LauncherAudioManager.play(LauncherSound.BACK, LauncherInputSource.GAMEPAD)
                moveBack()
            }
            android.view.KeyEvent.KEYCODE_BUTTON_A,
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                LauncherAudioManager.playConfirm(LauncherInputSource.GAMEPAD)
                handleConfirm()
            }
            android.view.KeyEvent.KEYCODE_DPAD_UP -> handleDirectional(Key.DirectionUp)
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> handleDirectional(Key.DirectionDown)
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> handleDirectional(Key.DirectionLeft)
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> handleDirectional(Key.DirectionRight)
            else -> return false
        }
        return true
    }

    val nativeGamepadHandler = rememberUpdatedState<(android.view.KeyEvent) -> Boolean>(
        { event -> handleNativeGamepadKey(event) }
    )

    DisposableEffect(Unit) {
        OnboardingSession.active = true
        OnboardingSession.keyHandler = { event -> nativeGamepadHandler.value(event) }
        OnboardingAudioManager.playOnce(context)
        onDispose {
            OnboardingSession.keyHandler = null
            OnboardingSession.active = false
            OnboardingAudioManager.stop()
        }
    }

    BackHandler(enabled = state.page != OnboardingPage.INTRO && state.page != OnboardingPage.FINISHING) {
        moveBack()
    }

    CompositionLocalProvider(LocalSettingsInputMode provides remember { mutableStateOf(SettingsInputMode.TOUCH) }) {
        OnboardingWaveBackground(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onFocusChanged { focusState ->
                        if (!focusState.hasFocus && !showKeyboard &&
                            !state.gamesDialogOpen
                        ) {
                            scope.launch {
                                delay(50)
                                focusRequester.requestFocus()
                            }
                        }
                    }
                    .onPreviewKeyEvent(::handleGamepadKey)
                    .onKeyEvent(::handleGamepadKey)
            ) {
                if (state.page != OnboardingPage.INTRO && state.page != OnboardingPage.FINISHING) {
                    OnboardingTopBar(state.page)
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (state.page == OnboardingPage.INTRO || state.page == OnboardingPage.FINISHING) 0.dp else 66.dp,
                            bottom = if (state.page == OnboardingPage.INTRO || state.page == OnboardingPage.FINISHING) 0.dp else 56.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = state.page,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(tween(420)) + slideInHorizontally(tween(420)) { it / 9 },
                                initialContentExit = fadeOut(tween(240)) + slideOutHorizontally(tween(240)) { -it / 12 }
                            )
                        },
                        label = "onboardingPageTransition"
                    ) { page ->
                        when (page) {
                            OnboardingPage.INTRO -> OnboardingIntroStage()
                            OnboardingPage.LANGUAGE -> OnboardingLanguageStage(
                                focusIndex = state.languageIndex,
                                onSelect = { index ->
                                    selectLanguage(index)
                                    state = state.copy(page = OnboardingPage.PROFILE, profileFocus = 0)
                                },
                                onFocus = { index -> state = state.copy(languageIndex = index) }
                            )
                            OnboardingPage.PROFILE -> OnboardingProfileStage(
                                profile = profile,
                                nick = nick,
                                focusIndex = state.profileFocus,
                                onEditNick = { showKeyboard = true },
                                onContinue = ::continueFromProfile,
                                onFocus = { index -> state = state.copy(profileFocus = index) }
                            )
                            OnboardingPage.THEME -> OnboardingThemeStage(
                                selectedIndex = state.themeIndex,
                                onSelect = { index ->
                                    selectTheme(index)
                                    state = state.copy(
                                        page = OnboardingPage.PERMISSIONS,
                                        permissionIndex = 0,
                                        permissionFocus = 0,
                                        permissionAwaitingReturn = false
                                    )
                                },
                                onFocus = { index -> selectTheme(index) }
                            )
                            OnboardingPage.PERMISSIONS -> currentPermission()?.let { step ->
                                OnboardingPermissionStage(
                                    step = step,
                                    currentIndex = state.permissionIndex,
                                    total = permissionSteps.size,
                                    focusIndex = state.permissionFocus,
                                    onGrant = ::requestCurrentPermission
                                )
                            }
                            OnboardingPage.GAMES -> OnboardingGamesStage(
                                hasAddedGames = state.hasAddedGames,
                                focusIndex = state.gamesFocus,
                                onOpenGames = { state = state.copy(gamesDialogOpen = true) },
                                onContinue = { state = state.copy(page = OnboardingPage.TUTORIAL, tutorialIndex = 0) },
                                onFocus = { index -> state = state.copy(gamesFocus = index) }
                            )
                            OnboardingPage.TUTORIAL -> OnboardingTutorialStage(state.tutorialIndex, ::advanceTutorial)
                            OnboardingPage.OPEN_SOURCE -> OnboardingOpenSourceStage(
                                onContinue = { state = state.copy(page = OnboardingPage.AUDIO, audioFocus = 0) }
                            )
                            OnboardingPage.AUDIO -> OnboardingAudioStage(
                                settingsSoundEnabled = settings.interfaceSounds,
                                ambientEnabled = settings.backgroundMusicEnabled,
                                vibrationEnabled = settings.hapticFeedbackEnabled,
                                focusIndex = state.audioFocus,
                                onFocus = { index -> state = state.copy(audioFocus = index) },
                                onSoundToggle = { scope.launch { settingsRepository.setInterfaceSounds(!settings.interfaceSounds) } },
                                onAmbientToggle = { scope.launch { settingsRepository.setBackgroundMusicEnabled(!settings.backgroundMusicEnabled) } },
                                onVibrationToggle = {
                                    scope.launch {
                                        val next = !settings.hapticFeedbackEnabled
                                        settingsRepository.setHapticFeedbackEnabled(next)
                                        settingsRepository.setVibrationEnabled(next)
                                    }
                                },
                                onFinish = { state = state.copy(page = OnboardingPage.FINISHING) }
                            )
                            OnboardingPage.FINISHING -> OnboardingFinalStage()
                        }
                    }
                }
                if (state.page != OnboardingPage.INTRO && state.page != OnboardingPage.FINISHING) {
                    OnboardingControlHints(
                        backLabel = stringResource(R.string.onboarding_back),
                        confirmLabel = stringResource(R.string.onboarding_select),
                        onBack = ::moveBack,
                        onConfirm = ::handleConfirm,
                        showBack = true,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    if (state.gamesDialogOpen) {
        GamesScreen(
            onDismiss = { state = state.copy(gamesDialogOpen = false) },
            onGamesAdded = { added ->
                if (added.isNotEmpty()) {
                    ScanCoordinator.init(context)
                    ScanCoordinator.consumeHint()
                    ScanCoordinator.enqueue(added)
                    state = state.copy(hasAddedGames = true, gamesFocus = 1, gamesDialogOpen = false)
                }
            }
        )
    }

    if (showKeyboard) {
        HorizonKeyboardDialog(
            title = stringResource(R.string.onboarding_profile_nick_prompt),
            initialValue = nick,
            initialLanguage = if (state.languageIndex == 0) KeyboardLanguage.RU else KeyboardLanguage.EN,
            maxLength = 10,
            onConfirm = { value ->
                nick = value.trim()
                if (value.isNotBlank()) profileRepository.setNick(value.trim())
                showKeyboard = false
            },
            onCancel = { showKeyboard = false }
        )
    }

}
