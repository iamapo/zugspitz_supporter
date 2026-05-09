package de.zugspitz.supporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SupporterApp(liveRaceRepository = KmpFirebaseLiveRaceRepository())
        }
    }
}
