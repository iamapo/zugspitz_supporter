package de.zugspitz.supporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveSharingEnabled = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
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
