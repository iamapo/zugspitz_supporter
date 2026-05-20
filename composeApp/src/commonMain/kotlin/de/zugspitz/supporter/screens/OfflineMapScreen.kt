package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.data.AidStation
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.presentation.state.OfflineMapUiState
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.map_download
import zugspitz_supporter.composeapp.generated.resources.map_offline_hint
import zugspitz_supporter.composeapp.generated.resources.map_offline_title
import zugspitz_supporter.composeapp.generated.resources.map_preparing
import zugspitz_supporter.composeapp.generated.resources.map_ready
import zugspitz_supporter.composeapp.generated.resources.map_tiles_progress
import zugspitz_supporter.composeapp.generated.resources.tab_map
import kotlin.math.round

@Composable
fun OfflineMapScreen(
    projection: RaceProjection,
    offlineMap: OfflineMapUiState,
    onDownloadStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            eyebrow = projection.estimate.raceId.uppercase(),
            title = stringResource(Res.string.tab_map),
            subtitle = stringResource(Res.string.map_offline_hint),
            pill = if (offlineMap.isReady) stringResource(Res.string.map_ready) else null,
        )

        val focusStation = projection.stations
            .lastOrNull { it.isCheckedIn }
            ?.station
            ?: projection.stations.firstOrNull()?.station
        val vpBounds = rememberVpBounds(projection)
        val routeGeoJson by produceState<String?>(initialValue = null, key1 = projection.estimate.raceId) {
            val gpxPath = gpxPathForRace(projection.estimate.raceId)
            value = runCatching {
                gpxPath
                    ?.let { Res.readBytes(it).decodeToString() }
                    ?.let(::parseRouteLineGeoJson)
                    ?.takeIf(::geoJsonHasCoordinates)
                    ?: buildRouteFallbackGeoJson(projection.stations.map { it.station })
            }.getOrNull() ?: buildRouteFallbackGeoJson(projection.stations.map { it.station })
        }
        val cameraState = rememberCameraState(
            firstPosition = CameraPosition(
                target = Position(
                    longitude = focusStation?.longitude ?: 11.092212,
                    latitude = focusStation?.latitude ?: 47.494648,
                ),
                zoom = 10.0,
            ),
        )
        val uncheckedPointsJson = rememberVpPointsGeoJson(
            projection = projection,
            onlyCheckedIn = false,
        )
        val checkedInPointsJson = rememberVpPointsGeoJson(
            projection = projection,
            onlyCheckedIn = true,
        )
        var mapReady by remember { mutableStateOf(false) }
        Card {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
                    cameraState = cameraState,
                    boundingBox = vpBounds,
                    onMapLoadFinished = { mapReady = true },
                    onMapLoadFailed = { mapReady = true },
                ) {
                    routeGeoJson?.let { routeJson ->
                        val routeSource = rememberGeoJsonSource(
                            data = GeoJsonData.JsonString(routeJson),
                        )
                        LineLayer(
                            id = "race-route-line",
                            source = routeSource,
                            color = const(Color(0xFF2A9D8F)),
                            width = const(3.dp),
                        )
                    }
                    val uncheckedVpSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(uncheckedPointsJson),
                    )
                    CircleLayer(
                        id = "vp-circles-unchecked",
                        source = uncheckedVpSource,
                        color = const(Color(0xFF264653)),
                        radius = const(6.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                    )
                    val checkedInVpSource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(checkedInPointsJson),
                    )
                    CircleLayer(
                        id = "vp-circles-checked",
                        source = checkedInVpSource,
                        color = const(Color(0xFFD62828)),
                        radius = const(7.dp),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp),
                    )
                }
                if (!mapReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF4F1E8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(Res.string.map_preparing), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.map_offline_title), fontWeight = FontWeight.Bold)
                if (offlineMap.isDownloading || offlineMap.isReady) {
                    LinearProgressIndicator(
                        progress = { (offlineMap.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val downloadedMb = round(offlineMap.downloadedMegabytes * 10.0) / 10.0
                    Text(
                        stringResource(
                            Res.string.map_tiles_progress,
                            offlineMap.progressPercent,
                            offlineMap.downloadedTiles,
                            offlineMap.totalTiles,
                            downloadedMb.toString(),
                        ),
                    )
                }
                Button(
                    onClick = onDownloadStart,
                    enabled = !offlineMap.isDownloading,
                ) {
                    Text(stringResource(Res.string.map_download))
                }
            }
        }
    }
}

