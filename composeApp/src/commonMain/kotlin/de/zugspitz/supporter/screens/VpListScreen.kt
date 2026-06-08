package de.zugspitz.supporter.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun VpListScreen(
    projection: RaceProjection,
    runnerLocation: LiveRunnerLocation?,
    onStationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    RunOverviewScreen(
        projection = projection,
        runnerLocation = runnerLocation,
        onStationClick = onStationClick,
        modifier = modifier,
    )
}

@Preview
@Composable
fun VpListScreenPreview() {
    SupporterTheme {
        VpListScreen(
            projection = RaceCalculator().project(RaceEstimate(), listOf(CheckIn(3, 281)), 2),
            runnerLocation = null,
            onStationClick = {},
        )
    }
}
