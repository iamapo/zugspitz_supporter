package de.zugspitz.supporter

import android.Manifest
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import de.zugspitz.supporter.data.KmpFirebaseLiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.notifications.AndroidEventNotificationService
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        val liveSharingEnabled = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        setContent {
            SupporterApp(
                liveRaceRepository = if (liveSharingEnabled) {
                    KmpFirebaseLiveRaceRepository()
                } else {
                    NoOpLiveRaceRepository()
                },
                liveSharingEnabled = liveSharingEnabled,
                eventNotificationService = AndroidEventNotificationService(this),
            )
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
