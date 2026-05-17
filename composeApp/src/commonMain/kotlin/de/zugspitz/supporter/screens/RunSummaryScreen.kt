package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.VpListRow
import de.zugspitz.supporter.data.CheckIn
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
import zugspitz_supporter.composeapp.generated.resources.summary_average_pace
import zugspitz_supporter.composeapp.generated.resources.summary_arrived_at
import zugspitz_supporter.composeapp.generated.resources.summary_distance_done
import zugspitz_supporter.composeapp.generated.resources.summary_expected_at
import zugspitz_supporter.composeapp.generated.resources.summary_last_known
import zugspitz_supporter.composeapp.generated.resources.summary_progress
import zugspitz_supporter.composeapp.generated.resources.summary_started
import zugspitz_supporter.composeapp.generated.resources.summary_subtitle
import zugspitz_supporter.composeapp.generated.resources.summary_title

@Composable
fun RunOverviewScreen(
    projection: RaceProjection,
    onStationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
                )
            }
            runStationListItems(
                projection = projection,
                onStationClick = onStationClick,
            )
        }
    }
}

@Composable
private fun RunSummaryContent(
    projection: RaceProjection,
) {
    val completedStations = projection.stations.filter { it.isCheckedIn }
    val lastKnown = completedStations.maxByOrNull { it.station.section }
    val lastKnownMinutes = lastKnown?.actualDepartureMinutes ?: lastKnown?.actualArrivalMinutes
    val distanceDone = lastKnown?.station?.totalKm ?: 0.0

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InfoCard(title = stringResource(Res.string.summary_progress)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile(
                    label = stringResource(Res.string.summary_distance_done),
                    value = ComposeUiUtils.formattedKm(distanceDone),
                    detail = lastKnown?.station?.name ?: stringResource(Res.string.summary_started),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(Res.string.summary_average_pace),
                    value = ComposeUiUtils.averagePace(lastKnownMinutes, distanceDone),
                    detail = lastKnownMinutes?.let { formatRaceTime(projection.estimate.startTimeMinutes + it) }
                        ?: stringResource(Res.string.summary_last_known),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.runStationListItems(
    projection: RaceProjection,
    onStationClick: (Int) -> Unit,
) {
    val stations = projection.stations
    itemsIndexed(stations) { index, station ->
        val previousStation = stations.getOrNull(index - 1)
        VpListRow(
            projection = station,
            timingText = timingTextForRow(station),
            paceText = paceTextForRow(station, previousStation, projection.actualStartMinutes),
            onClick = { onStationClick(index) },
        )
    }
}

@Composable
private fun timingTextForRow(station: StationProjection): String? {
    station.actualArrival?.let { return stringResource(Res.string.summary_arrived_at, it) }
    if (station.isCurrent) {
        return stringResource(Res.string.summary_expected_at, station.window)
    }
    return null
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
            onStationClick = {},
        )
    }
}
