package de.zugspitz.supporter

internal expect object LiveSharingLogger {
    fun d(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
