package de.zugspitz.supporter.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RaceCalculatorTest {
    private val calculator = RaceCalculator()

    @Test
    fun `range projection distributes target time from segment effort and stops`() {
        val projection = calculator.project(
            estimate = RaceEstimate(
                targetMode = TargetTimeMode.Range,
                minDurationMinutes = 17 * 60,
                maxDurationMinutes = 18 * 60,
            ),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        val finish = projection.stations.last().station
        assertEquals(17 * 60, finish.plannedArrivalMinutes)
        assertEquals(17 * 60, finish.windowStartMinutes)
        assertEquals(18 * 60, finish.windowEndMinutes)
        assertEquals("15:00-16:00", projection.stations.last().window)
    }

    @Test
    fun `fixed projection uses the fixed duration for plan and window`() {
        val projection = calculator.project(
            estimate = RaceEstimate(
                targetMode = TargetTimeMode.Fixed,
                fixedDurationMinutes = 17 * 60 + 30,
            ),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        val finish = projection.stations.last().station
        assertEquals(17 * 60 + 30, finish.plannedArrivalMinutes)
        assertEquals(17 * 60 + 30, finish.windowStartMinutes)
        assertEquals(17 * 60 + 30, finish.windowEndMinutes)
        assertEquals("15:30-15:30", projection.stations.last().window)
    }

    @Test
    fun `ehrwald projection uses 23 o clock start and ehrwald stations`() {
        val projection = calculator.project(
            estimate = RaceDefinitions.byId(RaceDefinitions.EhrwaldTrailId).defaultEstimate(),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        assertEquals(9, projection.stations.size)
        assertEquals("Z3 Pestkapelle", projection.stations.first().station.name)
        assertEquals("Ziel Garmisch", projection.stations.last().station.name)
        assertEquals("17:00-20:00", projection.stations.last().window)
    }

    @Test
    fun `leutasch projection uses saturday 9 o clock start and leutasch stations`() {
        val projection = calculator.project(
            estimate = RaceDefinitions.byId(RaceDefinitions.LeutaschTrailId).defaultEstimate(),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        assertEquals(7, projection.stations.size)
        assertEquals("Z5 Hubertushof", projection.stations.first().station.name)
        assertEquals("Ziel Garmisch", projection.stations.last().station.name)
        assertEquals("21:00-01:00", projection.stations.last().window)
    }

    @Test
    fun `mittenwald projection uses saturday 7 o clock start and mittenwald stations`() {
        val projection = calculator.project(
            estimate = RaceDefinitions.byId(RaceDefinitions.MittenwaldTrailId).defaultEstimate(),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        assertEquals(5, projection.stations.size)
        assertEquals("Z7 Schloss Elmau", projection.stations.first().station.name)
        assertEquals("Ziel Garmisch", projection.stations.last().station.name)
        assertEquals("15:00-17:00", projection.stations.last().window)
    }

    @Test
    fun `garmisch partenkirchen projection uses friday 10 o clock start and garmisch stations`() {
        val projection = calculator.project(
            estimate = RaceDefinitions.byId(RaceDefinitions.GarmischPartenkirchenTrailId).defaultEstimate(),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        assertEquals(4, projection.stations.size)
        assertEquals("Z8 Laubhütte", projection.stations.first().station.name)
        assertEquals("Ziel Garmisch", projection.stations.last().station.name)
        assertEquals("15:00-17:00", projection.stations.last().window)
    }

    @Test
    fun `grainau projection uses saturday 18 o clock start and grainau stations`() {
        val projection = calculator.project(
            estimate = RaceDefinitions.byId(RaceDefinitions.GrainauTrailId).defaultEstimate(),
            checkIns = emptyList(),
            selectedIndex = 0,
        )

        assertEquals(2, projection.stations.size)
        assertEquals("Z10 Tröglift", projection.stations.first().station.name)
        assertEquals("Ziel Garmisch", projection.stations.last().station.name)
        assertEquals("20:00-22:00", projection.stations.last().window)
    }

    @Test
    fun `segment factors shift intermediate station timings`() {
        val stations = listOf(
            AidStation(1, "A", "Start", 10.0, 47.0, 11.0, 10.0, 0, 0, 0, 0, 0, 0),
            AidStation(2, "B", "A", 20.0, 47.1, 11.1, 10.0, 0, 0, 0, 0, 0, 0),
        )

        val withoutFactors = RaceCalculator(segmentFactors = emptyMap()).project(
            estimate = RaceEstimate(
                targetMode = TargetTimeMode.Fixed,
                fixedDurationMinutes = 100,
            ),
            checkIns = emptyList(),
            selectedIndex = 0,
            stations = stations,
        )
        val withFactors = RaceCalculator(segmentFactors = mapOf(1 to 2.0, 2 to 1.0)).project(
            estimate = RaceEstimate(
                targetMode = TargetTimeMode.Fixed,
                fixedDurationMinutes = 100,
            ),
            checkIns = emptyList(),
            selectedIndex = 0,
            stations = stations,
        )

        assertEquals(50, withoutFactors.stations[0].station.plannedArrivalMinutes)
        assertEquals(67, withFactors.stations[0].station.plannedArrivalMinutes)
        assertEquals(100, withFactors.stations[1].station.plannedArrivalMinutes)
    }

    @Test
    fun `latest check-in shifts future stations from effort based plan`() {
        val baseProjection = calculator.project(RaceEstimate(), emptyList(), selectedIndex = 0)
        val checkedStation = baseProjection.stations[2].station
        val actualArrivalMinutes = checkedStation.plannedArrivalMinutes + 12

        val projection = calculator.project(
            estimate = RaceEstimate(),
            checkIns = listOf(CheckIn(checkedStation.section, actualArrivalMinutes)),
            selectedIndex = 3,
        )

        assertEquals(12, projection.activeShiftMinutes)
        assertEquals(checkedStation.plannedArrivalMinutes, projection.stations[2].station.plannedArrivalMinutes)
        assertEquals(formatRaceTime(RaceEstimate().startTimeMinutes + actualArrivalMinutes), projection.stations[2].actualArrival)
        assertEquals(
            baseProjection.stations[3].station.plannedArrivalMinutes + 12,
            projection.stations[3].station.plannedArrivalMinutes,
        )
    }

    @Test
    fun `checkout extends future station timings by actual stop duration`() {
        val baseProjection = calculator.project(RaceEstimate(), emptyList(), selectedIndex = 0)
        val checkedStation = baseProjection.stations[2].station
        val actualArrivalMinutes = checkedStation.plannedArrivalMinutes
        val actualDepartureMinutes = actualArrivalMinutes + checkedStation.stopMinutes + 18

        val projection = calculator.project(
            estimate = RaceEstimate(),
            checkIns = listOf(
                CheckIn(
                    stationSection = checkedStation.section,
                    actualArrivalMinutes = actualArrivalMinutes,
                    actualDepartureMinutes = actualDepartureMinutes,
                ),
            ),
            selectedIndex = 3,
        )

        assertEquals(18, projection.activeShiftMinutes)
        assertEquals(18, projection.stations[3].station.plannedArrivalMinutes - baseProjection.stations[3].station.plannedArrivalMinutes)
        assertEquals(checkedStation.stopMinutes + 18, projection.stations[2].actualStopMinutes)
        assertEquals(formatRaceTime(RaceEstimate().startTimeMinutes + actualDepartureMinutes), projection.stations[2].actualDeparture)
    }
}
