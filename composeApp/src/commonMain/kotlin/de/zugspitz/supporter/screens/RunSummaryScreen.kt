package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ElevationProfileChart
import de.zugspitz.supporter.components.ElevationProfileLocationMarker
import de.zugspitz.supporter.components.ElevationProfileMarker
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.VpListRow
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.ElevationSample
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.data.StationProjection
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.current_label
import zugspitz_supporter.composeapp.generated.resources.summary_arrived_at
import zugspitz_supporter.composeapp.generated.resources.summary_expected_at
import zugspitz_supporter.composeapp.generated.resources.summary_route_profile
import zugspitz_supporter.composeapp.generated.resources.summary_started
import zugspitz_supporter.composeapp.generated.resources.summary_subtitle
import zugspitz_supporter.composeapp.generated.resources.summary_title

@Composable
fun RunOverviewScreen(
    projection: RaceProjection,
    runnerLocation: LiveRunnerLocation?,
    onStationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    previewRouteElevationProfile: List<ElevationSample>? = null,
) {
    val race = RaceDefinitions.byId(projection.estimate.raceId)
    val paceShiftMinutes = -projection.activeShiftMinutes
    val shiftPill = if (paceShiftMinutes == 0) null else {
        val sign = if (paceShiftMinutes > 0) "+" else ""
        "$sign$paceShiftMinutes min"
    }
    val shiftPillTextColor = if (paceShiftMinutes > 0) SupporterColors.Pine else SupporterColors.Danger
    val shiftPillBackgroundColor = if (paceShiftMinutes > 0) {
        SupporterColors.Mint
    } else {
        SupporterColors.Danger.copy(alpha = 0.16f)
    }
    val loadedRouteElevationProfile = rememberRouteElevationProfile(projection.estimate.raceId)
    val routeElevationProfile = previewRouteElevationProfile ?: loadedRouteElevationProfile
    val routeMarkers = remember(projection.stations) {
        projection.stations.map { stationProjection ->
            ElevationProfileMarker(
                distanceKm = stationProjection.station.totalKm,
                isCheckedIn = stationProjection.isCheckedIn,
            )
        }
    }
    val completedClimbMeters = projection.stations
        .filter { it.isCheckedIn }
        .sumOf { it.station.climbMeters }
    val totalClimbMeters = projection.stations.sumOf { it.station.climbMeters }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            eyebrow = race.name,
            title = stringResource(Res.string.summary_title),
            subtitle = stringResource(Res.string.summary_subtitle),
            pill = shiftPill,
            pillTextColor = shiftPillTextColor,
            pillBackgroundColor = shiftPillBackgroundColor,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                RunSummaryContent(
                    projection = projection,
                    runnerLocation = runnerLocation,
                )
            }
            if (routeElevationProfile.size >= 2) {
                item {
                    ElevationProfileChart(
                        points = routeElevationProfile,
                        title = stringResource(Res.string.summary_route_profile),
                        markers = routeMarkers,
                        locationMarker = runnerLocation
                            ?.takeIf { it.raceId == projection.estimate.raceId && it.isOnRoute }
                            ?.let {
                                ElevationProfileLocationMarker(
                                    distanceKm = it.distanceKm,
                                    isOnRoute = it.isOnRoute,
                                )
                            },
                        containerColor = SupporterColors.Card,
                        headerValue = "+$completedClimbMeters / +$totalClimbMeters hm",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            runStationListItems(
                projection = projection,
                onStationClick = onStationClick,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.runStationListItems(
    projection: RaceProjection,
    onStationClick: (Int) -> Unit,
) {
    val stations = projection.stations
    val highlightedStationIndex = highlightedStationIndexForList(stations)
    itemsIndexed(stations) { index, station ->
        val previousStation = stations.getOrNull(index - 1)
        val isHighlighted = index == highlightedStationIndex
        val isCurrentVisit = station.isCheckedIn && !station.isCheckedOut
        val isNextAfterCheckout = previousStation?.isCheckedOut == true && !station.isCheckedIn
        VpListRow(
            projection = station,
            isHighlighted = isHighlighted,
            isCurrentVisit = isCurrentVisit,
            isNextAfterCheckout = isNextAfterCheckout,
            timingText = timingTextForRow(station, isHighlighted),
            paceText = paceTextForRow(station, previousStation, projection.actualStartMinutes),
            onClick = { onStationClick(index) },
        )
    }
}

@Composable
private fun timingTextForRow(station: StationProjection, isHighlighted: Boolean): String? {
    station.actualArrival?.let { return stringResource(Res.string.summary_arrived_at, it) }
    if (isHighlighted) {
        return stringResource(Res.string.summary_expected_at, station.window)
    }
    return null
}

internal fun highlightedStationIndexForList(stations: List<StationProjection>): Int? {
    return stations.indexOfFirst { it.isCheckedIn && !it.isCheckedOut }
        .takeIf { it >= 0 }
        ?: stations.indexOfFirst { !it.isCheckedIn }.takeIf { it >= 0 }
}

private fun paceTextForRow(
    station: StationProjection,
    previousStation: StationProjection?,
    actualStartMinutes: Int?,
): String {
    val sectionDuration = ComposeUiUtils.sectionDurationMinutes(station, previousStation, actualStartMinutes)
    return ComposeUiUtils.sectionPace(sectionDuration, station.station.sectionKm)
}

@Preview
@Composable
fun RunSummaryScreenPreview() {
    SupporterTheme {
        RunOverviewScreen(
            projection = RaceCalculator().project(
                estimate = RaceEstimate(),
                checkIns = listOf(
                    CheckIn(1, actualArrivalMinutes = 72, actualDepartureMinutes = 78),
                    CheckIn(2, actualArrivalMinutes = 184),
                ),
                selectedIndex = 1,
            ),
            runnerLocation = LiveRunnerLocation(
                runCode = "PREVIEW",
                raceId = RaceDefinitions.ZugspitzUltratrailId,
                latitude = 47.3844085,
                longitude = 10.9714273,
                distanceKm = 26.0,
                elevationMeters = 1518.0,
                distanceFromRouteMeters = 0.0,
                accuracyMeters = 8.0,
                isOnRoute = true,
                updatedAtEpochMillis = 0L,
            ),
            previewRouteElevationProfile = listOf(
                ElevationSample(0.0, 700.0),
                ElevationSample(5.0, 741.0),
                ElevationSample(12.0, 1220.0),
                ElevationSample(17.0, 1390.0),
                ElevationSample(26.0, 1518.0),
                ElevationSample(34.0, 980.0),
                ElevationSample(48.0, 1180.0),
                ElevationSample(64.0, 720.0),
            ),
            onStationClick = {},
        )
    }
}
