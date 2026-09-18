package ru.nekostul.horizonos.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import ru.nekostul.horizonos.ui.files.RootHelper
import ru.nekostul.horizonos.ui.files.StorageAccess

enum class OnboardingPage {
    INTRO,
    LANGUAGE,
    PROFILE,
    THEME,
    PERMISSIONS,
    GAMES,
    TUTORIAL,
    OPEN_SOURCE,
    AUDIO,
    FINISHING
}

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.INTRO,
    val languageIndex: Int = 0,
    val profileFocus: Int = 0,
    val themeIndex: Int = 0,
    val permissionIndex: Int = 0,
    val permissionFocus: Int = 0,
    val permissionAwaitingReturn: Boolean = false,
    val gamesFocus: Int = 0,
    val tutorialIndex: Int = 0,
    val openSourceFocus: Int = 0,
    val audioFocus: Int = 0,
    val gamesDialogOpen: Boolean = false,
    val hasAddedGames: Boolean = false,
)

data class OnboardingPermissionStep(
    val id: String,
    val permission: String? = null,
    val permissions: List<String> = emptyList(),
    val allFilesAccess: Boolean = false,
    val isRoot: Boolean = false
)

fun onboardingPermissionSteps(): List<OnboardingPermissionStep> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add(OnboardingPermissionStep(id = "all_files", allFilesAccess = true))
    }
    StorageAccess.legacyPermissions().forEach { permission ->
        add(OnboardingPermissionStep(id = permission, permission = permission))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(
            OnboardingPermissionStep(
                id = "nearby_devices",
                permissions = listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        add(
            OnboardingPermissionStep(
                id = "wifi_location",
                permission = Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            OnboardingPermissionStep(
                id = Manifest.permission.POST_NOTIFICATIONS,
                permission = Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }
    if (RootHelper.hasSuBinary()) {
        add(OnboardingPermissionStep(id = "root", isRoot = true))
    }
}

fun OnboardingPermissionStep.isGranted(context: Context): Boolean {
    if (isRoot) return false
    if (allFilesAccess) return StorageAccess.hasAllFilesAccess()

    val requiredPermissions = permissions.ifEmpty { listOfNotNull(permission) }
    return requiredPermissions.all { requestedPermission ->
        ContextCompat.checkSelfPermission(
            context,
            requestedPermission
        ) == PackageManager.PERMISSION_GRANTED
    }
}

object OnboardingSession {
    @Volatile
    var active: Boolean = false

    @Volatile
    var keyHandler: ((android.view.KeyEvent) -> Boolean)? = null

    fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean =
        keyHandler?.invoke(event) == true
}