@Composable
private fun rememberVpBounds(projection: RaceProjection): BoundingBox? {
    if (projection.stations.size < 2) return null
    val longitudes = projection.stations.map { it.station.longitude }
    val latitudes = projection.stations.map { it.station.latitude }
    val west = longitudes.minOrNull() ?: return null
    val east = longitudes.maxOrNull() ?: return null
    val south = latitudes.minOrNull() ?: return null
    val north = latitudes.maxOrNull() ?: return null
    if (west == east || south == north) return null
    return BoundingBox(west = west, south = south, east = east, north = north)
}

private fun gpxPathForRace(raceId: String): String? = when (raceId) {
    "zugspitz-ultratrail" -> "files/gpx/Ultratrail_ZUT_2025_3b6cbaa510.gpx"
    "mittenwald-trail" -> "files/gpx/Mittenwald_Trail_ZUT_2025_fa6c0d4010.gpx"
    "leutasch-trail" -> "files/gpx/Leutasch_Trail_ZUT_2025_620e36ae36.gpx"
    "zut-100" -> "files/gpx/ZUT_100_2026_Start_Ga_Pa_5bcee57cdf.gpx"
    "ehrwald-trail" -> "files/gpx/Ehrwald_Trail_ZUT_2025_85a841b963.gpx"
    "garmisch-partenkirchen-trail" -> "files/gpx/Garmisch_Partenkirchen_Trail_ZUT_2025_1d59df01ee.gpx"
    else -> null
}

internal fun parseRouteLineGeoJson(gpxContent: String): String {
    val pointTagRegex = Regex("""<(?:trkpt|rtept)\b([^>]*)>""")
    val attributeRegex = Regex("""([a-zA-Z]+)="([^"]+)"""")
    val coordinates = pointTagRegex.findAll(gpxContent)
        .mapNotNull { match ->
            val attributes = attributeRegex.findAll(match.groupValues[1]).associate { it.groupValues[1] to it.groupValues[2] }
            val latitude = attributes["lat"]?.toDoubleOrNull()
            val longitude = attributes["lon"]?.toDoubleOrNull()
            if (latitude == null || longitude == null) null else "[${longitude},${latitude}]"
        }
        .filterNotNull()
        .toList()

    return buildLineStringGeoJson(coordinates)
}

internal fun buildRouteFallbackGeoJson(stations: List<AidStation>): String {
    val coordinates = stations
        .asSequence()
        .map { station -> station.longitude to station.latitude }
        .filter { (longitude, latitude) -> longitude != 0.0 || latitude != 0.0 }
        .distinct()
        .map { (longitude, latitude) -> "[${longitude},${latitude}]" }
        .toList()
    return buildLineStringGeoJson(coordinates)
}

private fun buildLineStringGeoJson(coordinates: List<String>): String {
    if (coordinates.size < 2) return """{"type":"FeatureCollection","features":[]}"""
    return """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"kind":"route"},"geometry":{"type":"LineString","coordinates":[${coordinates.joinToString(",")}]} }]}"""
}

private fun geoJsonHasCoordinates(geoJson: String): Boolean = "\"coordinates\":[" in geoJson && geoJson.count { it == '[' } > 2

@Composable
private fun rememberVpPointsGeoJson(
    projection: RaceProjection,
    onlyCheckedIn: Boolean,
): String {
    val features = projection.stations.mapNotNull { stationProjection ->
        if (stationProjection.isCheckedIn != onlyCheckedIn) return@mapNotNull null
        val station = stationProjection.station
        val safeName = station.name.replace("\"", "\\\"")
        """{"type":"Feature","properties":{"name":"$safeName","section":${station.section}},"geometry":{"type":"Point","coordinates":[${station.longitude},${station.latitude}]}}"""
    }.joinToString(",")
    return """{"type":"FeatureCollection","features":[$features]}"""
}

@Preview
@Composable
fun OfflineMapScreenPreview() {
    SupporterTheme {
        OfflineMapScreen(
            projection = RaceCalculator().project(
                estimate = RaceEstimate(),
                checkIns = listOf(CheckIn(stationSection = 2, actualArrivalMinutes = 194)),
                selectedIndex = 1,
            ),
            offlineMap = OfflineMapUiState(
                isDownloading = true,
                progressPercent = 62,
                downloadedTiles = 1630,
                totalTiles = 2620,
                downloadedMegabytes = 19.6,
                isReady = false,
            ),
            onDownloadStart = {},
        )
    }
}
