package de.zugspitz.supporter

import platform.Foundation.NSLog

internal actual object LiveSharingLogger {
    actual fun d(message: String) {
        NSLog("LiveSharing: $message")
    }

    actual fun e(message: String, throwable: Throwable?) {
        val suffix = throwable?.let { " | ${it.message ?: it::class.simpleName}" }.orEmpty()
        NSLog("LiveSharing ERROR: $message$suffix")
    }
}
