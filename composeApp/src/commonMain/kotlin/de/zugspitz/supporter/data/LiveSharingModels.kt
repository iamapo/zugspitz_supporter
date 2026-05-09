package de.zugspitz.supporter.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class LiveRole {
    Runner,
    Supporter,
}

@Immutable
@Serializable
data class LiveRunLink(
    val role: LiveRole = LiveRole.Runner,
    val runCode: String = "",
    val runnerName: String = "",
    val isEnabled: Boolean = false,
) {
    val canPublish: Boolean
        get() = isEnabled && role == LiveRole.Runner && runCode.isNotBlank()

    val canSubscribe: Boolean
        get() = isEnabled && role == LiveRole.Supporter && runCode.isNotBlank()
}

@Serializable
enum class CheckEventType {
    CheckIn,
    CheckOut,
}

@Immutable
@Serializable
data class CheckEvent(
    val id: String,
    val runCode: String,
    val stationSection: Int,
    val stationName: String,
    val type: CheckEventType,
    val raceMinutes: Int,
    val createdAtEpochMillis: Long,
)

interface LiveRaceRepository {
    fun publish(event: CheckEvent)
}

class NoOpLiveRaceRepository : LiveRaceRepository {
    override fun publish(event: CheckEvent) = Unit
}
