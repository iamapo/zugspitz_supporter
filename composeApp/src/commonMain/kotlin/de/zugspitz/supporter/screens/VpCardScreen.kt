package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.VpHeroCard
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.next_section
import zugspitz_supporter.composeapp.generated.resources.support_info
import zugspitz_supporter.composeapp.generated.resources.support_info_text
import zugspitz_supporter.composeapp.generated.resources.swipe_hint
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

class VpCardScreen {
    @Composable
    fun Content(
        projection: RaceProjection,
        selectedIndex: Int,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onCheckInClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        val stationProjection = projection.stations[selectedIndex]
        val nextProjection = projection.stations.getOrNull(selectedIndex + 1)
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenHeader().Content(
                eyebrow = "VP ${selectedIndex + 1} von ${projection.stations.size}",
                title = stationProjection.station.name,
                subtitle = stringResource(Res.string.swipe_hint),
                pill = "${projection.estimate.minDurationMinutes / 60}-${projection.estimate.maxDurationMinutes / 60} h",
            )

            VpHeroCard().Content(
                projection = stationProjection,
                completedElevation = completedElevation(projection, selectedIndex),
                onCheckInClick = onCheckInClick,
                onChangeTimeClick = onCheckInClick,
            )

            SwipeDots(selectedIndex, projection.stations.size, onPrevious, onNext)

            if (nextProjection != null) {
                InfoCard().Content(title = stringResource(Res.string.next_section)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile().Content("Nach", nextProjection.station.name.removePrefix("Z${nextProjection.station.section} "), nextProjection.station.name, Modifier.weight(1f))
                        StatTile().Content("Ankunft dort", nextProjection.window, null, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        StatTile().Content("Distanz", "${nextProjection.station.sectionKm} km", null, Modifier.weight(1f))
                        StatTile().Content("Höhenmeter", "+${nextProjection.station.climbMeters} / -${nextProjection.station.descentMeters}", null, Modifier.weight(1f))
                    }
                }
            }

            InfoCard().Content(title = stringResource(Res.string.support_info)) {
                Text(stringResource(Res.string.support_info_text), color = SupporterColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun SwipeDots(index: Int, count: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onPrevious, color = androidx.compose.ui.graphics.Color.Transparent) {
                Text("‹", color = SupporterColors.Moss, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            repeat(count) { dotIndex ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .background(
                            if (dotIndex == index) SupporterColors.Moss else androidx.compose.ui.graphics.Color(0xFFCBD4CB),
                            RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = if (dotIndex == index) 11.dp else 4.dp, vertical = 4.dp),
                )
            }
            Surface(onClick = onNext, color = androidx.compose.ui.graphics.Color.Transparent) {
                Text("›", color = SupporterColors.Moss, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }

    private fun completedElevation(projection: RaceProjection, selectedIndex: Int): Pair<Int, Int> {
        val stations = projection.stations.take(selectedIndex + 1).map { it.station }
        return stations.sumOf { it.climbMeters } to stations.sumOf { it.descentMeters }
    }
}

@Preview
@Composable
fun VpCardScreenPreview() {
    SupporterTheme {
        VpCardScreen().Content(
            projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2),
            selectedIndex = 2,
            onPrevious = {},
            onNext = {},
            onCheckInClick = {},
        )
    }
}
