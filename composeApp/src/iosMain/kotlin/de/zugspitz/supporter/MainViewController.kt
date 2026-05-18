package de.zugspitz.supporter

import androidx.compose.ui.window.ComposeUIViewController
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.SupabaseLiveRaceRepository
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    val liveSharingEnabled = Platform.isDebugBinary
    val liveSharingConfig = platformLiveSharingConfig()
    when {
        !liveSharingEnabled -> LiveSharingLogger.d("iOS live sharing disabled for this build.")
        liveSharingConfig == null -> LiveSharingLogger.d("iOS live sharing enabled, but Supabase config is missing.")
        else -> LiveSharingLogger.d("iOS live sharing enabled with Supabase backend.")
    }
    SupporterApp(
        liveRaceRepository = if (liveSharingEnabled && liveSharingConfig != null) {
            SupabaseLiveRaceRepository(liveSharingConfig)
        } else {
            NoOpLiveRaceRepository()
        },
        liveSharingEnabled = liveSharingEnabled,
    )
}
