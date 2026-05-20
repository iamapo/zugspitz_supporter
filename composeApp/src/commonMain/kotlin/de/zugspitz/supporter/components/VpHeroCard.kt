package de.zugspitz.supporter.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.StationProjection
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.check_in_now
import zugspitz_supporter.composeapp.generated.resources.check_out
import zugspitz_supporter.composeapp.generated.resources.checked_in
import zugspitz_supporter.composeapp.generated.resources.completed
import zugspitz_supporter.composeapp.generated.resources.expected_arrival
import zugspitz_supporter.composeapp.generated.resources.planned_check_in
import zugspitz_supporter.composeapp.generated.resources.planned_check_out
import zugspitz_supporter.composeapp.generated.resources.stop_time
import zugspitz_supporter.composeapp.generated.resources.to_here
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VpCard(
    projection: StationProjection,
    completedElevation: Pair<Int, Int>,
    canEditCheckIns: Boolean,
    onCheckInNowClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPlanningCheckout = projection.isCheckedIn && !projection.isCheckedOut
    val planLabel = if (isPlanningCheckout) {
        stringResource(Res.string.planned_check_out)
    } else {
        stringResource(Res.string.planned_check_in)
    }
    val planTime = if (isPlanningCheckout) projection.plannedDeparture else projection.plannedArrival

    Surface(
        color = SupporterColors.Pine,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (projection.isCheckedIn) {
                            stringResource(Res.string.checked_in).uppercase()
                        } else {
                            stringResource(Res.string.expected_arrival).uppercase()
                        },
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        projection.actualArrival ?: projection.window,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF7FD36B).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF7FD36B).copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                ) {
                    Text(
                        planLabel.uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(planTime, color = Color(0xFF8CE075), fontWeight = FontWeight.Black)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(Res.string.completed),
                    value = "${projection.station.totalKm} km",
                    detail = "+${completedElevation.first} / -${completedElevation.second} hm",
                    modifier = Modifier.weight(1f),
                    inverted = true,
                )
                StatTile(
                    label = stringResource(Res.string.stop_time),
                    value = projection.actualStopMinutes?.let { "${it} min" } ?: "${projection.station.stopMinutes} min",
                    detail = projection.actualDeparture ?: projection.plannedDeparture,
                    modifier = Modifier.weight(1f),
                    inverted = true,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = stringResource(Res.string.to_here),
                    value = "${projection.station.sectionKm} km",
                    detail = "+${projection.station.climbMeters} / -${projection.station.descentMeters} hm",
                    modifier = Modifier.fillMaxWidth(),
                    inverted = true,
                )
            }

            if (canEditCheckIns) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (projection.isCheckedIn && !projection.isCheckedOut) {
                        Button(
                            onClick = onCheckOutClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE9F1E5), contentColor = Color(0xFF0F1B12)),
                        ) {
                            Text(stringResource(Res.string.check_out), fontWeight = FontWeight.Black)
                        }
                    } else if (!projection.isCheckedIn) {
                        Button(
                            onClick = onCheckInNowClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8CE075), contentColor = Color(0xFF0F1B12)),
                        ) {
                            Text(stringResource(Res.string.check_in_now), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun VpCardPreview() {
    val projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2).stations[2]
    SupporterTheme {
        VpCard(
            projection = projection,
            completedElevation = 1750 to 834,
            canEditCheckIns = true,
            onCheckInNowClick = {},
            onCheckOutClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
