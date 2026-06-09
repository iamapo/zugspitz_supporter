package de.zugspitz.supporter.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.summary_average_pace
import zugspitz_supporter.composeapp.generated.resources.summary_distance_done
import zugspitz_supporter.composeapp.generated.resources.summary_next_vp
import zugspitz_supporter.composeapp.generated.resources.summary_next_vp_detail
import zugspitz_supporter.composeapp.generated.resources.summary_progress

@Composable
internal fun RunSummaryContent(
    projection: RaceProjection,
    runnerLocation: LiveRunnerLocation?,
) {
    val completedStations = projection.stations.filter { it.isCheckedIn }
    val lastKnown = completedStations.maxByOrNull { it.station.section }
    val lastKnownMinutes = lastKnown?.actualDepartureMinutes ?: lastKnown?.actualArrivalMinutes
    val elapsedMinutes = lastKnownMinutes?.minus(projection.actualStartMinutes ?: 0)
    val liveDistanceKm = runnerLocation
        ?.takeIf { it.raceId == projection.estimate.raceId && it.isOnRoute }
        ?.distanceKm
    val stationDistanceKm = lastKnown?.station?.totalKm ?: 0.0
    val distanceDone = liveDistanceKm ?: stationDistanceKm

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InfoCard(title = stringResource(Res.string.summary_progress)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(
                        label = stringResource(Res.string.summary_distance_done),
                        value = ComposeUiUtils.formattedKm(distanceDone),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(Res.string.summary_average_pace),
                        value = ComposeUiUtils.averagePace(elapsedMinutes, stationDistanceKm),
                        modifier = Modifier.weight(1f),
                    )
                }
                nextVpProgress(projection, runnerLocation)?.let { progress ->
                    StatTile(
                        label = stringResource(Res.string.summary_next_vp),
                        value = ComposeUiUtils.formattedKm(progress.remainingKm),
                        detail = stringResource(Res.string.summary_next_vp_detail, progress.stationName),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private data class NextVpProgress(
    val remainingKm: Double,
    val stationName: String,
)

private fun nextVpProgress(
    projection: RaceProjection,
    runnerLocation: LiveRunnerLocation?,
): NextVpProgress? {
    if (runnerLocation == null || !runnerLocation.isOnRoute || runnerLocation.raceId != projection.estimate.raceId) {
        return null
    }
    val nextStation = projection.stations.firstOrNull { stationProjection ->
        stationProjection.station.totalKm > runnerLocation.distanceKm
    } ?: return null
    return NextVpProgress(
        remainingKm = (nextStation.station.totalKm - runnerLocation.distanceKm).coerceAtLeast(0.0),
        stationName = nextStation.station.name,
    )
}

@Preview
@Composable
fun RunSummaryContentWithLiveLocationPreview() {
    SupporterTheme {
        RunSummaryContent(
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
        )
    }
}

@Preview
@Composable
fun RunSummaryContentWithoutLiveLocationPreview() {
    SupporterTheme {
        RunSummaryContent(
            projection = RaceCalculator().project(
                estimate = RaceEstimate(),
                checkIns = listOf(CheckIn(1, actualArrivalMinutes = 72, actualDepartureMinutes = 78)),
                selectedIndex = 1,
            ),
            runnerLocation = null,
        )
    }
}
