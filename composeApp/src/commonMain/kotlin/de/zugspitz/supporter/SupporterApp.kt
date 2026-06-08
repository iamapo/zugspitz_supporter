package de.zugspitz.supporter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import de.zugspitz.supporter.components.AppBottomBar
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.presentation.SupporterViewModel
import de.zugspitz.supporter.screens.RaceSelectionScreen
import de.zugspitz.supporter.screens.RoleSelectionScreen
import de.zugspitz.supporter.screens.OfflineMapScreen
import de.zugspitz.supporter.screens.RunOverviewScreen
import de.zugspitz.supporter.screens.SettingsScreen
import de.zugspitz.supporter.screens.SetupScreen
import de.zugspitz.supporter.screens.SupportCodeScreen
import de.zugspitz.supporter.screens.VpCardScreen
import de.zugspitz.supporter.screens.VpListScreen
import de.zugspitz.supporter.theme.SupporterColors
import de.zugspitz.supporter.theme.SupporterTheme
import androidx.compose.ui.tooling.preview.Preview
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.screens.RouteLocationUiState
import de.zugspitz.supporter.screens.rememberRouteLocationTrackingState
import kotlinx.coroutines.delay

@Composable
fun SupporterApp(
    liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    liveSharingEnabled: Boolean = AppFeatureFlags.LiveSharingEnabled,
    supporterPushNotifications: SupporterPushNotifications = NoOpSupporterPushNotifications,
) {
    SupporterTheme {
        SupporterAppRoot(
            liveRaceRepository = liveRaceRepository,
            liveSharingEnabled = liveSharingEnabled,
            supporterPushNotifications = supporterPushNotifications,
        )
    }
}

