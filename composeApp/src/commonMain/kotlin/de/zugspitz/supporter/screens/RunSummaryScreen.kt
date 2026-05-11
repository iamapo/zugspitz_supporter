package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.SummarySectionRow
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.summary_average_pace
import zugspitz_supporter.composeapp.generated.resources.summary_distance_done
import zugspitz_supporter.composeapp.generated.resources.summary_last_known
import zugspitz_supporter.composeapp.generated.resources.summary_no_checkins
import zugspitz_supporter.composeapp.generated.resources.summary_progress
import zugspitz_supporter.composeapp.generated.resources.summary_sections
import zugspitz_supporter.composeapp.generated.resources.summary_started
import zugspitz_supporter.composeapp.generated.resources.summary_subtitle
import zugspitz_supporter.composeapp.generated.resources.summary_title

@Composable
fun RunSummaryScreen(
    projection: RaceProjection,
    onStationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val race = RaceDefinitions.byId(projection.estimate.raceId)
    val completedStations = projection.stations.filter { it.isCheckedIn }
    val lastKnown = completedStations.maxByOrNull { it.station.section }
    val lastKnownMinutes = lastKnown?.actualDepartureMinutes ?: lastKnown?.actualArrivalMinutes
    val distanceDone = lastKnown?.station?.totalKm ?: 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            eyebrow = race.name,
            title = stringResource(Res.string.summary_title),
            subtitle = stringResource(Res.string.summary_subtitle),
            pill = if (projection.activeShiftMinutes == 0) null else "+${projection.activeShiftMinutes} min",
        )

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

        InfoCard(title = stringResource(Res.string.summary_sections)) {
            if (completedStations.isEmpty()) {
                Text(
                    text = stringResource(Res.string.summary_no_checkins),
                    color = SupporterColors.Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    completedStations.forEachIndexed { index, station ->
                        val previousStation = completedStations.getOrNull(index - 1)
                        SummarySectionRow(
                            fromName = if (index == 0) {
                                stringResource(Res.string.summary_started)
                            } else {
                                completedStations[index - 1].station.name
                            },
                            projection = station,
                            startTimeMinutes = projection.estimate.startTimeMinutes,
                            sectionDurationMinutes = ComposeUiUtils.sectionDurationMinutes(station, previousStation),
                            onClick = {
                                val stationIndex = projection.stations.indexOfFirst {
                                    it.station.section == station.station.section
                                }
                                if (stationIndex >= 0) onStationClick(stationIndex)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun RunSummaryScreenPreview() {
    SupporterTheme {
        RunSummaryScreen(
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
