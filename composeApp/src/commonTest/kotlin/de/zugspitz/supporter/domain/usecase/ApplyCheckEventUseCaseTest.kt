package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceDefinitions
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
        val location = testRunnerLocation(runCode = "ABC123")
        val result = useCase(
            checkIns = emptyList(),
            checkEvents = emptyList(),
            runCode = "ABC123",
            stationSection = 1,
            stationName = "Z1 Eibsee",
            type = CheckEventType.CheckIn,
            raceMinutes = 86,
            createdAtEpochMillis = 1_000L,
            runnerLocation = location,
        )

        assertEquals(listOf(CheckIn(1, actualArrivalMinutes = 86)), result.checkIns)
        assertEquals("ABC123-1-CheckIn-1000", result.event?.id)
        assertEquals(location, result.event?.runnerLocation)
        assertEquals(result.event, result.checkEvents.single())
    }

    @Test
    fun `drops event location when it belongs to another run`() {
        val result = useCase(
            checkIns = emptyList(),
            checkEvents = emptyList(),
            runCode = "ABC123",
            stationSection = 1,
            stationName = "Z1 Eibsee",
            type = CheckEventType.CheckIn,
            raceMinutes = 86,
            createdAtEpochMillis = 1_000L,
            runnerLocation = testRunnerLocation(runCode = "OTHER"),
        )

        assertNull(result.event?.runnerLocation)
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

    private fun testRunnerLocation(runCode: String) = LiveRunnerLocation(
        runCode = runCode,
        raceId = RaceDefinitions.ZugspitzUltratrailId,
        latitude = 47.4710978,
        longitude = 11.0550948,
        distanceKm = 5.0,
        elevationMeters = 741.0,
        distanceFromRouteMeters = 3.0,
        accuracyMeters = 25.0,
        isOnRoute = true,
        updatedAtEpochMillis = 1_789_509_000_000L,
    )
}
