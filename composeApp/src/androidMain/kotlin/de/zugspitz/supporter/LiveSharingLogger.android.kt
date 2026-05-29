package de.zugspitz.supporter

import android.util.Log

internal actual object LiveSharingLogger {
    private const val TAG = "LiveSharing"

    actual fun d(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    actual fun e(message: String, throwable: Throwable?) {
        runCatching { Log.e(TAG, message, throwable) }
    }
}
