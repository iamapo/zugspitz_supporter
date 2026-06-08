package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunnerLocation

data class AppUiState(
    val tab: AppTab,
    val checkIns: List<CheckIn>,
    val checkEvents: List<CheckEvent>,
    val setup: SetupUiState,
    val vp: VpUiState,
    val offlineMap: OfflineMapUiState,
    val settings: SettingsUiState,
    val runnerLocation: LiveRunnerLocation? = null,
)
