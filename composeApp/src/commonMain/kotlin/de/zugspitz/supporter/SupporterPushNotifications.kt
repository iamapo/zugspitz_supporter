package de.zugspitz.supporter

interface SupporterPushNotifications {
    fun currentToken(): String?
    fun requestToken(onToken: (String) -> Unit)
}

object NoOpSupporterPushNotifications : SupporterPushNotifications {
    override fun currentToken(): String? = null

    override fun requestToken(onToken: (String) -> Unit) = Unit
}
