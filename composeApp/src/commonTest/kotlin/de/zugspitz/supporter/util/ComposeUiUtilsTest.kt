package de.zugspitz.supporter.util

import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.START_LINE_SECTION
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeUiUtilsTest {
    @Test
    fun `first section duration uses actual start when available`() {
        val projection = RaceCalculator().project(
            estimate = RaceEstimate(),
            checkIns = listOf(
                CheckIn(START_LINE_SECTION, actualArrivalMinutes = -7 * 60),
                CheckIn(stationSection = 1, actualArrivalMinutes = -7 * 60),
            ),
            selectedIndex = 0,
        )

        val duration = ComposeUiUtils.sectionDurationMinutes(
            current = projection.stations.first(),
            previous = null,
            actualStartMinutes = projection.actualStartMinutes,
        )

        assertEquals(0, duration)
        assertEquals("-", ComposeUiUtils.sectionPace(duration, projection.stations.first().station.sectionKm))
    }

    @Test
    fun `pace from start to first station uses actual start offset as baseline`() {
        val projection = RaceCalculator().project(
            estimate = RaceEstimate(),
            checkIns = listOf(CheckIn(START_LINE_SECTION, actualArrivalMinutes = 9)),
            selectedIndex = 0,
        )
        val firstStation = projection.stations.first()

        assertEquals(
            ComposeUiUtils.sectionPace(firstStation.station.plannedArrivalMinutes - 9, firstStation.station.sectionKm)
                .removeSuffix(" min/km") + "/km",
            ComposeUiUtils.likelyPaceFromStart(firstStation, projection.actualStartMinutes),
        )
    }

    @Test
    fun `average pace does not show zero for non positive elapsed time`() {
        assertEquals("-", ComposeUiUtils.averagePace(0, 10.0))
        assertEquals("-", ComposeUiUtils.averagePace(-5, 10.0))
    }
}
