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
import zugspitz_supporter.composeapp.generated.resources.change_time
import zugspitz_supporter.composeapp.generated.resources.check_in
import zugspitz_supporter.composeapp.generated.resources.check_out
import zugspitz_supporter.composeapp.generated.resources.completed
import zugspitz_supporter.composeapp.generated.resources.expected_arrival
import zugspitz_supporter.composeapp.generated.resources.plan
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
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onChangeTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        stringResource(Res.string.expected_arrival).uppercase(),
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        projection.window,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
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
                        stringResource(Res.string.plan).uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(projection.plannedArrival, color = Color(0xFF8CE075), fontWeight = FontWeight.Black)
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
                    detail = projection.actualDeparture ?: projection.actualArrival,
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

            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onCheckInClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8CE075), contentColor = Color(0xFF0F1B12)),
                ) {
                    Text(stringResource(Res.string.check_in), fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = onCheckOutClick,
                    enabled = projection.isCheckedIn,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE9F1E5), contentColor = Color(0xFF0F1B12)),
                ) {
                    Text(stringResource(Res.string.check_out), fontWeight = FontWeight.Black)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onChangeTimeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f), contentColor = Color.White),
                ) {
                    Text(stringResource(Res.string.change_time), fontWeight = FontWeight.Black)
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
            onCheckInClick = {},
            onCheckOutClick = {},
            onChangeTimeClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
