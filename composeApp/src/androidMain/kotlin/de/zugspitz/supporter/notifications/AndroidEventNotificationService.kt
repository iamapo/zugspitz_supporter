package de.zugspitz.supporter.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.zugspitz.supporter.R
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.RaceEstimate

class AndroidEventNotificationService(
    context: Context,
) : EventNotificationService {
    private val appContext = context.applicationContext

    init {
        createChannelIfNeeded()
    }

    override fun notifyRemoteEvent(event: CheckEvent, estimate: RaceEstimate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) return
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(event.notificationTitle())
            .setContentText(event.notificationBody(estimate))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(appContext).notify(event.id.hashCode(), notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = CHANNEL_DESCRIPTION
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "supporter_live_events"
        const val CHANNEL_NAME = "Support Events"
        const val CHANNEL_DESCRIPTION = "Notifications for remote check-ins and check-outs"
    }
}
