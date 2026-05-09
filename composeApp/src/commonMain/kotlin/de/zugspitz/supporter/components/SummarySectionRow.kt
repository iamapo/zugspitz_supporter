package de.zugspitz.supporter.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.StationProjection
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.summary_checkin_label
import zugspitz_supporter.composeapp.generated.resources.summary_checkout_label

@Composable
fun SummarySectionRow(
    fromName: String,
    projection: StationProjection,
    startTimeMinutes: Int,
    sectionDurationMinutes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = SupporterColors.Paper,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SupporterColors.Line, RoundedCornerShape(8.dp)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = fromName,
                        color = SupporterColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = projection.station.name,
                        color = SupporterColors.Pine,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = ComposeUiUtils.sectionPace(sectionDurationMinutes, projection.station.sectionKm),
                    color = SupporterColors.Ink,
                    fontWeight = FontWeight.Black,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SummaryTimeTile(
                    label = stringResource(Res.string.summary_checkin_label),
                    value = projection.actualArrivalMinutes?.let { formatRaceTime(startTimeMinutes + it) } ?: "-",
                    modifier = Modifier.weight(1f),
                )
                SummaryTimeTile(
                    label = stringResource(Res.string.summary_checkout_label),
                    value = projection.actualDepartureMinutes?.let { formatRaceTime(startTimeMinutes + it) } ?: "-",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
fun SummarySectionRowPreview() {
    val projection = RaceCalculator()
        .project(
            estimate = RaceEstimate(),
            checkIns = listOf(CheckIn(1, actualArrivalMinutes = 72, actualDepartureMinutes = 78)),
            selectedIndex = 0,
        )
        .stations
        .first()
    SupporterTheme {
        SummarySectionRow(
            fromName = "Start",
            projection = projection,
            startTimeMinutes = RaceEstimate().startTimeMinutes,
            sectionDurationMinutes = 72,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
