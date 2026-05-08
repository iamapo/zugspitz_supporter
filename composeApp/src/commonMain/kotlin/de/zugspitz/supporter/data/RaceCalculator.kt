package de.zugspitz.supporter.data

import kotlin.math.roundToInt

class RaceCalculator {
    fun project(
        estimate: RaceEstimate,
        checkIns: List<CheckIn>,
        selectedIndex: Int,
        stations: List<AidStation> = ZugspitzStations,
    ): RaceProjection {
        val effectiveDurationMinutes = if (estimate.targetMode == TargetTimeMode.Fixed) {
            estimate.fixedDurationMinutes
        } else {
            estimate.minDurationMinutes
        }
        val targetScale = effectiveDurationMinutes.toDouble() / (17 * 60)
        val rangeExtra = if (estimate.targetMode == TargetTimeMode.Fixed) {
            0
        } else {
            estimate.maxDurationMinutes - estimate.minDurationMinutes
        }
        val scaledStations = stations.map { station ->
            val scaledPlan = (station.plannedArrivalMinutes * targetScale).roundToInt()
            val spreadStart = (station.windowStartMinutes * targetScale).roundToInt()
            val spreadEnd = spreadStart + ((station.windowEndMinutes - station.windowStartMinutes) + rangeExtra / 24)
            station.copy(
                plannedArrivalMinutes = scaledPlan,
                windowStartMinutes = spreadStart,
                windowEndMinutes = spreadEnd,
            )
        }

        val latestCheckIn = checkIns.maxByOrNull { it.stationSection }
        val shift = latestCheckIn?.let { checkIn ->
            val station = scaledStations.first { it.section == checkIn.stationSection }
            checkIn.actualArrivalMinutes - station.plannedArrivalMinutes
        } ?: 0

        val projections = scaledStations.mapIndexed { index, station ->
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
                station = station,
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
}

fun formatRaceTime(minutes: Int): String {
    val normalized = ((minutes % (24 * 60)) + (24 * 60)) % (24 * 60)
    val hours = normalized / 60
    val mins = normalized % 60
    return "${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}"
}
