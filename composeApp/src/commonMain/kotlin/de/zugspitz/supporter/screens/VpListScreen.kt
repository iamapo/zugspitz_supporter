package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.VpListRow
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.all_vps
import zugspitz_supporter.composeapp.generated.resources.all_vps_subtitle
import zugspitz_supporter.composeapp.generated.resources.race_name
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview

class VpListScreen {
    @Composable
    fun Content(
        projection: RaceProjection,
        onStationClick: (Int) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SupporterColors.Paper)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader().Content(
                eyebrow = stringResource(Res.string.race_name),
                title = stringResource(Res.string.all_vps),
                subtitle = stringResource(Res.string.all_vps_subtitle),
                pill = if (projection.activeShiftMinutes == 0) null else "+${projection.activeShiftMinutes} min",
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                itemsIndexed(projection.stations) { index, station ->
                    VpListRow().Content(
                        projection = station,
                        onClick = { onStationClick(index) },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun VpListScreenPreview() {
    SupporterTheme {
        VpListScreen().Content(
            projection = RaceCalculator().project(RaceEstimate(), listOf(CheckIn(3, 281)), 2),
            onStationClick = {},
        )
    }
}
