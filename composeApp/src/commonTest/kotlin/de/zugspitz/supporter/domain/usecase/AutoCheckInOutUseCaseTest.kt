package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AutoCheckInOutUseCaseTest {
    private val useCase = AutoCheckInOutUseCase()

    @Test
    fun `returns none when automatic checks are disabled`() {
        val action = useCase(
            projection = projection(),
            location = location(distanceKm = 10.7),
            enabled = false,
            canPublish = true,
            alreadyAutoCheckedIn = emptySet(),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 90,
        )

        assertEquals(AutoCheckAction.None, action)
    }

    @Test
    fun `returns low accuracy skip when gps accuracy is too low`() {
        val action = useCase(
            projection = projection(),
            location = location(distanceKm = 10.7, accuracyMeters = 75.0),
            enabled = true,
            canPublish = true,
            alreadyAutoCheckedIn = emptySet(),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 90,
        )

        assertEquals(AutoCheckAction.SkippedLowAccuracy(75.0), action)
    }

    @Test
    fun `returns check in for next open station inside corridor`() {
        val action = useCase(
            projection = projection(),
            location = location(distanceKm = 10.7),
            enabled = true,
            canPublish = true,
            alreadyAutoCheckedIn = emptySet(),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 90,
        )

        val checkIn = assertIs<AutoCheckAction.CheckIn>(action)
        assertEquals(0, checkIn.stationIndex)
        assertEquals(1, checkIn.stationSection)
        assertEquals(90, checkIn.raceMinutes)
    }

    @Test
    fun `does not check in when next station was already auto checked in`() {
        val action = useCase(
            projection = projection(),
            location = location(distanceKm = 10.7),
            enabled = true,
            canPublish = true,
            alreadyAutoCheckedIn = setOf(1),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 90,
        )

        assertEquals(AutoCheckAction.None, action)
    }

    @Test
    fun `returns none for check out before minimum stop elapsed`() {
        val action = useCase(
            projection = projection(checkIns = listOf(CheckIn(1, actualArrivalMinutes = 90))),
            location = location(distanceKm = 11.2),
            enabled = true,
            canPublish = true,
            alreadyAutoCheckedIn = emptySet(),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 91,
        )

        assertEquals(AutoCheckAction.None, action)
    }

    @Test
    fun `returns check out after minimum stop and distance past station`() {
        val action = useCase(
            projection = projection(checkIns = listOf(CheckIn(1, actualArrivalMinutes = 90))),
            location = location(distanceKm = 11.2),
            enabled = true,
            canPublish = true,
            alreadyAutoCheckedIn = emptySet(),
            alreadyAutoCheckedOut = emptySet(),
            raceMinutes = 93,
        )

        val checkOut = assertIs<AutoCheckAction.CheckOut>(action)
        assertEquals(0, checkOut.stationIndex)
        assertEquals(1, checkOut.stationSection)
        assertEquals(93, checkOut.raceMinutes)
    }

    private fun projection(checkIns: List<CheckIn> = emptyList()) =
        RaceCalculator().project(RaceEstimate(), checkIns, selectedIndex = 0)

    private fun location(
        distanceKm: Double,
        accuracyMeters: Double = 25.0,
    ) = LiveRunnerLocation(
        runCode = "ABC123",
        raceId = RaceDefinitions.ZugspitzUltratrailId,
        latitude = 47.4710978,
        longitude = 11.0550948,
        distanceKm = distanceKm,
        elevationMeters = 741.0,
        distanceFromRouteMeters = 3.0,
        accuracyMeters = accuracyMeters,
        isOnRoute = true,
        updatedAtEpochMillis = 1_789_509_000_000L,
    )
}
