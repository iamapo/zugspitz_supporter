package de.zugspitz.supporter.data

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
data class ElevationSample(
    val distanceKm: Double,
    val elevationMeters: Double,
)

@Immutable
data class RouteSample(
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val elevationMeters: Double,
)

@Immutable
data class RouteLocationMatch(
    val distanceKm: Double,
    val elevationMeters: Double,
    val distanceFromRouteMeters: Double,
    val isOnRoute: Boolean,
)

private data class GpxElevationPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
)

private const val EarthRadiusKm = 6371.0
private const val MaxOnRouteDistanceMeters = 300.0

private val gpxElevationPointRegex = Regex(
    pattern = """<(?:trkpt|rtept)\b([^>]*)>(.*?)</(?:trkpt|rtept)>""",
    options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val elevationTagRegex = Regex("""<ele>([^<]+)</ele>""", RegexOption.IGNORE_CASE)

internal fun parseElevationProfile(gpxContent: String): List<ElevationSample> {
    return parseRouteSamples(gpxContent).map { sample ->
        ElevationSample(sample.distanceKm, sample.elevationMeters)
    }
}

internal fun parseRouteSamples(gpxContent: String): List<RouteSample> {
    val points = gpxElevationPointRegex
        .findAll(gpxContent)
        .mapNotNull { match ->
            val attributes = match.groupValues[1]
            val latitude = attributes.readXmlAttribute("lat")?.toDoubleOrNull()
            val longitude = attributes.readXmlAttribute("lon")?.toDoubleOrNull()
            val elevation = elevationTagRegex
                .find(match.groupValues[2])
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.toDoubleOrNull()

            if (latitude != null && longitude != null && elevation != null) {
                GpxElevationPoint(latitude, longitude, elevation)
            } else {
                null
            }
        }
        .toList()

    if (points.size < 2) return emptyList()

    var distanceKm = 0.0
    var previous = points.first()
    return points.mapIndexed { index, point ->
        if (index > 0) {
            distanceKm += haversineKm(previous, point)
            previous = point
        }
        RouteSample(
            latitude = point.latitude,
            longitude = point.longitude,
            distanceKm = distanceKm,
            elevationMeters = point.elevationMeters,
        )
    }
}

internal fun matchLocationToRoute(
    routeSamples: List<RouteSample>,
    latitude: Double,
    longitude: Double,
    maxOnRouteDistanceMeters: Double = MaxOnRouteDistanceMeters,
): RouteLocationMatch? {
    if (routeSamples.size < 2) return null

    val origin = routeSamples.minByOrNull {
        haversineKm(
            GpxElevationPoint(latitude, longitude, 0.0),
            GpxElevationPoint(it.latitude, it.longitude, it.elevationMeters),
        )
    } ?: return null
    val current = localPoint(latitude, longitude, origin.latitude, origin.longitude)

    var best: ProjectedRoutePoint? = null
    routeSamples.zipWithNext().forEach { (start, end) ->
        val projected = projectOnSegment(
            current = current,
            start = localPoint(start.latitude, start.longitude, origin.latitude, origin.longitude),
            end = localPoint(end.latitude, end.longitude, origin.latitude, origin.longitude),
            startSample = start,
            endSample = end,
        )
        if (best == null || projected.distanceFromRouteMeters < best.distanceFromRouteMeters) {
            best = projected
        }
    }

    return best?.let { projected ->
        RouteLocationMatch(
            distanceKm = projected.distanceKm,
            elevationMeters = projected.elevationMeters,
            distanceFromRouteMeters = projected.distanceFromRouteMeters,
            isOnRoute = projected.distanceFromRouteMeters <= maxOnRouteDistanceMeters,
        )
    }
}

internal fun sectionElevationProfile(
    routeProfile: List<ElevationSample>,
    fromKm: Double,
    toKm: Double,
): List<ElevationSample> {
    if (routeProfile.size < 2 || toKm <= fromKm) return emptyList()

    val startKm = fromKm.coerceAtLeast(routeProfile.first().distanceKm)
    val endKm = toKm.coerceAtMost(routeProfile.last().distanceKm)
    if (endKm <= startKm) return emptyList()

    val samples = mutableListOf<ElevationSample>()
    interpolateElevation(routeProfile, startKm)?.let(samples::add)
    routeProfile
        .filter { it.distanceKm > startKm && it.distanceKm < endKm }
        .forEach(samples::add)
    interpolateElevation(routeProfile, endKm)?.let(samples::add)

    return samples
        .distinctBy { it.distanceKm }
        .map { sample ->
            ElevationSample(
                distanceKm = sample.distanceKm - startKm,
                elevationMeters = sample.elevationMeters,
            )
        }
}

private fun interpolateElevation(
    profile: List<ElevationSample>,
    targetKm: Double,
): ElevationSample? {
    profile.firstOrNull { it.distanceKm == targetKm }?.let { return it }
    val rightIndex = profile.indexOfFirst { it.distanceKm > targetKm }
    if (rightIndex <= 0) return null
    val left = profile[rightIndex - 1]
    val right = profile[rightIndex]
    val span = right.distanceKm - left.distanceKm
    if (span <= 0.0) return left

    val fraction = (targetKm - left.distanceKm) / span
    return ElevationSample(
        distanceKm = targetKm,
        elevationMeters = left.elevationMeters + (right.elevationMeters - left.elevationMeters) * fraction,
    )
}

private fun String.readXmlAttribute(name: String): String? {
    val valuePrefix = "$name=\""
    val valueStart = indexOf(valuePrefix)
        .takeIf { it >= 0 }
        ?.plus(valuePrefix.length)
        ?: return null
    val valueEnd = indexOf('"', startIndex = valueStart).takeIf { it >= 0 } ?: return null
    return substring(valueStart, valueEnd)
}

private fun haversineKm(from: GpxElevationPoint, to: GpxElevationPoint): Double {
    val lat1 = from.latitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val deltaLat = (to.latitude - from.latitude).toRadians()
    val deltaLon = (to.longitude - from.longitude).toRadians()
    val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
    return EarthRadiusKm * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

private fun Double.toRadians(): Double = this * PI / 180.0

private data class LocalPoint(val xMeters: Double, val yMeters: Double)

private data class ProjectedRoutePoint(
    val distanceKm: Double,
    val elevationMeters: Double,
    val distanceFromRouteMeters: Double,
)

private fun localPoint(latitude: Double, longitude: Double, originLatitude: Double, originLongitude: Double): LocalPoint {
    val metersPerDegreeLatitude = 111_320.0
    val metersPerDegreeLongitude = metersPerDegreeLatitude * cos(originLatitude.toRadians())
    return LocalPoint(
        xMeters = (longitude - originLongitude) * metersPerDegreeLongitude,
        yMeters = (latitude - originLatitude) * metersPerDegreeLatitude,
    )
}

private fun projectOnSegment(
    current: LocalPoint,
    start: LocalPoint,
    end: LocalPoint,
    startSample: RouteSample,
    endSample: RouteSample,
): ProjectedRoutePoint {
    val segmentX = end.xMeters - start.xMeters
    val segmentY = end.yMeters - start.yMeters
    val segmentLengthSquared = segmentX * segmentX + segmentY * segmentY
    val rawFraction = if (segmentLengthSquared <= 0.0) {
        0.0
    } else {
        ((current.xMeters - start.xMeters) * segmentX + (current.yMeters - start.yMeters) * segmentY) /
            segmentLengthSquared
    }
    val fraction = rawFraction.coerceIn(0.0, 1.0)
    val projectedX = start.xMeters + segmentX * fraction
    val projectedY = start.yMeters + segmentY * fraction
    val distanceX = current.xMeters - projectedX
    val distanceY = current.yMeters - projectedY

    return ProjectedRoutePoint(
        distanceKm = startSample.distanceKm + (endSample.distanceKm - startSample.distanceKm) * fraction,
        elevationMeters = startSample.elevationMeters +
            (endSample.elevationMeters - startSample.elevationMeters) * fraction,
        distanceFromRouteMeters = sqrt(distanceX * distanceX + distanceY * distanceY),
    )
}
