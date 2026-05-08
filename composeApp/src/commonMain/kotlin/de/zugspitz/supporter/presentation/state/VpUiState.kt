package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.data.RaceProjection

data class VpUiState(
    val projection: RaceProjection,
    val selectedIndex: Int,
    val checkInOpen: Boolean,
    val checkInMinutes: Int,
    val checkInInputTime: String,
)
