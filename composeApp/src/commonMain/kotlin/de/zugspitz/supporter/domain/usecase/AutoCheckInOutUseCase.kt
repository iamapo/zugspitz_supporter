package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceProjection
import kotlin.math.abs

class AutoCheckInOutUseCase {
    operator fun invoke(
        projection: RaceProjection,
        location: LiveRunnerLocation,
        enabled: Boolean,
        canPublish: Boolean,
        alreadyAutoCheckedIn: Set<Int>,
        alreadyAutoCheckedOut: Set<Int>,
        raceMinutes: Int,
    ): AutoCheckAction {
        if (!enabled || !canPublish) return AutoCheckAction.None
        if (!location.isOnRoute || location.raceId != projection.estimate.raceId) return AutoCheckAction.None
        val accuracy = location.accuracyMeters ?: return AutoCheckAction.None
        if (accuracy > MaxAccuracyMeters) {
            return AutoCheckAction.SkippedLowAccuracy(accuracyMeters = accuracy)
        }

        val checkedInOpenStation = projection.stations.firstOrNull { it.isCheckedIn && !it.isCheckedOut }
        if (checkedInOpenStation != null) {
            val stationIndex = projection.stations.indexOfFirst {
                it.station.section == checkedInOpenStation.station.section
            }
            if (stationIndex < 0) return AutoCheckAction.None
            if (alreadyAutoCheckedOut.contains(checkedInOpenStation.station.section)) return AutoCheckAction.None

            val arrivalMinutes = checkedInOpenStation.actualArrivalMinutes ?: return AutoCheckAction.None
            if (raceMinutes - arrivalMinutes < CheckOutMinStopMinutes) return AutoCheckAction.None

            val distancePastStationKm = location.distanceKm - checkedInOpenStation.station.totalKm
            if (distancePastStationKm < CheckOutDistancePastVpKm) return AutoCheckAction.None

            return AutoCheckAction.CheckOut(
                stationIndex = stationIndex,
                stationSection = checkedInOpenStation.station.section,
                raceMinutes = raceMinutes,
                distancePastStationKm = distancePastStationKm,
            )
        }

        val nextOpenIndex = projection.stations.indexOfFirst { !it.isCheckedIn }
        if (nextOpenIndex < 0) return AutoCheckAction.None
        val nextOpenStation = projection.stations[nextOpenIndex].station
        if (alreadyAutoCheckedIn.contains(nextOpenStation.section)) return AutoCheckAction.None

        val distanceDeltaKm = abs(location.distanceKm - nextOpenStation.totalKm)
        if (distanceDeltaKm > CheckInRadiusKm) return AutoCheckAction.None

        return AutoCheckAction.CheckIn(
            stationIndex = nextOpenIndex,
            stationSection = nextOpenStation.section,
            raceMinutes = raceMinutes,
            distanceDeltaKm = distanceDeltaKm,
        )
    }

    companion object {
        const val CheckInRadiusKm = 0.2
        const val CheckOutDistancePastVpKm = 0.4
        const val CheckOutMinStopMinutes = 2
        const val MaxAccuracyMeters = 50.0
    }
}

sealed interface AutoCheckAction {
    data object None : AutoCheckAction

    data class SkippedLowAccuracy(
        val accuracyMeters: Double,
    ) : AutoCheckAction

    data class CheckIn(
        val stationIndex: Int,
        val stationSection: Int,
        val raceMinutes: Int,
        val distanceDeltaKm: Double,
    ) : AutoCheckAction

    data class CheckOut(
        val stationIndex: Int,
        val stationSection: Int,
        val raceMinutes: Int,
        val distancePastStationKm: Double,
    ) : AutoCheckAction
}
