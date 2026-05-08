package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.zugspitz.supporter.components.AppBottomBar
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.screens.CheckInSheet
import de.zugspitz.supporter.screens.SetupScreen
import de.zugspitz.supporter.screens.VpCardScreen
import de.zugspitz.supporter.screens.VpListScreen
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SupporterApp() {
    SupporterTheme {
        SupporterAppRoot().Content()
    }
}

class SupporterAppRoot {
    @Composable
    fun Content() {
        var tab by remember { mutableStateOf(AppTab.Setup) }
        var estimate by remember { mutableStateOf(RaceEstimate()) }
        var selectedIndex by remember { mutableIntStateOf(2) }
        val checkIns = remember { mutableStateListOf<CheckIn>() }
        val calculator = remember { RaceCalculator() }
        val projection = calculator.project(estimate, checkIns, selectedIndex)
        var checkInOpen by remember { mutableStateOf(false) }
        var checkInMinutes by remember {
            mutableIntStateOf(projection.stations[selectedIndex].station.plannedArrivalMinutes + 10)
        }

        Scaffold(
            containerColor = SupporterColors.Paper,
            bottomBar = if (tab == AppTab.Setup) {
                {}
            } else {
                {
                    AppBottomBar().Content(
                        selectedTab = tab,
                        onTabSelected = { tab = it },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SupporterColors.Paper)
                    .padding(padding),
            ) {
                when (tab) {
                    AppTab.Setup -> SetupScreen().Content(
                        estimate = estimate,
                        onEstimateChange = { estimate = it },
                        onCalculateClick = { tab = AppTab.Vp },
                    )
                    AppTab.Vp -> VpCardScreen().Content(
                        projection = projection,
                        selectedIndex = selectedIndex,
                        onPrevious = { selectedIndex = (selectedIndex - 1).coerceAtLeast(0) },
                        onNext = { selectedIndex = (selectedIndex + 1).coerceAtMost(projection.stations.lastIndex) },
                        onCheckInClick = {
                            checkInMinutes = projection.stations[selectedIndex].station.plannedArrivalMinutes + 10
                            checkInOpen = true
                        },
                    )
                    AppTab.List -> VpListScreen().Content(
                        projection = projection,
                        onStationClick = {
                            selectedIndex = it
                            tab = AppTab.Vp
                        },
                    )
                }

                if (checkInOpen) {
                    val selectedProjection = projection.stations[selectedIndex]
                    CheckInSheet().Content(
                        projection = selectedProjection,
                        inputTime = formatRaceTime(estimate.startTimeMinutes + checkInMinutes),
                        onDecrease = { checkInMinutes -= 1 },
                        onIncrease = { checkInMinutes += 1 },
                        onSave = {
                            checkIns.removeAll { it.stationSection == selectedProjection.station.section }
                            checkIns.add(CheckIn(selectedProjection.station.section, checkInMinutes))
                            checkInOpen = false
                            tab = AppTab.List
                        },
                        onDismiss = { checkInOpen = false },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SupporterAppPreview() {
    SupporterApp()
}
