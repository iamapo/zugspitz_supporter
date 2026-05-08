package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.DefaultSessionRepository
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.SessionRepository
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.data.toAppTab
import de.zugspitz.supporter.data.toSavedTab
import de.zugspitz.supporter.domain.usecase.LoadSessionUseCase
import de.zugspitz.supporter.domain.usecase.ResetSessionUseCase
import de.zugspitz.supporter.domain.usecase.SaveCheckInUseCase
import de.zugspitz.supporter.domain.usecase.SaveSessionUseCase
import de.zugspitz.supporter.domain.usecase.SelectVpUseCase
import de.zugspitz.supporter.domain.usecase.UpdateEstimateUseCase
import de.zugspitz.supporter.presentation.state.AppUiState
import de.zugspitz.supporter.presentation.state.SettingsUiState
import de.zugspitz.supporter.presentation.state.SetupUiState
import de.zugspitz.supporter.presentation.state.VpUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SupporterViewModel(
    sessionRepository: SessionRepository = DefaultSessionRepository(),
    private val calculator: RaceCalculator = RaceCalculator(),
) {
    private val loadSession = LoadSessionUseCase(sessionRepository)
    private val saveSession = SaveSessionUseCase(sessionRepository)
    private val resetSession = ResetSessionUseCase(sessionRepository)
    private val updateEstimate = UpdateEstimateUseCase()
    private val selectVp = SelectVpUseCase()
    private val saveCheckIn = SaveCheckInUseCase()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: AppTab) = updateState { copy(tab = tab) }

    fun onEstimateChange(estimate: RaceEstimate) = updateState {
        copy(setup = setup.copy(estimate = updateEstimate(estimate)))
    }

    fun onCalculateClick() = updateState { copy(tab = AppTab.Vp) }

    fun onPreviousVp() = updateState {
        copy(vp = vp.copy(selectedIndex = selectVp(vp.selectedIndex - 1, vp.projection.stations.lastIndex)))
    }

    fun onNextVp() = updateState {
        copy(vp = vp.copy(selectedIndex = selectVp(vp.selectedIndex + 1, vp.projection.stations.lastIndex)))
    }

    fun onStationSelected(index: Int) = updateState {
        copy(
            tab = AppTab.Vp,
            vp = vp.copy(selectedIndex = selectVp(index, vp.projection.stations.lastIndex)),
        )
    }

    fun onCheckInOpen() = updateState {
        val minutes = vp.projection.stations[vp.selectedIndex].station.plannedArrivalMinutes + 10
        copy(vp = vp.copy(checkInOpen = true, checkInMinutes = minutes))
    }

    fun onCheckInTimeDecrease() = updateState { copy(vp = vp.copy(checkInMinutes = vp.checkInMinutes - 1)) }

    fun onCheckInTimeIncrease() = updateState { copy(vp = vp.copy(checkInMinutes = vp.checkInMinutes + 1)) }

    fun onCheckInDismiss() = updateState { copy(vp = vp.copy(checkInOpen = false)) }

    fun onCheckInSave() = updateState {
        val stationSection = vp.projection.stations[vp.selectedIndex].station.section
        val newCheckIns = saveCheckIn(checkIns, stationSection, vp.checkInMinutes)
        copy(
            tab = AppTab.List,
            checkIns = newCheckIns,
            vp = vp.copy(checkInOpen = false),
        )
    }

    fun onResetAllData() {
        resetSession()
        _uiState.value = createInitialState()
    }

    private fun createInitialState(): AppUiState {
        val session = loadSession()
        return buildUiState(
            tab = session.tab.toAppTab(),
            estimate = session.estimate,
            selectedIndex = session.selectedIndex.coerceAtLeast(0).coerceAtMost(10),
            checkIns = session.checkIns,
            checkInOpen = false,
            checkInMinutesOverride = null,
        )
    }

    private fun updateState(transform: AppUiState.() -> AppUiState) {
        _uiState.update { current ->
            val mutated = current.transform()
            val rebuilt = buildUiState(
                tab = mutated.tab,
                estimate = mutated.setup.estimate,
                selectedIndex = mutated.vp.selectedIndex,
                checkIns = mutated.checkIns,
                checkInOpen = mutated.vp.checkInOpen,
                checkInMinutesOverride = mutated.vp.checkInMinutes,
            )
            persist(rebuilt)
            rebuilt
        }
    }

    private fun buildUiState(
        tab: AppTab,
        estimate: RaceEstimate,
        selectedIndex: Int,
        checkIns: List<CheckIn>,
        checkInOpen: Boolean,
        checkInMinutesOverride: Int?,
    ): AppUiState {
        val projection = calculator.project(estimate, checkIns, selectedIndex)
        val clampedIndex = selectedIndex.coerceIn(0, projection.stations.lastIndex)
        val checkInMinutes = checkInMinutesOverride
            ?: (projection.stations[clampedIndex].station.plannedArrivalMinutes + 10)
        return AppUiState(
            tab = tab,
            checkIns = checkIns,
            setup = SetupUiState(estimate = estimate),
            vp = VpUiState(
                projection = projection,
                selectedIndex = clampedIndex,
                checkInOpen = checkInOpen,
                checkInMinutes = checkInMinutes,
                checkInInputTime = formatRaceTime(estimate.startTimeMinutes + checkInMinutes),
            ),
            settings = SettingsUiState(),
        )
    }

    private fun persist(state: AppUiState) {
        saveSession(
            AppSessionState(
                estimate = state.setup.estimate,
                tab = state.tab.toSavedTab(),
                selectedIndex = state.vp.selectedIndex,
                checkIns = state.checkIns,
            ),
        )
    }
}
