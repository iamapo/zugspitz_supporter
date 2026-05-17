package de.zugspitz.supporter.data

import kotlin.math.roundToInt

class RaceCalculator(
    private val segmentFactors: Map<Int, Double>? = null,
) {
    fun project(
        estimate: RaceEstimate,
        checkIns: List<CheckIn>,
        selectedIndex: Int,
        stations: List<AidStation>? = null,
    ): RaceProjection {
        val race = RaceDefinitions.byId(estimate.raceId)
        val raceStations = stations ?: race.stations
        val raceSegmentFactors = segmentFactors ?: race.segmentFactors
        val minDurationMinutes = if (estimate.targetMode == TargetTimeMode.Fixed) {
            estimate.fixedDurationMinutes
        } else {
            estimate.minDurationMinutes
        }
        val maxDurationMinutes = if (estimate.targetMode == TargetTimeMode.Fixed) {
            estimate.fixedDurationMinutes
        } else {
            estimate.maxDurationMinutes
        }
        val plannedStations = calculateStations(
            stations = raceStations,
            segmentFactors = raceSegmentFactors,
            plannedDurationMinutes = minDurationMinutes,
            windowEndDurationMinutes = maxDurationMinutes,
        )

        val startCheckIn = checkIns.firstOrNull { it.stationSection == START_LINE_SECTION }
        val startOffsetMinutes = startCheckIn?.actualArrivalMinutes ?: 0
        val latestStationCheckIn = checkIns
            .filter { it.stationSection != START_LINE_SECTION }
            .maxByOrNull { it.stationSection }
        val performanceShiftMinutes = latestStationCheckIn?.let { checkIn ->
            val station = plannedStations.first { it.section == checkIn.stationSection }
            val plannedDepartureMinutes = station.plannedArrivalMinutes + startOffsetMinutes + station.stopMinutes
            val actualDepartureMinutes = checkIn.actualDepartureMinutes ?: (checkIn.actualArrivalMinutes + station.stopMinutes)
            actualDepartureMinutes - plannedDepartureMinutes
        } ?: 0

        val projections = plannedStations.mapIndexed { index, station ->
            val checkIn = checkIns.firstOrNull { it.stationSection == station.section }
            val stationShiftMinutes = (if (startCheckIn != null) startOffsetMinutes else 0) +
                if (latestStationCheckIn != null && station.section > latestStationCheckIn.stationSection) {
                    performanceShiftMinutes
                } else {
                    0
                }
            val projectedStation = if (stationShiftMinutes != 0) {
                station.copy(
                    plannedArrivalMinutes = station.plannedArrivalMinutes + stationShiftMinutes,
                    windowStartMinutes = station.windowStartMinutes + stationShiftMinutes,
                    windowEndMinutes = station.windowEndMinutes + stationShiftMinutes,
                )
            } else {
                station
            }
            val plannedDepartureMinutes = (checkIn?.actualArrivalMinutes ?: projectedStation.plannedArrivalMinutes) +
                projectedStation.stopMinutes
            StationProjection(
                station = projectedStation,
                projectedArrivalMinutes = projectedStation.plannedArrivalMinutes,
                actualArrivalMinutes = checkIn?.actualArrivalMinutes,
                actualDepartureMinutes = checkIn?.actualDepartureMinutes,
                plannedArrival = formatRaceTime(estimate.startTimeMinutes + projectedStation.plannedArrivalMinutes),
                window = "${formatRaceTime(estimate.startTimeMinutes + projectedStation.windowStartMinutes)}-${formatRaceTime(estimate.startTimeMinutes + projectedStation.windowEndMinutes)}",
                windowStart = formatRaceTime(estimate.startTimeMinutes + projectedStation.windowStartMinutes),
                windowEnd = formatRaceTime(estimate.startTimeMinutes + projectedStation.windowEndMinutes),
                actualArrival = checkIn?.let { formatRaceTime(estimate.startTimeMinutes + it.actualArrivalMinutes) },
                actualDeparture = checkIn?.actualDepartureMinutes?.let { formatRaceTime(estimate.startTimeMinutes + it) },
                plannedDeparture = formatRaceTime(estimate.startTimeMinutes + plannedDepartureMinutes),
                actualStopMinutes = checkIn?.actualDepartureMinutes?.let { it - checkIn.actualArrivalMinutes },
                diffMinutes = checkIn?.let {
                    it.actualArrivalMinutes - projectedStation.plannedArrivalMinutes
                } ?: if (latestStationCheckIn != null && station.section > latestStationCheckIn.stationSection) {
                    performanceShiftMinutes
                } else {
                    0
                },
                isCheckedIn = checkIn != null,
                isCheckedOut = checkIn?.actualDepartureMinutes != null,
                isCurrent = index == selectedIndex,
            )
        }

        return RaceProjection(estimate, projections, performanceShiftMinutes, startCheckIn?.actualArrivalMinutes)
    }

    private fun calculateStations(
        stations: List<AidStation>,
        segmentFactors: Map<Int, Double>,
        plannedDurationMinutes: Int,
        windowEndDurationMinutes: Int,
        metersUpPerKm: Double = 100.0,
        metersDownPerKm: Double = 300.0,
    ): List<AidStation> {
        require(stations.isNotEmpty()) { "stations must not be empty" }

        val totalStopMinutes = stations.sumOf { it.stopMinutes }
        val plannedMovingMinutes = plannedDurationMinutes - totalStopMinutes
        val windowEndMovingMinutes = windowEndDurationMinutes - totalStopMinutes

        require(plannedMovingMinutes > 0) {
            "Target total time must be greater than total stop time"
        }
        require(windowEndMovingMinutes > 0) {
            "Window end time must be greater than total stop time"
        }

        val totalEffortKm = stations.sumOf { station ->
            station.effortKm(segmentFactors, metersUpPerKm, metersDownPerKm)
        }
        require(totalEffortKm > 0.0) { "total effort must be greater than zero" }

        var cumulativeEffortKm = 0.0
        var stopMinutesBeforeStation = 0

        return stations.map { station ->
            cumulativeEffortKm += station.effortKm(segmentFactors, metersUpPerKm, metersDownPerKm)

            val plannedArrivalMinutes = stopMinutesBeforeStation +
                proportionalMinutes(plannedMovingMinutes, cumulativeEffortKm, totalEffortKm)
            val windowEndMinutes = stopMinutesBeforeStation +
                proportionalMinutes(windowEndMovingMinutes, cumulativeEffortKm, totalEffortKm)

            stopMinutesBeforeStation += station.stopMinutes

            station.copy(
                plannedArrivalMinutes = plannedArrivalMinutes,
                windowStartMinutes = plannedArrivalMinutes,
                windowEndMinutes = windowEndMinutes,
            )
        }
    }

    private fun AidStation.effortKm(
        segmentFactors: Map<Int, Double>,
        metersUpPerKm: Double,
        metersDownPerKm: Double,
    ): Double {
        val baseEffortKm = sectionKm + climbMeters / metersUpPerKm + descentMeters / metersDownPerKm
        val factor = segmentFactors[section] ?: 1.0
        return baseEffortKm * factor
    }

    private fun proportionalMinutes(
        totalMovingMinutes: Int,
        cumulativeEffortKm: Double,
        totalEffortKm: Double,
    ): Int = (totalMovingMinutes * cumulativeEffortKm / totalEffortKm).roundToInt()
}

fun formatRaceTime(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hours = normalized / 60
    val mins = normalized % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}
