package de.zugspitz.supporter

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal object AndroidRunnerLocationBus {
    private val _locations = MutableSharedFlow<RunnerDeviceLocation>(
        replay = 1,
        extraBufferCapacity = 8,
    )

    val locations: SharedFlow<RunnerDeviceLocation> = _locations.asSharedFlow()

    fun publish(location: RunnerDeviceLocation) {
        _locations.tryEmit(location)
    }
}
