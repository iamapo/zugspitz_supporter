package de.zugspitz.supporter

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

data class RunnerDeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val horizontalAccuracyMeters: Double?,
    val timestampEpochMillis: Long,
)

interface RunnerLocationProvider {
    val locations: Flow<RunnerDeviceLocation>

    fun start()

    fun stop()
}

@Composable
expect fun rememberRunnerLocationProvider(): RunnerLocationProvider
