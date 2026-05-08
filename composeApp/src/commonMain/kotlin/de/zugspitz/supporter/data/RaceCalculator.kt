package de.zugspitz.supporter.data

import kotlin.math.roundToInt

class RaceCalculator(
    private val segmentFactors: Map<Int, Double> = ZutSegmentFactors,
) {
    fun project(
        estimate: RaceEstimate,
        checkIns: List<CheckIn>,
        selectedIndex: Int,
        stations: List<AidStation> = ZugspitzStations,
    ): RaceProjection {
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
            stations = stations,
            plannedDurationMinutes = minDurationMinutes,
            windowEndDurationMinutes = maxDurationMinutes,
        )

        val latestCheckIn = checkIns.maxByOrNull { it.stationSection }
        val shift = latestCheckIn?.let { checkIn ->
            val station = plannedStations.first { it.section == checkIn.stationSection }
            checkIn.actualArrivalMinutes - station.plannedArrivalMinutes
        } ?: 0

        val projections = plannedStations.mapIndexed { index, station ->
            val checkIn = checkIns.firstOrNull { it.stationSection == station.section }
            val shouldShift = latestCheckIn != null && station.section > latestCheckIn.stationSection
            val projectedStation = if (shouldShift) {
                station.copy(
                    plannedArrivalMinutes = station.plannedArrivalMinutes + shift,
                    windowStartMinutes = station.windowStartMinutes + shift,
                    windowEndMinutes = station.windowEndMinutes + shift,
                )
            } else {
                station
            }
            StationProjection(
                station = projectedStation,
                projectedArrivalMinutes = projectedStation.plannedArrivalMinutes,
                actualArrivalMinutes = checkIn?.actualArrivalMinutes,
                plannedArrival = formatRaceTime(estimate.startTimeMinutes + projectedStation.plannedArrivalMinutes),
                window = "${formatRaceTime(estimate.startTimeMinutes + projectedStation.windowStartMinutes)}-${formatRaceTime(estimate.startTimeMinutes + projectedStation.windowEndMinutes)}",
                windowStart = formatRaceTime(estimate.startTimeMinutes + projectedStation.windowStartMinutes),
                windowEnd = formatRaceTime(estimate.startTimeMinutes + projectedStation.windowEndMinutes),
                actualArrival = checkIn?.let { formatRaceTime(estimate.startTimeMinutes + it.actualArrivalMinutes) },
                diffMinutes = checkIn?.let { it.actualArrivalMinutes - station.plannedArrivalMinutes } ?: if (shouldShift) shift else 0,
                isDone = checkIn != null,
                isCurrent = index == selectedIndex,
            )
        }

        return RaceProjection(estimate, projections, shift)
    }

    private fun calculateStations(
        stations: List<AidStation>,
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
            station.effortKm(metersUpPerKm, metersDownPerKm)
        }
        require(totalEffortKm > 0.0) { "total effort must be greater than zero" }

        var cumulativeEffortKm = 0.0
        var stopMinutesBeforeStation = 0

        return stations.map { station ->
            cumulativeEffortKm += station.effortKm(metersUpPerKm, metersDownPerKm)

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

    companion object {
        private val ZutSegmentFactors = mapOf(
            1 to 0.82,
            2 to 0.81,
            3 to 0.77,
            4 to 0.99,
            5 to 0.98,
            6 to 1.08,
            7 to 1.06,
            8 to 1.12,
            9 to 1.23,
            10 to 1.21,
            11 to 1.02,
        )
    }
}

fun formatRaceTime(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hours = normalized / 60
    val mins = normalized % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}
