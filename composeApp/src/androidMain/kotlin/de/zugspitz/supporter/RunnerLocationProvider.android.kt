package de.zugspitz.supporter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

@Composable
actual fun rememberRunnerLocationProvider(): RunnerLocationProvider {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidRunnerLocationProvider(context) }
}

private class AndroidRunnerLocationProvider(
    private val context: Context,
) : RunnerLocationProvider {
    override val locations: Flow<RunnerDeviceLocation> = AndroidRunnerLocationBus.locations

    override fun start() {
        val missingPermissions = missingForegroundPermissions(context)
        if (missingPermissions.isNotEmpty()) {
            requestLocationPermissions(missingPermissions)
            LiveSharingLogger.d("Android runner location permissions requested: ${missingPermissions.joinToString()}")
            return
        }
        val intent = Intent(context, AndroidRunnerLocationService::class.java)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            LiveSharingLogger.d("Android runner location foreground service started.")
        }.onFailure { error ->
            LiveSharingLogger.e("Android runner location foreground service failed to start", error)
        }
    }

    override fun stop() {
        runCatching {
            context.stopService(Intent(context, AndroidRunnerLocationService::class.java))
            LiveSharingLogger.d("Android runner location foreground service stopped.")
        }.onFailure { error ->
            LiveSharingLogger.e("Android runner location foreground service failed to stop", error)
        }
    }
}

private fun missingForegroundPermissions(context: Context): List<String> {
    val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    return requiredPermissions.filter { permission ->
        context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
    }
}

private fun requestLocationPermissions(permissions: List<String>) {
    val activity = MainActivity.currentActivity ?: return
    activity.requestPermissions(permissions.toTypedArray(), RunnerLocationPermissionRequestCode)
}

private const val RunnerLocationPermissionRequestCode = 7341
