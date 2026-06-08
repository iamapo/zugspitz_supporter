package de.zugspitz.supporter.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import de.zugspitz.supporter.RunnerLocationProvider
import de.zugspitz.supporter.data.gpxPathForRace
import de.zugspitz.supporter.data.matchLocationToRoute
import de.zugspitz.supporter.data.parseRouteSamples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import zugspitz_supporter.composeapp.generated.resources.Res

data class RouteLocationUiState(
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val elevationMeters: Double,
    val distanceFromRouteMeters: Double,
    val accuracyMeters: Double?,
    val isOnRoute: Boolean,
    val updatedAtEpochMillis: Long,
)

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun rememberRouteLocationTrackingState(
    raceId: String,
    locationProvider: RunnerLocationProvider,
    enabled: Boolean,
): State<RouteLocationUiState?> {
    return produceState<RouteLocationUiState?>(initialValue = null, key1 = raceId, key2 = locationProvider, key3 = enabled) {
        if (!enabled) {
            value = null
            return@produceState
        }
        val routeSamples = withContext(Dispatchers.Default) {
            runCatching {
                gpxPathForRace(raceId)
                    ?.let { Res.readBytes(it).decodeToString() }
                    ?.let(::parseRouteSamples)
                    .orEmpty()
            }.getOrElse { emptyList() }
        }
        if (routeSamples.size < 2) return@produceState

        var lastAcceptedAt = 0L
        locationProvider.start()
        try {
            locationProvider.locations.collect { location ->
                val shouldAccept = lastAcceptedAt == 0L ||
                    location.timestampEpochMillis - lastAcceptedAt >= LocationUpdateIntervalMillis
                if (!shouldAccept) return@collect

                val matched = withContext(Dispatchers.Default) {
                    matchLocationToRoute(
                        routeSamples = routeSamples,
                        latitude = location.latitude,
                        longitude = location.longitude,
                    )
                } ?: return@collect
                lastAcceptedAt = location.timestampEpochMillis
                value = RouteLocationUiState(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    distanceKm = matched.distanceKm,
                    elevationMeters = matched.elevationMeters,
                    distanceFromRouteMeters = matched.distanceFromRouteMeters,
                    accuracyMeters = location.horizontalAccuracyMeters,
                    isOnRoute = matched.isOnRoute,
                    updatedAtEpochMillis = location.timestampEpochMillis,
                )
            }
        } finally {
            locationProvider.stop()
        }
    }
}

private const val LocationUpdateIntervalMillis = 5 * 60 * 1000L
