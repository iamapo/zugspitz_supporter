package de.zugspitz.supporter

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AndroidRunnerLocationService : Service(), LocationListener {
    private var isUpdatingLocation = false
    private val locationManager: LocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NotificationId, buildNotification())
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        isUpdatingLocation = false
        runCatching {
            locationManager.removeUpdates(this)
        }.onFailure { error ->
            LiveSharingLogger.e("Android runner location updates failed to stop", error)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: Location) {
        val timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        val accuracy = location.accuracy.takeIf { location.hasAccuracy() }?.toDouble()
        LiveSharingLogger.d("Android runner location received accuracy=${accuracy ?: "unknown"}.")
        AndroidRunnerLocationBus.publish(
            RunnerDeviceLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                horizontalAccuracyMeters = accuracy,
                timestampEpochMillis = timestamp,
            ),
        )
    }

    @Deprecated("Deprecated in Android framework")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) {
        LiveSharingLogger.d("Android runner location provider enabled provider=$provider")
    }

    override fun onProviderDisabled(provider: String) {
        LiveSharingLogger.d("Android runner location provider disabled provider=$provider")
    }

    private fun startLocationUpdates() {
        if (isUpdatingLocation) return
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            LiveSharingLogger.d("Android runner location updates not started because fine location permission is missing.")
            stopSelf()
            return
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider -> locationManager.isProviderEnabled(provider) }
        if (providers.isEmpty()) {
            LiveSharingLogger.d("Android runner location updates not started because no provider is enabled.")
            return
        }

        providers.forEach { provider ->
            runCatching {
                locationManager.requestLocationUpdates(
                    provider,
                    LocationUpdateMinTimeMillis,
                    LocationUpdateMinDistanceMeters,
                    this,
                )
                locationManager.getLastKnownLocation(provider)?.let(::onLocationChanged)
                LiveSharingLogger.d("Android runner location updates started provider=$provider")
            }.onFailure { error ->
                LiveSharingLogger.e("Android runner location updates failed provider=$provider", error)
            }
        }
        isUpdatingLocation = true
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NotificationChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.runner_location_notification_title))
            .setContentText(getString(R.string.runner_location_notification_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NotificationChannelId,
            getString(R.string.runner_location_notification_title),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val NotificationChannelId = "runner-location"
        const val NotificationId = 4217
        const val LocationUpdateMinTimeMillis = 60_000L
        const val LocationUpdateMinDistanceMeters = 25f
    }
}
