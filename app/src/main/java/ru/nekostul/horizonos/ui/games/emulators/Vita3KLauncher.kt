package ru.nekostul.horizonos.ui.games.emulators

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import ru.nekostul.horizonos.R
import ru.nekostul.horizonos.ui.games.Emulator
import ru.nekostul.horizonos.ui.games.Game
import ru.nekostul.horizonos.ui.games.GameLaunchResult

class Vita3KLauncher : EmulatorGameLauncher {
    override val emulator = Emulator.VITA3K

    override fun launch(context: Context, game: Game): GameLaunchResult {
        val titleId = game.romUri
        if (context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) == null) {
            return GameLaunchResult.Failed(context.getString(R.string.games_error_vita3k_not_installed))
        }
        val intent = Intent().apply {
            component = ComponentName(PACKAGE_NAME, "$PACKAGE_NAME.Emulator")
            putExtra(EXTRA_TITLE_ID, titleId)
            putExtra(EXTRA_GAME_TITLE, game.displayTitle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        return if (runCatching { context.startActivity(intent) }.isSuccess) {
            GameLaunchResult.Launched
        } else {
            GameLaunchResult.Failed(context.getString(R.string.games_error_launch_failed))
        }
    }

    private companion object {
        const val PACKAGE_NAME = "org.vita3k.emulator"
        const val EXTRA_TITLE_ID = "title_id"
        const val EXTRA_GAME_TITLE = "game_title"
    }
}
