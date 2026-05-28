package de.zugspitz.supporter.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.zugspitz.supporter.components.ActualStartCard
import de.zugspitz.supporter.components.ElevationProfileChart
import de.zugspitz.supporter.components.InfoCard
import de.zugspitz.supporter.components.PagerDots
import de.zugspitz.supporter.components.ScreenHeader
import de.zugspitz.supporter.components.StatTile
import de.zugspitz.supporter.components.VpCard
import de.zugspitz.supporter.data.ElevationSample
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.RaceProjection
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.data.gpxPathForRace
import de.zugspitz.supporter.data.parseElevationProfile
import de.zugspitz.supporter.data.sectionElevationProfile
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterRadius
import de.zugspitz.supporter.theme.SupporterSpacing
import de.zugspitz.supporter.theme.SupporterTheme
import de.zugspitz.supporter.util.ComposeUiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import zugspitz_supporter.composeapp.generated.resources.Res
import zugspitz_supporter.composeapp.generated.resources.arrival_there
import zugspitz_supporter.composeapp.generated.resources.actual_start_title
import zugspitz_supporter.composeapp.generated.resources.distance
import zugspitz_supporter.composeapp.generated.resources.elevation_profile
import zugspitz_supporter.composeapp.generated.resources.duration_hours_range
import zugspitz_supporter.composeapp.generated.resources.elevation
import zugspitz_supporter.composeapp.generated.resources.next_section
import zugspitz_supporter.composeapp.generated.resources.open_in_maps
import zugspitz_supporter.composeapp.generated.resources.pace_time
import zugspitz_supporter.composeapp.generated.resources.planned_duration
import zugspitz_supporter.composeapp.generated.resources.planned_start
import zugspitz_supporter.composeapp.generated.resources.section_to
import zugspitz_supporter.composeapp.generated.resources.vp_of_total

