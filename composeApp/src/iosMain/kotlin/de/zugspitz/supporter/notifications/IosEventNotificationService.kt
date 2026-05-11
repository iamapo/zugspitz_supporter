package de.zugspitz.supporter.notifications

import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.RaceEstimate
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosEventNotificationService : EventNotificationService {
    private var permissionRequested = false

    override fun notifyRemoteEvent(event: CheckEvent, estimate: RaceEstimate) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        if (!permissionRequested) {
            permissionRequested = true
            center.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                completionHandler = { granted, _ ->
                    if (granted) {
                        postNotification(center, event, estimate)
                    }
                },
            )
            return
        }
        postNotification(center, event, estimate)
    }

    private fun postNotification(
        center: UNUserNotificationCenter,
        event: CheckEvent,
        estimate: RaceEstimate,
    ) {
        val content = UNMutableNotificationContent().apply {
            title = event.notificationTitle()
            body = event.notificationBody(estimate)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = NSUUID().UUIDString(),
            content = content,
            trigger = trigger,
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}
