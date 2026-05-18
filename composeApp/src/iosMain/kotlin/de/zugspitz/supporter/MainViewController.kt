package de.zugspitz.supporter

import androidx.compose.ui.window.ComposeUIViewController
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.SupabaseLiveRaceRepository

fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    val liveSharingConfig = platformLiveSharingConfig()
    val liveSharingEnabled = liveSharingConfig != null
    val liveRaceRepository = liveSharingConfig?.let(::SupabaseLiveRaceRepository) ?: NoOpLiveRaceRepository()
    if (liveSharingEnabled) {
        LiveSharingLogger.d("iOS live sharing enabled with Supabase backend.")
    } else {
        LiveSharingLogger.d("iOS live sharing disabled because Supabase config is missing.")
    }
    SupporterApp(
        liveRaceRepository = liveRaceRepository,
        liveSharingEnabled = liveSharingEnabled,
    )
}
