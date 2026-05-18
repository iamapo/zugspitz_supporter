package de.zugspitz.supporter

import de.zugspitz.supporter.data.LiveSharingBackendConfig

actual fun platformLiveSharingConfig(): LiveSharingBackendConfig? {
    val url = BuildConfig.SUPABASE_URL.trim()
    val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    if (url.isBlank() || publishableKey.isBlank()) return null

    return LiveSharingBackendConfig(
        url = url,
        publishableKey = publishableKey,
    )
}
