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

@Immutable
@Serializable
data class LiveRunInfo(
    val runCode: String,
    val estimate: RaceEstimate,
    val createdAtEpochMillis: Long,
    val runnerName: String = "",
)

@Immutable
data class LiveRunSnapshot(
    val info: LiveRunInfo?,
    val events: List<CheckEvent> = emptyList(),
)

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

@Serializable
enum class PushPlatform {
    Ios,
}

interface LiveRaceRepository {
    suspend fun createRun(estimate: RaceEstimate): LiveRunInfo?
    fun deleteRun(runCode: String)
    fun publishRunInfo(info: LiveRunInfo)
    fun publish(event: CheckEvent)
    fun registerSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String)
    fun unregisterSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String)
    fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription
}

interface LiveRaceSubscription {
    fun close()
}

class NoOpLiveRaceRepository : LiveRaceRepository {
    override suspend fun createRun(estimate: RaceEstimate): LiveRunInfo? = null

    override fun deleteRun(runCode: String) = Unit

    override fun publishRunInfo(info: LiveRunInfo) = Unit

    override fun publish(event: CheckEvent) = Unit

    override fun registerSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) = Unit

    override fun unregisterSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) = Unit

    override fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription {
        return NoOpLiveRaceSubscription
    }
}

object NoOpLiveRaceSubscription : LiveRaceSubscription {
    override fun close() = Unit
}