@Composable
fun SupporterAppRoot(
    liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    liveSharingEnabled: Boolean = AppFeatureFlags.LiveSharingEnabled,
    supporterPushNotifications: SupporterPushNotifications = NoOpSupporterPushNotifications,
) {
    val viewModel = remember(liveRaceRepository, liveSharingEnabled, supporterPushNotifications) {
        SupporterViewModel(
            liveRaceRepository = liveRaceRepository,
            liveSharingEnabled = liveSharingEnabled,
            supporterPushNotifications = supporterPushNotifications,
        )
    }
    val runnerLocationProvider = rememberRunnerLocationProvider()
    val state by viewModel.uiState.collectAsState()
    val liveRunLink = state.settings.liveRunLink
    val localRouteLocation by rememberRouteLocationTrackingState(
        raceId = state.setup.estimate.raceId,
        locationProvider = runnerLocationProvider,
        enabled = liveRunLink.canPublish,
    )
    val localLiveRunnerLocation = localRouteLocation?.toLiveRunnerLocation(
        runCode = liveRunLink.runCode,
        raceId = state.setup.estimate.raceId,
    )
    LaunchedEffect(localLiveRunnerLocation, liveRunLink.canPublish) {
        if (liveRunLink.canPublish) {
            localLiveRunnerLocation?.let(viewModel::onRunnerLocationChanged)
        }
    }
    val visibleRunnerLocation = when {
        liveRunLink.canSubscribe -> state.runnerLocation
        liveRunLink.canPublish -> localLiveRunnerLocation
        else -> null
    }
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(state.offlineMap.isDownloading, state.offlineMap.progressPercent) {
        if (!state.offlineMap.isDownloading) return@LaunchedEffect
        delay(280)
        viewModel.onOfflineMapDownloadProgress((state.offlineMap.progressPercent + 7).coerceAtMost(100))
    }

    Scaffold(
        containerColor = SupporterColors.Paper,
        bottomBar = if (
            isKeyboardVisible ||
            state.tab == AppTab.Role ||
            state.tab == AppTab.SupportCode ||
            state.tab == AppTab.Race ||
            state.tab == AppTab.Setup
        ) {
            {}
        } else {
            {
                AppBottomBar(
                    selectedTab = if (state.tab == AppTab.Summary) AppTab.List else state.tab,
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
                AppTab.Role -> RoleSelectionScreen(
                    liveSharingEnabled = liveSharingEnabled,
                    onRunnerSelected = viewModel::onRunnerModeSelected,
                    onSupporterSelected = viewModel::onSupporterModeSelected,
                )
                AppTab.SupportCode -> SupportCodeScreen(
                    liveRunLink = state.settings.liveRunLink,
                    onRunCodeChanged = viewModel::onRunCodeChanged,
                    onConnectClick = viewModel::onSupportCodeConnect,
                    onContinueWithoutCodeClick = viewModel::onContinueWithoutSupportCode,
                )
                AppTab.Race -> RaceSelectionScreen(
                    selectedRaceId = if (state.setup.hasSelectedRace) state.setup.estimate.raceId else "",
                    onRaceSelected = viewModel::onRaceSelected,
                )
                AppTab.Setup -> SetupScreen(
                    estimate = state.setup.estimate,
                    pauseMinutesBySection = state.setup.pauseMinutesBySection,
                    onEstimateChange = viewModel::onEstimateChange,
                    onPauseMinutesChange = viewModel::onPauseMinutesChanged,
                    onBackClick = viewModel::onSetupBack,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.PauseSetup -> SetupScreen(
                    estimate = state.setup.estimate,
                    pauseMinutesBySection = state.setup.pauseMinutesBySection,
                    onEstimateChange = viewModel::onEstimateChange,
                    onPauseMinutesChange = viewModel::onPauseMinutesChanged,
                    onBackClick = viewModel::onSetupBack,
                    onCalculateClick = viewModel::onCalculateClick,
                )
                AppTab.Vp -> VpCardScreen(
                    projection = state.vp.projection,
                    selectedIndex = state.vp.selectedIndex,
                    canEditCheckIns = !state.settings.liveRunLink.canSubscribe,
                    onPageChanged = viewModel::onVpPageChanged,
                    onActualStartNowClick = viewModel::onActualStartNowSave,
                    onCheckInNowClick = viewModel::onCheckInNowSave,
                    onCheckOutClick = viewModel::onCheckOutNowSave,
                )
                AppTab.Summary -> RunOverviewScreen(
                    projection = state.vp.projection,
                    runnerLocation = visibleRunnerLocation,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Map -> OfflineMapScreen(
                    projection = state.vp.projection,
                    offlineMap = state.offlineMap,
                    onDownloadStart = viewModel::onOfflineMapDownloadStart,
                    modifier = Modifier.fillMaxSize(),
                )
                AppTab.List -> VpListScreen(
                    projection = state.vp.projection,
                    runnerLocation = visibleRunnerLocation,
                    onStationClick = viewModel::onStationSelected,
                )
                AppTab.Settings -> SettingsScreen(
                    state = state.settings,
                    liveSharingEnabled = liveSharingEnabled,
                    onRoleSelected = viewModel::onLiveRoleSelected,
                    onRunCodeChanged = viewModel::onRunCodeChanged,
                    onRunnerNameChanged = viewModel::onRunnerNameChanged,
                    onCreateRunCode = viewModel::onCreateRunCode,
                    onLiveSharingToggle = viewModel::onLiveSharingToggle,
                    onResetClick = viewModel::onResetAllData,
                    isKeyboardVisible = isKeyboardVisible,
                )
            }

        }
    }
}

private fun RouteLocationUiState.toLiveRunnerLocation(runCode: String, raceId: String) = LiveRunnerLocation(
    runCode = runCode,
    raceId = raceId,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    elevationMeters = elevationMeters,
    distanceFromRouteMeters = distanceFromRouteMeters,
    accuracyMeters = accuracyMeters,
    isOnRoute = isOnRoute,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

@Preview
@Composable
fun SupporterAppPreview() {
    SupporterApp()
}
