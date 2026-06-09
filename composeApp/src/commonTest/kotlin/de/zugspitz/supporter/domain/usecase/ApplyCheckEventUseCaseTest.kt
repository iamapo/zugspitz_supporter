package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplyCheckEventUseCaseTest {
    private val useCase = ApplyCheckEventUseCase()

    @Test
    fun `stores local check in without creating event when run code is missing`() {
        val result = useCase(
            checkIns = emptyList(),
            checkEvents = emptyList(),
            runCode = null,
            stationSection = 1,
            stationName = "Z1 Eibsee",
            type = CheckEventType.CheckIn,
            raceMinutes = 86,
            createdAtEpochMillis = 1_000L,
        )

        assertEquals(listOf(CheckIn(1, actualArrivalMinutes = 86)), result.checkIns)
        assertEquals(emptyList(), result.checkEvents)
        assertNull(result.event)
    }

    @Test
    fun `creates live check in event when run code is available`() {
        val result = useCase(
            checkIns = emptyList(),
            checkEvents = emptyList(),
            runCode = "ABC123",
            stationSection = 1,
            stationName = "Z1 Eibsee",
            type = CheckEventType.CheckIn,
            raceMinutes = 86,
            createdAtEpochMillis = 1_000L,
        )

        assertEquals(listOf(CheckIn(1, actualArrivalMinutes = 86)), result.checkIns)
        assertEquals("ABC123-1-CheckIn-1000", result.event?.id)
        assertEquals(result.event, result.checkEvents.single())
    }

    @Test
    fun `replaces previous event for same station and type`() {
        val previousCheckIn = event(
            id = "ABC123-1-CheckIn-1000",
            stationSection = 1,
            type = CheckEventType.CheckIn,
            raceMinutes = 86,
            createdAtEpochMillis = 1_000L,
        )
        val previousCheckOut = event(
            id = "ABC123-1-CheckOut-1100",
            stationSection = 1,
            type = CheckEventType.CheckOut,
            raceMinutes = 90,
            createdAtEpochMillis = 1_100L,
        )

        val result = useCase(
            checkIns = listOf(CheckIn(1, actualArrivalMinutes = 86, actualDepartureMinutes = 90)),
            checkEvents = listOf(previousCheckIn, previousCheckOut),
            runCode = "ABC123",
            stationSection = 1,
            stationName = "Z1 Eibsee",
            type = CheckEventType.CheckIn,
            raceMinutes = 88,
            createdAtEpochMillis = 2_000L,
        )

        assertEquals(listOf(CheckIn(1, actualArrivalMinutes = 88, actualDepartureMinutes = 90)), result.checkIns)
        assertEquals(
            listOf(previousCheckOut, result.event),
            result.checkEvents,
        )
    }

    private fun event(
        id: String,
        stationSection: Int,
        type: CheckEventType,
        raceMinutes: Int,
        createdAtEpochMillis: Long,
    ) = CheckEvent(
        id = id,
        runCode = "ABC123",
        stationSection = stationSection,
        stationName = "Z$stationSection Test",
        type = type,
        raceMinutes = raceMinutes,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}
