package de.zugspitz.supporter

import androidx.compose.ui.window.ComposeUIViewController
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    val liveSharingEnabled = Platform.isDebugBinary
    SupporterApp(
        liveRaceRepository = if (liveSharingEnabled) {
            KmpFirebaseLiveRaceRepository()
        } else {
            NoOpLiveRaceRepository()
        },
        liveSharingEnabled = liveSharingEnabled,
    )
}
