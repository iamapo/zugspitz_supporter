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
        SupporterAppRoot()
    }
}

@Composable
fun SupporterAppRoot() {
    val viewModel = remember { SupporterViewModel() }
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
                    onCheckInNowClick = viewModel::onCheckInNowSave,
                    onCheckOutClick = viewModel::onCheckOutNowSave,
                )
                AppTab.List -> VpListScreen(
                    projection = state.vp.projection,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Settings -> SettingsScreen(
                    onResetClick = viewModel::onResetAllData,
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
