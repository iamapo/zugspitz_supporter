package de.zugspitz.supporter.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import de.zugspitz.supporter.LiveSharingLogger
import de.zugspitz.supporter.RunnerLocationProvider
import de.zugspitz.supporter.data.gpxPathForRace
import de.zugspitz.supporter.data.matchLocationToRoute
import de.zugspitz.supporter.data.parseRouteSamples
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onMatchedLocation: (RouteLocationUiState) -> Unit = {},
    onAcceptedLocation: (RouteLocationUiState) -> Unit = {},
): State<RouteLocationUiState?> {
    val currentOnMatchedLocation = rememberUpdatedState(onMatchedLocation)
    val currentOnAcceptedLocation = rememberUpdatedState(onAcceptedLocation)
    return produceState<RouteLocationUiState?>(
        initialValue = null,
        key1 = raceId,
        key2 = locationProvider,
        key3 = enabled,
    ) {
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
        var pendingOnRouteLocation: RouteLocationUiState? = null
        var pendingPublishJob: Job? = null
        locationProvider.start()
        try {
            locationProvider.locations.collect { location ->
                val matched = withContext(Dispatchers.Default) {
                    matchLocationToRoute(
                        routeSamples = routeSamples,
                        latitude = location.latitude,
                        longitude = location.longitude,
                    )
                } ?: return@collect
                LiveSharingLogger.d(
                    "Matched runner location raceId=$raceId km=${matched.distanceKm.formatForLog()} " +
                        "routeDistanceMeters=${matched.distanceFromRouteMeters.formatForLog()} " +
                        "accuracyMeters=${location.horizontalAccuracyMeters?.formatForLog() ?: "unknown"} " +
                        "isOnRoute=${matched.isOnRoute}",
                )
                if (!matched.isOnRoute) {
                    pendingOnRouteLocation = null
                    pendingPublishJob?.cancel()
                    pendingPublishJob = null
                    value = RouteLocationUiState(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        distanceKm = matched.distanceKm,
                        elevationMeters = matched.elevationMeters,
                        distanceFromRouteMeters = matched.distanceFromRouteMeters,
                        accuracyMeters = location.horizontalAccuracyMeters,
                        isOnRoute = false,
                        updatedAtEpochMillis = location.timestampEpochMillis,
                    )
                    return@collect
                }

                val routeLocation = RouteLocationUiState(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    distanceKm = matched.distanceKm,
                    elevationMeters = matched.elevationMeters,
                    distanceFromRouteMeters = matched.distanceFromRouteMeters,
                    accuracyMeters = location.horizontalAccuracyMeters,
                    isOnRoute = true,
                    updatedAtEpochMillis = location.timestampEpochMillis,
                )
                currentOnMatchedLocation.value(routeLocation)
                val shouldAccept = lastAcceptedAt == 0L ||
                    location.timestampEpochMillis - lastAcceptedAt >= LocationUpdateIntervalMillis
                if (!shouldAccept) {
                    pendingOnRouteLocation = routeLocation
                    val remainingDelay = (LocationUpdateIntervalMillis - (location.timestampEpochMillis - lastAcceptedAt))
                        .coerceAtLeast(0L)
                    LiveSharingLogger.d(
                        "Delaying runner location publish because update interval has not elapsed. " +
                            "remainingMs=$remainingDelay",
                    )
                    if (pendingPublishJob?.isActive != true) {
                        pendingPublishJob = launch {
                            delay(remainingDelay)
                            pendingOnRouteLocation?.let { pendingLocation ->
                                LiveSharingLogger.d(
                                    "Publishing latest delayed runner location km=${pendingLocation.distanceKm.formatForLog()}.",
                                )
                                lastAcceptedAt = pendingLocation.updatedAtEpochMillis
                                currentOnAcceptedLocation.value(pendingLocation)
                                value = pendingLocation
                                pendingOnRouteLocation = null
                            }
                            pendingPublishJob = null
                        }
                    }
                    return@collect
                }

                pendingOnRouteLocation = null
                pendingPublishJob?.cancel()
                pendingPublishJob = null
                lastAcceptedAt = location.timestampEpochMillis
                currentOnAcceptedLocation.value(routeLocation)
                value = routeLocation
            }
        } finally {
            pendingPublishJob?.cancel()
            locationProvider.stop()
        }
    }
}

private const val LocationUpdateIntervalMillis = 5 * 60 * 1000L

private fun Double.formatForLog(): String = (this * 10.0).toInt().let { roundedTenths ->
    "${roundedTenths / 10}.${roundedTenths % 10}"
}
