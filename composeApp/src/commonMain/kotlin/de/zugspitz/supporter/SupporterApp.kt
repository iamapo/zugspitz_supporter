package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import de.zugspitz.supporter.components.AppBottomBar
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.presentation.SupporterViewModel
import de.zugspitz.supporter.screens.CheckInSheet
import de.zugspitz.supporter.screens.SettingsScreen
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
        val viewModel = remember { SupporterViewModel() }
        val state by viewModel.uiState.collectAsState()

        Scaffold(
            containerColor = SupporterColors.Paper,
            bottomBar = if (state.tab == AppTab.Setup) {
                {}
            } else {
                {
                    AppBottomBar().Content(
                        selectedTab = state.tab,
                        onTabSelected = viewModel::onTabSelected,
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
                when (state.tab) {
                    AppTab.Setup -> SetupScreen().Content(
                        estimate = state.setup.estimate,
                        onEstimateChange = viewModel::onEstimateChange,
                        onCalculateClick = viewModel::onCalculateClick,
                    )
                    AppTab.Vp -> VpCardScreen().Content(
                        projection = state.vp.projection,
                        selectedIndex = state.vp.selectedIndex,
                        onPrevious = viewModel::onPreviousVp,
                        onNext = viewModel::onNextVp,
                        onCheckInClick = viewModel::onCheckInOpen,
                    )
                    AppTab.List -> VpListScreen().Content(
                        projection = state.vp.projection,
                        onStationClick = viewModel::onStationSelected,
                    )
                    AppTab.Settings -> SettingsScreen().Content(
                        onResetClick = viewModel::onResetAllData,
                    )
                }

                if (state.vp.checkInOpen) {
                    val selectedProjection = state.vp.projection.stations[state.vp.selectedIndex]
                    CheckInSheet().Content(
                        projection = selectedProjection,
                        inputTime = state.vp.checkInInputTime,
                        onDecrease = viewModel::onCheckInTimeDecrease,
                        onIncrease = viewModel::onCheckInTimeIncrease,
                        onSave = viewModel::onCheckInSave,
                        onDismiss = viewModel::onCheckInDismiss,
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
