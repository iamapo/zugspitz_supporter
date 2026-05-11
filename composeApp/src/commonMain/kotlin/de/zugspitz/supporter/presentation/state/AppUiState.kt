package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckIn

data class AppUiState(
    val tab: AppTab,
    val checkIns: List<CheckIn>,
    val checkEvents: List<CheckEvent>,
    val setup: SetupUiState,
    val vp: VpUiState,
    val settings: SettingsUiState,
)
