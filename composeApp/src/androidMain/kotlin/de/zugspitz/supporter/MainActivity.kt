package de.zugspitz.supporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.SupabaseLiveRaceRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveSharingEnabled = BuildConfig.LIVE_SHARING_ENABLED
        val liveSharingConfig = platformLiveSharingConfig()
        when {
            !liveSharingEnabled -> LiveSharingLogger.d("Android live sharing disabled for this build.")
            liveSharingConfig == null -> LiveSharingLogger.d("Android live sharing enabled, but Supabase config is missing.")
            else -> LiveSharingLogger.d("Android live sharing enabled with Supabase backend.")
        }
        setContent {
            SupporterApp(
                liveRaceRepository = if (liveSharingEnabled && liveSharingConfig != null) {
                    SupabaseLiveRaceRepository(liveSharingConfig)
                } else {
                    NoOpLiveRaceRepository()
                },
                liveSharingEnabled = liveSharingEnabled,
            )
        }
    }
}
