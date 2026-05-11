package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.data.RaceEstimate

data class SetupUiState(
    val estimate: RaceEstimate,
    val hasSelectedRace: Boolean,
)
