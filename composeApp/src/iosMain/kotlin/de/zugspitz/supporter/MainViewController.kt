package de.zugspitz.supporter

import androidx.compose.ui.window.ComposeUIViewController
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository

fun MainViewController() = ComposeUIViewController(
    configure = {
        enforceStrictPlistSanityCheck = false
    }
) {
    SupporterApp(liveRaceRepository = KmpFirebaseLiveRaceRepository())
}