@Composable
fun VpCardScreen(
    projection: RaceProjection,
    selectedIndex: Int,
    canEditCheckIns: Boolean,
    onPageChanged: (Int) -> Unit,
    onActualStartNowClick: () -> Unit,
    onCheckInNowClick: (Int) -> Unit,
    onCheckOutClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val pageCount = projection.stations.size + 1
    val scope = rememberCoroutineScope()
    val hasRaceProgress = projection.actualStartMinutes != null || projection.stations.any { it.isCheckedIn }
    val initialPage = if (hasRaceProgress) {
        (selectedIndex + 1).coerceIn(1, pageCount - 1)
    } else {
        0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount },
    )
    var hasHandledInitialSelection by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, pageCount) {
        if (!hasHandledInitialSelection) {
            hasHandledInitialSelection = true
            return@LaunchedEffect
        }
        val targetPage = selectedIndex + 1
        if (targetPage != pagerState.currentPage && targetPage in 1 until pageCount) {
            pagerState.scrollToPage(targetPage)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page > 0 && page - 1 != selectedIndex) onPageChanged(page - 1)
        }
    }
    val uiPage by remember(pagerState.currentPage, pageCount) {
        derivedStateOf { pagerState.currentPage.coerceIn(0, pageCount - 1) }
    }
    val raceId = projection.estimate.raceId
    val routeElevationProfile by produceState(
        initialValue = elevationProfileCache[raceId].orEmpty(),
        key1 = raceId,
    ) {
        elevationProfileCache[raceId]?.let { cachedProfile ->
            value = cachedProfile
            return@produceState
        }
        val parsedProfile = withContext(Dispatchers.Default) {
            runCatching {
                gpxPathForRace(raceId)
                    ?.let { Res.readBytes(it).decodeToString() }
                    ?.let(::parseElevationProfile)
                    .orEmpty()
            }.getOrDefault(emptyList())
        }
        elevationProfileCache[raceId] = parsedProfile
        value = parsedProfile
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SupporterColors.Paper)
            .verticalScroll(rememberScrollState())
            .padding(top = SupporterSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(SupporterSpacing.Md),
    ) {
        val stationIndex = uiPage - 1
        val activeProjection = projection.stations.getOrNull(stationIndex)
        val nextProjection = if (stationIndex >= 0) {
            projection.stations.getOrNull(stationIndex + 1)
        } else {
            projection.stations.firstOrNull()
        }

        ScreenHeader(
            modifier = Modifier.padding(horizontal = SupporterSpacing.Xl),
            eyebrow = if (uiPage == 0) {
                stringResource(Res.string.planned_start)
            } else {
                stringResource(Res.string.vp_of_total, stationIndex + 1, projection.stations.size)
            },
            title = activeProjection?.station?.name ?: stringResource(Res.string.actual_start_title),
            pill = stringResource(
                Res.string.duration_hours_range,
                projection.estimate.minDurationMinutes / 60,
                projection.estimate.maxDurationMinutes / 60,
            ),
            pillLabel = stringResource(Res.string.planned_duration),
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = SupporterSpacing.Md,
            contentPadding = PaddingValues(start = 28.dp, top = 0.dp, end = 28.dp, bottom = SupporterSpacing.Md),
        ) { page ->
            if (page == 0) {
                ActualStartCard(
                    officialStart = formatRaceTime(projection.estimate.startTimeMinutes),
                    currentStartPreview = rememberCurrentTimeLabel(),
                    actualStart = projection.actualStartMinutes?.let {
                        formatRaceTime(projection.estimate.startTimeMinutes + it)
                    },
                    canEditCheckIns = canEditCheckIns,
                    onActualStartNowClick = {
                        onActualStartNowClick()
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                )
            } else {
                val stationProjection = projection.stations[page - 1]
                VpCard(
                    projection = stationProjection,
                    completedElevation = ComposeUiUtils.completedElevation(projection, page - 1),
                    canEditCheckIns = canEditCheckIns,
                    onCheckInNowClick = { onCheckInNowClick(page - 1) },
                    onCheckOutClick = { onCheckOutClick(page - 1) },
                )
            }
        }

        PagerDots(
            index = uiPage,
            count = pageCount,
            onDotClick = { dotIndex ->
                scope.launch {
                    pagerState.animateScrollToPage(dotIndex)
                }
            },
        )

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
                val routeSectionProfile = remember(routeElevationProfile, activeProjection, nextProjection) {
                    sectionElevationProfile(
                        routeProfile = routeElevationProfile,
                        fromKm = activeProjection?.station?.totalKm ?: 0.0,
                        toKm = nextProjection.station.totalKm,
                    )
                }
                if (routeSectionProfile.size >= 2) {
                    ElevationProfileChart(
                        points = routeSectionProfile,
                        title = stringResource(Res.string.elevation_profile),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth(),
                    )
                }
                StatTile(
                    stringResource(Res.string.pace_time),
                    activeProjection?.let {
                        ComposeUiUtils.likelyPace(it, nextProjection)
                    } ?: ComposeUiUtils.likelyPaceFromStart(nextProjection, projection.actualStartMinutes),
                    null,
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                )
                Button(
                    onClick = { uriHandler.openUri(ComposeUiUtils.mapsUri(nextProjection.station)) },
                    colors = ButtonDefaults.buttonColors(containerColor = SupporterColors.Pine),
                    shape = RoundedCornerShape(SupporterRadius.Card),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(Res.string.open_in_maps),
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberCurrentTimeLabel(): String {
    var currentMinutes by remember { mutableStateOf(currentLocalMinutesOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMinutes = currentLocalMinutesOfDay()
            delay(30_000L)
        }
    }
    return formatRaceTime(currentMinutes)
}

private fun currentLocalMinutesOfDay(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.hour * 60 + now.minute
}

private val elevationProfileCache = mutableMapOf<String, List<ElevationSample>>()

@Preview
@Composable
fun VpCardScreenPreview() {
    SupporterTheme {
        VpCardScreen(
            projection = RaceCalculator().project(RaceEstimate(), emptyList(), 2),
            selectedIndex = 2,
            canEditCheckIns = true,
            onPageChanged = {},
            onActualStartNowClick = {},
            onCheckInNowClick = {},
            onCheckOutClick = {},
        )
    }
}
