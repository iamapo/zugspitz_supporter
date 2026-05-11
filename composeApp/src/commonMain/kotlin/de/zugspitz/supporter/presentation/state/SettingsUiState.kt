package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.data.LiveRunLink

data class SettingsUiState(
    val canReset: Boolean = true,
    val liveRunLink: LiveRunLink = LiveRunLink(),
    val lastLiveEventText: String? = null,
)
