package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
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
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.NoOpLiveRaceRepository

@Composable
fun SupporterApp(liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository()) {
    SupporterTheme {
        SupporterAppRoot(liveRaceRepository = liveRaceRepository)
    }
}

@Composable
fun SupporterAppRoot(liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository()) {
    val viewModel = remember(liveRaceRepository) {
        SupporterViewModel(liveRaceRepository = liveRaceRepository)
    }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = SupporterColors.Paper,
        bottomBar = if (state.tab == AppTab.Setup) {
            {}
        } else {
            {
                AppBottomBar(
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
                AppTab.Setup -> SetupScreen(
                    estimate = state.setup.estimate,
                    onEstimateChange = viewModel::onEstimateChange,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.Vp -> VpCardScreen(
                    projection = state.vp.projection,
                    selectedIndex = state.vp.selectedIndex,
                    onPageChanged = viewModel::onVpPageChanged,
                    onCheckInClick = viewModel::onCheckInOpen,
                    onCheckInNowClick = viewModel::onCheckInNowSave,
                    onCheckOutClick = viewModel::onCheckOutOpen,
                )
                AppTab.List -> VpListScreen(
                    projection = state.vp.projection,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Settings -> SettingsScreen(
                    state = state.settings,
                    onRoleSelected = viewModel::onLiveRoleSelected,
                    onRunCodeChanged = viewModel::onRunCodeChanged,
                    onCreateRunCode = viewModel::onCreateRunCode,
                    onLiveSharingToggle = viewModel::onLiveSharingToggle,
                    onResetClick = viewModel::onResetAllData,
                )
            }

            if (state.vp.checkSheetOpen) {
                val selectedProjection = state.vp.projection.stations[state.vp.selectedIndex]
                CheckInSheet(
                    projection = selectedProjection,
                    action = state.vp.checkAction,
                    inputTime = state.vp.checkInputTime,
                    onNow = viewModel::onCheckInNow,
                    onDecrease = viewModel::onCheckInTimeDecrease,
                    onIncrease = viewModel::onCheckInTimeIncrease,
                    onSave = viewModel::onCheckInSave,
                    onDismiss = viewModel::onCheckInDismiss,
                )
            }
        }
    }
}

@Preview
@Composable
fun SupporterAppPreview() {
    SupporterApp()
}
