package de.zugspitz.supporter.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import de.zugspitz.supporter.data.ElevationSample
import de.zugspitz.supporter.data.gpxPathForRace
import de.zugspitz.supporter.data.parseElevationProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zugspitz_supporter.composeapp.generated.resources.Res

@Composable
internal fun rememberRouteElevationProfile(raceId: String): List<ElevationSample> {
    val routeElevationProfile = produceState(
        initialValue = elevationProfileCache[raceId].orEmpty(),
        key1 = raceId,
    ) {
        elevationProfileCache[raceId]?.let { cachedProfile ->
            value = cachedProfile
            return@produceState
        }
        val parsedProfile = withContext(Dispatchers.Default) {
            runCatching {
                gpxPathForRace(raceId)
                    ?.let { Res.readBytes(it).decodeToString() }
                    ?.let(::parseElevationProfile)
                    .orEmpty()
            }.getOrDefault(emptyList())
        }
        elevationProfileCache[raceId] = parsedProfile
        value = parsedProfile
    }
    return routeElevationProfile.value
}

private val elevationProfileCache = mutableMapOf<String, List<ElevationSample>>()
