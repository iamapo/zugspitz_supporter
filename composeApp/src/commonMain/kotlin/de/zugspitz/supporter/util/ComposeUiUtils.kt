package de.zugspitz.supporter.util

import de.zugspitz.supporter.data.AidStation
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.data.StationProjection
import kotlin.math.round

object ComposeUiUtils {
    fun minutesToDurationInput(totalMinutes: Int): String {
        val hours = (totalMinutes / 60).coerceAtLeast(0)
        val minutes = (totalMinutes % 60).coerceAtLeast(0)
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    fun parseDurationInput(input: String): Int? {
        val parts = input.split(":")
        if (parts.size != 2) return null

        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..72 || minutes !in 0..59) return null

        return (hours * 60) + minutes
    }

    fun completedElevation(projection: RaceProjection, selectedIndex: Int): Pair<Int, Int> {
        val stations = projection.stations.take(selectedIndex + 1).map { it.station }
        return stations.sumOf { it.climbMeters } to stations.sumOf { it.descentMeters }
    }

    fun likelyPace(from: StationProjection, to: StationProjection): String {
        val startMinutes = from.actualArrivalMinutes ?: from.projectedArrivalMinutes
        val segmentMinutes = (to.projectedArrivalMinutes - startMinutes).coerceAtLeast(1)
        val paceMinutesPerKm = segmentMinutes / to.station.sectionKm
        val totalSeconds = (paceMinutesPerKm * 60).toInt().coerceAtLeast(0)
        val paceMinutes = totalSeconds / 60
        val paceSeconds = totalSeconds % 60
        return "${paceMinutes}:${paceSeconds.toString().padStart(2, '0')}/km"
    }

    fun formattedKm(km: Double): String = "${oneDecimal(km)} km"

    fun averagePace(minutes: Int?, distanceKm: Double): String {
        if (minutes == null || distanceKm <= 0.0) return "-"
        return pace(minutes / distanceKm)
    }

    fun sectionPace(sectionDurationMinutes: Int?, sectionKm: Double): String {
        if (sectionDurationMinutes == null || sectionDurationMinutes <= 0 || sectionKm <= 0.0) return "-"
        return pace(sectionDurationMinutes / sectionKm)
    }

    fun sectionDurationMinutes(
        current: StationProjection,
        previous: StationProjection?,
        actualStartMinutes: Int? = null,
    ): Int? {
        val sectionStartMinutes = previous?.actualDepartureMinutes
            ?: previous?.actualArrivalMinutes
            ?: actualStartMinutes
            ?: 0
        return current.actualArrivalMinutes?.minus(sectionStartMinutes)
    }

    fun mapsUri(station: AidStation): String =
        "https://www.google.com/maps/search/?api=1&query=${station.latitude},${station.longitude}"

    private fun pace(minutesPerKm: Double): String {
        val totalSeconds = (minutesPerKm * 60).toInt().coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "$minutes:${seconds.toString().padStart(2, '0')} min/km"
    }

    private fun oneDecimal(value: Double): String {
        val rounded = round(value * 10.0) / 10.0
        val text = rounded.toString()
        return if (text.contains(".")) text else "$text.0"
    }
}
