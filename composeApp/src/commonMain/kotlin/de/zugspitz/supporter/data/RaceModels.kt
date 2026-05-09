package de.zugspitz.supporter.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class RaceEstimate(
    val startTimeMinutes: Int = 22 * 60,
    val targetMode: TargetTimeMode = TargetTimeMode.Range,
    val fixedDurationMinutes: Int = 17 * 60,
    val minDurationMinutes: Int = 17 * 60,
    val maxDurationMinutes: Int = 18 * 60,
)

@Serializable
enum class TargetTimeMode {
    Fixed,
    Range,
}

@Immutable
data class AidStation(
    val section: Int,
    val name: String,
    val from: String,
    val totalKm: Double,
    val sectionKm: Double,
    val climbMeters: Int,
    val descentMeters: Int,
    val plannedArrivalMinutes: Int,
    val windowStartMinutes: Int,
    val windowEndMinutes: Int,
    val stopMinutes: Int,
)

@Immutable
@Serializable
data class CheckIn(
    val stationSection: Int,
    val actualArrivalMinutes: Int,
    val actualDepartureMinutes: Int? = null,
)

@Immutable
data class StationProjection(
    val station: AidStation,
    val projectedArrivalMinutes: Int,
    val actualArrivalMinutes: Int?,
    val actualDepartureMinutes: Int?,
    val plannedArrival: String,
    val window: String,
    val windowStart: String,
    val windowEnd: String,
    val actualArrival: String?,
    val actualDeparture: String?,
    val actualStopMinutes: Int?,
    val diffMinutes: Int,
    val isCheckedIn: Boolean,
    val isCheckedOut: Boolean,
    val isCurrent: Boolean,
)

@Immutable
data class RaceProjection(
    val estimate: RaceEstimate,
    val stations: List<StationProjection>,
    val activeShiftMinutes: Int,
)
