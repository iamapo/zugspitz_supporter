package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.VpCard
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.data.StationProjection
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.arrival_there
import zugspitz_supporter.composeapp.generated.resources.distance
import zugspitz_supporter.composeapp.generated.resources.duration_hours_range
import zugspitz_supporter.composeapp.generated.resources.elevation
import zugspitz_supporter.composeapp.generated.resources.next_section
import zugspitz_supporter.composeapp.generated.resources.pace_time
import zugspitz_supporter.composeapp.generated.resources.section_to
import zugspitz_supporter.composeapp.generated.resources.vp_of_total

@Composable
fun VpCardScreen(
    projection: RaceProjection,
    selectedIndex: Int,
    onPageChanged: (Int) -> Unit,
    onCheckInNowClick: (Int) -> Unit,
    onCheckOutClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, projection.stations.lastIndex),
        pageCount = { projection.stations.size },
    )
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != pagerState.currentPage && selectedIndex in 0 until projection.stations.size) {
            pagerState.scrollToPage(selectedIndex)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != selectedIndex) onPageChanged(page)
        }
    }
    val uiPage by remember(pagerState.currentPage, projection.stations.size) {
        derivedStateOf { pagerState.currentPage.coerceIn(0, projection.stations.lastIndex) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(top = SupporterSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
    ) {
        val activeProjection = projection.stations[uiPage]
        val nextProjection = projection.stations.getOrNull(uiPage + 1)

        ScreenHeader(
            modifier = Modifier.padding(horizontal = SupporterSpacing.Xl),
            eyebrow = stringResource(Res.string.vp_of_total, uiPage + 1, projection.stations.size),
            title = activeProjection.station.name,
            pill = stringResource(
                Res.string.duration_hours_range,
                projection.estimate.minDurationMinutes / 60,
                projection.estimate.maxDurationMinutes / 60,
            ),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = SupporterSpacing.Md,
            contentPadding = PaddingValues(horizontal = 28.dp),
        ) { page ->
            val stationProjection = projection.stations[page]
            VpCard(
                projection = stationProjection,
                completedElevation = completedElevation(projection, page),
                onCheckInNowClick = { onCheckInNowClick(page) },
                onCheckOutClick = { onCheckOutClick(page) },
            )
        }

        SwipeDots(index = uiPage, count = projection.stations.size, pagerState = pagerState)

        if (nextProjection != null) {
            InfoCard(
                title = stringResource(Res.string.next_section),
                modifier = Modifier.padding(horizontal = SupporterSpacing.Sm),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(Res.string.section_to), nextProjection.station.name.removePrefix("Z${nextProjection.station.section} "), nextProjection.station.name, Modifier.weight(1f))
                    StatTile(stringResource(Res.string.arrival_there), nextProjection.window, null, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    StatTile(stringResource(Res.string.distance), "${nextProjection.station.sectionKm} km", null, Modifier.weight(1f))
                    StatTile(stringResource(Res.string.elevation), "+${nextProjection.station.climbMeters} / -${nextProjection.station.descentMeters}", null, Modifier.weight(1f))
                }
                StatTile(
                    stringResource(Res.string.pace_time),
                    likelyPace(activeProjection, nextProjection),
                    null,
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SwipeDots(index: Int, count: Int, pagerState: PagerState) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { dotIndex ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clickable {
                        scope.launch {
                            pagerState.animateScrollToPage(dotIndex)
                        }
                    }
                    .background(
                        if (dotIndex == index) SupporterColors.Moss else androidx.compose.ui.graphics.Color(0xFFCBD4CB),
                        RoundedCornerShape(SupporterRadius.Pill),
                    )
                    .padding(horizontal = if (dotIndex == index) 11.dp else 4.dp, vertical = 4.dp),
            )
        }
    }
}

private fun completedElevation(projection: RaceProjection, selectedIndex: Int): Pair<Int, Int> {
    val stations = projection.stations.take(selectedIndex + 1).map { it.station }
    return stations.sumOf { it.climbMeters } to stations.sumOf { it.descentMeters }
}

private fun likelyPace(from: StationProjection, to: StationProjection): String {
    val startMinutes = from.actualArrivalMinutes ?: from.projectedArrivalMinutes
    val segmentMinutes = (to.projectedArrivalMinutes - startMinutes).coerceAtLeast(1)
    val paceMinutesPerKm = segmentMinutes / to.station.sectionKm
    val totalSeconds = (paceMinutesPerKm * 60).toInt().coerceAtLeast(0)
    val paceMinutes = totalSeconds / 60
    val paceSeconds = totalSeconds % 60
    return "${paceMinutes}:${paceSeconds.toString().padStart(2, '0')}/km"
}

@Preview
@Composable
fun VpCardScreenPreview() {
    SupporterTheme {
        VpCardScreen(
            projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2),
            selectedIndex = 2,
            onPageChanged = {},
            onCheckInNowClick = {},
            onCheckOutClick = {},
        )
    }
}
