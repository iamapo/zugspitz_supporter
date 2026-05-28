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

private data class GpxElevationPoint(
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Double,
)

private const val EarthRadiusKm = 6371.0

private val gpxElevationPointRegex = Regex(
    pattern = """<(?:trkpt|rtept)\b([^>]*)>(.*?)</(?:trkpt|rtept)>""",
    options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
)
private val elevationTagRegex = Regex("""<ele>([^<]+)</ele>""", RegexOption.IGNORE_CASE)

internal fun parseElevationProfile(gpxContent: String): List<ElevationSample> {
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
        ElevationSample(distanceKm, point.elevationMeters)
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
