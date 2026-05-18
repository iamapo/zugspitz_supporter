package de.zugspitz.supporter

import de.zugspitz.supporter.data.LiveSharingBackendConfig
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

actual fun platformLiveSharingConfig(): LiveSharingBackendConfig? {
    val environment = NSProcessInfo.processInfo.environment
    val envUrl = environment["SUPABASE_URL"] as? String
    val envPublishableKey = (environment["SUPABASE_PUBLISHABLE_KEY"] as? String)
        ?: (environment["SUPABASE_ANON_KEY"] as? String)
    val url = envUrl?.trim().takeUnless { it.isNullOrBlank() }
        ?: (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_URL") as? String)?.trim()
    val publishableKey = envPublishableKey?.trim().takeUnless { it.isNullOrBlank() }
        ?: (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_PUBLISHABLE_KEY") as? String)?.trim()
        ?: (NSBundle.mainBundle.objectForInfoDictionaryKey("SUPABASE_ANON_KEY") as? String)?.trim()
    if (url.isNullOrBlank() || publishableKey.isNullOrBlank()) return null

    return LiveSharingBackendConfig(
        url = url,
        publishableKey = publishableKey,
    )
}
