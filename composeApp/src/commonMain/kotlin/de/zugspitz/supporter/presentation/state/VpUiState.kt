package de.zugspitz.supporter.presentation.state

import de.zugspitz.supporter.data.RaceProjection

enum class CheckAction {
    CheckIn,
    CheckOut,
}

data class VpUiState(
    val projection: RaceProjection,
    val selectedIndex: Int,
    val checkSheetOpen: Boolean,
    val checkAction: CheckAction,
    val checkMinutes: Int,
    val checkInputTime: String,
)
