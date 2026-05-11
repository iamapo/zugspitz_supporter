package de.zugspitz.supporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveSharingEnabled = BuildConfig.LIVE_SHARING_ENABLED
        setContent {
            SupporterApp(
                liveRaceRepository = if (liveSharingEnabled) {
                    KmpFirebaseLiveRaceRepository()
                } else {
                    NoOpLiveRaceRepository()
                },
                liveSharingEnabled = liveSharingEnabled,
            )
        }
    }
}
