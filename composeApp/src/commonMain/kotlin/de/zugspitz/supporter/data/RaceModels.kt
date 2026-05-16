package de.zugspitz.supporter.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

const val START_LINE_SECTION = 0

@Immutable
@Serializable
data class RaceEstimate(
    val raceId: String = RaceDefinitions.ZugspitzUltratrailId,
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
    val latitude: Double,
    val longitude: Double,
    val sectionKm: Double,
    val climbMeters: Int,
    val descentMeters: Int,
    val plannedArrivalMinutes: Int,
    val windowStartMinutes: Int,
    val windowEndMinutes: Int,
    val stopMinutes: Int,
) {
    constructor(
        section: Int,
        name: String,
        from: String,
        totalKm: Double,
        sectionKm: Double,
        climbMeters: Int,
        descentMeters: Int,
        plannedArrivalMinutes: Int,
        windowStartMinutes: Int,
        windowEndMinutes: Int,
        stopMinutes: Int,
    ) : this(
        section = section,
        name = name,
        from = from,
        totalKm = totalKm,
        latitude = defaultCoordinates(name).first,
        longitude = defaultCoordinates(name).second,
        sectionKm = sectionKm,
        climbMeters = climbMeters,
        descentMeters = descentMeters,
        plannedArrivalMinutes = plannedArrivalMinutes,
        windowStartMinutes = windowStartMinutes,
        windowEndMinutes = windowEndMinutes,
        stopMinutes = stopMinutes,
    )
}

private fun defaultCoordinates(stationName: String): Pair<Double, Double> = when (stationName) {
    "Z1 Eibsee" -> 47.455253 to 10.995159
    "Z2 Gamsalm" -> 47.414055 to 10.941956
    "Z3 Pestkapelle" -> 47.380021 to 10.982417
    "Z4 Hämmermoosalm" -> 47.370897 to 11.082222
    "Z5 Hubertushof" -> 47.400458 to 11.181670
    "Z6 Mittenwald" -> 47.425571 to 11.257963
    "Z7 Schloss Elmau" -> 47.462605 to 11.188721
    "Z8 Laubhütte" -> 47.443889 to 11.098065
    "Z9 Hochalm" -> 47.439385 to 11.061616
    "Z10 Tröglift" -> 47.461641 to 11.089332
    "Ziel Garmisch" -> 47.494648 to 11.092212
    else -> 0.0 to 0.0
}

@Immutable
data class RaceDefinition(
    val id: String,
    val name: String,
    val distanceLabel: String,
    val startLocation: String,
    val startTimeMinutes: Int,
    val defaultMinDurationMinutes: Int,
    val defaultMaxDurationMinutes: Int,
    val defaultFixedDurationMinutes: Int,
    val stations: List<AidStation>,
    val segmentFactors: Map<Int, Double> = emptyMap(),
) {
    fun defaultEstimate(): RaceEstimate = RaceEstimate(
        raceId = id,
        startTimeMinutes = startTimeMinutes,
        fixedDurationMinutes = defaultFixedDurationMinutes,
        minDurationMinutes = defaultMinDurationMinutes,
        maxDurationMinutes = defaultMaxDurationMinutes,
    )
}

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
    val plannedDeparture: String,
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
    val actualStartMinutes: Int?,
)
