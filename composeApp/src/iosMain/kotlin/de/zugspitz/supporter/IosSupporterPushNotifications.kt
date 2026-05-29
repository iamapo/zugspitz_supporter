package de.zugspitz.supporter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

fun platformSupporterPushNotifications(): SupporterPushNotifications = IosSupporterPushNotifications()

private class IosSupporterPushNotifications(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SupporterPushNotifications {
    override fun currentToken(): String? = storedToken()

    override fun requestToken(onToken: (String) -> Unit) {
        storedToken()?.let(onToken)

        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, error ->
            if (error != null) {
                LiveSharingLogger.e("iOS notification permission request failed", Throwable(error.localizedDescription))
            }
            if (!granted) {
                LiveSharingLogger.d("iOS notification permission was not granted.")
                return@requestAuthorizationWithOptions
            }

            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = REGISTER_REMOTE_NOTIFICATIONS_NAME,
                `object` = null,
            )
            waitForDeviceToken(onToken)
        }
    }

    private fun waitForDeviceToken(onToken: (String) -> Unit) {
        scope.launch {
            repeat(TOKEN_POLL_ATTEMPTS) {
                val token = storedToken()
                if (!token.isNullOrBlank()) {
                    onToken(token)
                    return@launch
                }
                delay(TOKEN_POLL_INTERVAL_MS)
            }
            LiveSharingLogger.d("iOS APNs token was not available after registration request.")
        }
    }

    private fun storedToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(APNS_DEVICE_TOKEN_KEY)
            ?.trim()
            ?.takeUnless { it.isBlank() }

    private companion object {
        const val APNS_DEVICE_TOKEN_KEY = "zugspitz.apnsDeviceToken"
        const val REGISTER_REMOTE_NOTIFICATIONS_NAME = "zugspitz.registerForRemoteNotifications"
        const val TOKEN_POLL_ATTEMPTS = 20
        const val TOKEN_POLL_INTERVAL_MS = 500L
    }
}
