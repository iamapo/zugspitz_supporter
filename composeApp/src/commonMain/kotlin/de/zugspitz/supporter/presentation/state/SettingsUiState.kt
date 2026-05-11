package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.LiveRunLink

data class SettingsUiState(
    val canReset: Boolean = true,
    val liveRunLink: LiveRunLink = LiveRunLink(),
    val lastLiveEvent: LastLiveEventUiState? = null,
)

data class LastLiveEventUiState(
    val type: CheckEventType,
    val stationName: String,
    val raceTime: String,
)
