package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation

class ApplyCheckEventUseCase(
    private val saveCheckIn: SaveCheckInUseCase = SaveCheckInUseCase(),
) {
    operator fun invoke(
        checkIns: List<CheckIn>,
        checkEvents: List<CheckEvent>,
        runCode: String?,
        stationSection: Int,
        stationName: String,
        type: CheckEventType,
        raceMinutes: Int,
        createdAtEpochMillis: Long,
        runnerLocation: LiveRunnerLocation? = null,
    ): ApplyCheckEventResult {
        val updatedCheckIns = when (type) {
            CheckEventType.CheckIn -> saveCheckIn(
                existingCheckIns = checkIns,
                stationSection = stationSection,
                actualArrivalMinutes = raceMinutes,
            )
            CheckEventType.CheckOut -> saveCheckIn(
                existingCheckIns = checkIns,
                stationSection = stationSection,
                actualDepartureMinutes = raceMinutes,
            )
        }
        val event = runCode?.let { safeRunCode ->
            CheckEvent(
                id = "$safeRunCode-$stationSection-${type.name}-$createdAtEpochMillis",
                runCode = safeRunCode,
                stationSection = stationSection,
                stationName = stationName,
                type = type,
                raceMinutes = raceMinutes,
                createdAtEpochMillis = createdAtEpochMillis,
                runnerLocation = runnerLocation?.takeIf { it.runCode == safeRunCode },
            )
        }
        return ApplyCheckEventResult(
            checkIns = updatedCheckIns,
            checkEvents = event?.let { createdEvent ->
                checkEvents.filterNot {
                    it.stationSection == stationSection && it.type == type
                } + createdEvent
            } ?: checkEvents,
            event = event,
        )
    }
}

data class ApplyCheckEventResult(
    val checkIns: List<CheckIn>,
    val checkEvents: List<CheckEvent>,
    val event: CheckEvent?,
)
