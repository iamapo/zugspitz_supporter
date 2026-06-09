package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunInfo
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteSnapshotMergeUseCaseTest {
    private val useCase = RemoteSnapshotMergeUseCase()

    @Test
    fun `merges and deduplicates events by id`() {
        val localEvent = event(id = "RUN42-1-CheckIn-1", section = 1, type = CheckEventType.CheckIn, minutes = 75, createdAt = 1)
        val remoteCheckOut = event(id = "RUN42-1-CheckOut-2", section = 1, type = CheckEventType.CheckOut, minutes = 82, createdAt = 2)

        val result = useCase(
            currentTab = AppTab.Vp,
            currentEstimate = RaceEstimate(),
            currentEvents = listOf(localEvent),
            currentSelectedIndex = 0,
            currentStationSection = 1,
            currentLastStationIndex = 10,
            remoteSnapshot = LiveRunSnapshot(
                info = null,
                events = listOf(localEvent, remoteCheckOut),
            ),
        )

        assertEquals(listOf(localEvent, remoteCheckOut), result.checkEvents)
        assertEquals(CheckIn(1, actualArrivalMinutes = 75, actualDepartureMinutes = 82), result.checkIns.single())
    }

    @Test
    fun `opens vp tab and selects first open station when run info arrives from support code screen`() {
        val result = useCase(
            currentTab = AppTab.SupportCode,
            currentEstimate = RaceEstimate(),
            currentEvents = emptyList(),
            currentSelectedIndex = 0,
            currentStationSection = null,
            currentLastStationIndex = 10,
            remoteSnapshot = LiveRunSnapshot(
                info = LiveRunInfo(
                    runCode = "RUN42",
                    estimate = RaceDefinitions.byId(RaceDefinitions.EhrwaldTrailId).defaultEstimate(),
                    createdAtEpochMillis = 0L,
                ),
                events = listOf(
                    event(id = "RUN42-1-CheckIn-1", section = 1, type = CheckEventType.CheckIn, minutes = 30, createdAt = 1),
                    event(id = "RUN42-1-CheckOut-2", section = 1, type = CheckEventType.CheckOut, minutes = 35, createdAt = 2),
                ),
            ),
        )

        assertEquals(AppTab.Vp, result.tab)
        assertEquals(RaceDefinitions.EhrwaldTrailId, result.estimate.raceId)
        assertEquals(1, result.selectedIndex)
    }

    @Test
    fun `advances selected station after current station checkout`() {
        val result = useCase(
            currentTab = AppTab.Vp,
            currentEstimate = RaceEstimate(),
            currentEvents = emptyList(),
            currentSelectedIndex = 0,
            currentStationSection = 1,
            currentLastStationIndex = 10,
            remoteSnapshot = LiveRunSnapshot(
                info = null,
                events = listOf(
                    event(id = "RUN42-1-CheckIn-1", section = 1, type = CheckEventType.CheckIn, minutes = 75, createdAt = 1),
                    event(id = "RUN42-1-CheckOut-2", section = 1, type = CheckEventType.CheckOut, minutes = 82, createdAt = 2),
                ),
            ),
        )

        assertEquals(1, result.selectedIndex)
    }

    private fun event(
        id: String,
        section: Int,
        type: CheckEventType,
        minutes: Int,
        createdAt: Long,
    ) = CheckEvent(
        id = id,
        runCode = "RUN42",
        stationSection = section,
        stationName = "Z$section Test",
        type = type,
        raceMinutes = minutes,
        createdAtEpochMillis = createdAt,
    )
}
