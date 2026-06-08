package de.zugspitz.supporter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
actual fun rememberRunnerLocationProvider(): RunnerLocationProvider = remember { NoOpRunnerLocationProvider }

private object NoOpRunnerLocationProvider : RunnerLocationProvider {
    override val locations: Flow<RunnerDeviceLocation> = emptyFlow()

    override fun start() = Unit

    override fun stop() = Unit
}
