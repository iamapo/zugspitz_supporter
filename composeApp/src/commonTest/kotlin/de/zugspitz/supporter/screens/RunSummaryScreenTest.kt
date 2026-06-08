package de.zugspitz.supporter.screens

import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import kotlin.test.Test
import kotlin.test.assertEquals

class RunSummaryScreenTest {
    private val calculator = RaceCalculator()

    @Test
    fun `list highlights open station instead of selected card station`() {
        val baseProjection = calculator.project(RaceEstimate(), emptyList(), selectedIndex = 0)
        val openStation = baseProjection.stations[1].station

        val projection = calculator.project(
            estimate = RaceEstimate(),
            checkIns = listOf(CheckIn(openStation.section, openStation.plannedArrivalMinutes)),
            selectedIndex = 4,
        )

        assertEquals(1, highlightedStationIndexForList(projection.stations))
    }

    @Test
    fun `list highlights next station when no station is currently open`() {
        val baseProjection = calculator.project(RaceEstimate(), emptyList(), selectedIndex = 0)
        val completedStation = baseProjection.stations[0].station

        val projection = calculator.project(
            estimate = RaceEstimate(),
            checkIns = listOf(
                CheckIn(
                    stationSection = completedStation.section,
                    actualArrivalMinutes = completedStation.plannedArrivalMinutes,
                    actualDepartureMinutes = completedStation.plannedArrivalMinutes + completedStation.stopMinutes,
                ),
            ),
            selectedIndex = 4,
        )

        assertEquals(1, highlightedStationIndexForList(projection.stations))
    }
}
