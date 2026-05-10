package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.DefaultSessionRepository
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRaceSubscription
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
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
import de.zugspitz.supporter.presentation.state.CheckAction
import de.zugspitz.supporter.presentation.state.SettingsUiState
import de.zugspitz.supporter.presentation.state.SetupUiState
import de.zugspitz.supporter.presentation.state.VpUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

class SupporterViewModel(
    sessionRepository: SessionRepository = DefaultSessionRepository(),
    private val liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    private val calculator: RaceCalculator = RaceCalculator(),
    private val currentMinutesOfDay: () -> Int = ::systemMinutesOfDay,
) {
    private val loadSession = LoadSessionUseCase(sessionRepository)
    private val saveSession = SaveSessionUseCase(sessionRepository)
    private val resetSession = ResetSessionUseCase(sessionRepository)
    private val updateEstimate = UpdateEstimateUseCase()
    private val selectVp = SelectVpUseCase()
    private val saveCheckIn = SaveCheckInUseCase()

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var liveSubscription: LiveRaceSubscription? = null
    private var liveSubscriptionCode: String? = null

    init {
        syncLiveSubscription(_uiState.value.settings.liveRunLink)
    }

    fun onTabSelected(tab: AppTab) = updateState { copy(tab = tab) }

    fun onEstimateChange(estimate: RaceEstimate) = updateState {
        copy(setup = setup.copy(estimate = updateEstimate(estimate)))
    }

    fun onCalculateClick() = updateState {
        copy(
            tab = AppTab.Vp,
            vp = vp.copy(selectedIndex = 0),
        )
    }

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

    fun onVpPageChanged(index: Int) = updateState {
        copy(vp = vp.copy(selectedIndex = selectVp(index, vp.projection.stations.lastIndex)))
    }

    fun onCheckInOpen() = updateState {
        val minutes = vp.projection.stations[vp.selectedIndex].actualArrivalMinutes
            ?: vp.projection.stations[vp.selectedIndex].station.plannedArrivalMinutes + 10
        copy(vp = vp.copy(checkSheetOpen = true, checkAction = CheckAction.CheckIn, checkMinutes = minutes))
    }

    fun onCheckOutOpen() = updateState {
        val selected = vp.projection.stations[vp.selectedIndex]
        val minutes = selected.actualDepartureMinutes
            ?: selected.actualArrivalMinutes?.plus(selected.station.stopMinutes)
            ?: selected.station.plannedArrivalMinutes + selected.station.stopMinutes
        copy(vp = vp.copy(checkSheetOpen = true, checkAction = CheckAction.CheckOut, checkMinutes = minutes))
    }

    fun onCheckInTimeDecrease() = updateState { copy(vp = vp.copy(checkMinutes = vp.checkMinutes - 1)) }

    fun onCheckInTimeIncrease() = updateState { copy(vp = vp.copy(checkMinutes = vp.checkMinutes + 1)) }

    fun onCheckInNow() = updateState {
        copy(vp = vp.copy(checkMinutes = currentCheckMinutes()))
    }

    fun onCheckInNowSave(stationIndex: Int = uiState.value.vp.selectedIndex) = updateState {
        val selectedIndex = stationIndex.coerceIn(0, vp.projection.stations.lastIndex)
        val stationSection = vp.projection.stations[selectedIndex].station.section
        val checkMinutes = currentCheckMinutes()
        val newCheckIns = saveCheckIn(
            existingCheckIns = checkIns,
            stationSection = stationSection,
            actualArrivalMinutes = checkMinutes,
        )
        val newEvents = appendLiveEvent(
            type = CheckEventType.CheckIn,
            raceMinutes = checkMinutes,
        )
        copy(
            checkIns = newCheckIns,
            checkEvents = newEvents,
            vp = vp.copy(selectedIndex = selectedIndex, checkSheetOpen = false),
        )
    }

    fun onCheckOutNowSave(stationIndex: Int = uiState.value.vp.selectedIndex) = updateState {
        val selectedIndex = stationIndex.coerceIn(0, vp.projection.stations.lastIndex)
        val selected = vp.projection.stations[selectedIndex]
        if (!selected.isCheckedIn || selected.isCheckedOut) {
            return@updateState this
        }
        val checkMinutes = currentCheckMinutes()
        val newCheckIns = saveCheckIn(
            existingCheckIns = checkIns,
            stationSection = selected.station.section,
            actualDepartureMinutes = checkMinutes,
        )
        val newEvents = appendLiveEvent(
            type = CheckEventType.CheckOut,
            raceMinutes = checkMinutes,
        )
        copy(
            checkIns = newCheckIns,
            checkEvents = newEvents,
            vp = vp.copy(selectedIndex = selectedIndex, checkSheetOpen = false),
        )
    }

    fun onCheckInDismiss() = updateState { copy(vp = vp.copy(checkSheetOpen = false)) }

    fun onCheckInSave() = updateState {
        val stationSection = vp.projection.stations[vp.selectedIndex].station.section
        val newCheckIns = when (vp.checkAction) {
            CheckAction.CheckIn -> saveCheckIn(
                existingCheckIns = checkIns,
                stationSection = stationSection,
                actualArrivalMinutes = vp.checkMinutes,
            )
            CheckAction.CheckOut -> saveCheckIn(
                existingCheckIns = checkIns,
                stationSection = stationSection,
                actualDepartureMinutes = vp.checkMinutes,
            )
        }
        val eventType = when (vp.checkAction) {
            CheckAction.CheckIn -> CheckEventType.CheckIn
            CheckAction.CheckOut -> CheckEventType.CheckOut
        }
        val newEvents = appendLiveEvent(
            type = eventType,
            raceMinutes = vp.checkMinutes,
        )
        copy(
            tab = AppTab.List,
            checkIns = newCheckIns,
            checkEvents = newEvents,
            vp = vp.copy(checkSheetOpen = false),
        )
    }

    fun onCheckOutNow() = updateState {
        val newEvents = appendLiveEvent(
            type = CheckEventType.CheckOut,
            raceMinutes = currentMinutesOfDay() - setup.estimate.startTimeMinutes,
        )
        copy(checkEvents = newEvents)
    }

    fun onLiveRoleSelected(role: LiveRole) {
        updateState {
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(role = role)))
        }
        syncLiveSubscription()
    }

    fun onRunCodeChanged(runCode: String) {
        updateState {
            val normalizedCode = runCode
                .uppercase()
                .filter { it.isLetterOrDigit() }
                .take(MAX_RUN_CODE_LENGTH)
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(runCode = normalizedCode)))
        }
        syncLiveSubscription()
    }

    fun onCreateRunCode() {
        updateState {
            copy(
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(
                        role = LiveRole.Runner,
                        runCode = generateRunCode(),
                        isEnabled = true,
                    ),
                ),
            )
        }
        syncLiveSubscription()
    }

    fun onLiveSharingToggle(enabled: Boolean) {
        updateState {
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(isEnabled = enabled)))
        }
        syncLiveSubscription()
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
            checkEvents = session.checkEvents,
            liveRunLink = session.liveRunLink,
            checkSheetOpen = false,
            checkAction = CheckAction.CheckIn,
            checkMinutesOverride = null,
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
                checkEvents = mutated.checkEvents,
                liveRunLink = mutated.settings.liveRunLink,
                checkSheetOpen = mutated.vp.checkSheetOpen,
                checkAction = mutated.vp.checkAction,
                checkMinutesOverride = mutated.vp.checkMinutes,
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
        checkEvents: List<CheckEvent>,
        liveRunLink: LiveRunLink,
        checkSheetOpen: Boolean,
        checkAction: CheckAction,
        checkMinutesOverride: Int?,
    ): AppUiState {
        val projection = calculator.project(estimate, checkIns, selectedIndex)
        val clampedIndex = selectedIndex.coerceIn(0, projection.stations.lastIndex)
        val selectedStation = projection.stations[clampedIndex]
        val defaultCheckMinutes = when (checkAction) {
            CheckAction.CheckIn -> selectedStation.actualArrivalMinutes ?: (selectedStation.station.plannedArrivalMinutes + 10)
            CheckAction.CheckOut -> selectedStation.actualDepartureMinutes
                ?: selectedStation.actualArrivalMinutes?.plus(selectedStation.station.stopMinutes)
                ?: (selectedStation.station.plannedArrivalMinutes + selectedStation.station.stopMinutes)
        }
        val checkMinutes = checkMinutesOverride ?: defaultCheckMinutes
        return AppUiState(
            tab = tab,
            checkIns = checkIns,
            checkEvents = checkEvents,
            setup = SetupUiState(estimate = estimate),
            vp = VpUiState(
                projection = projection,
                selectedIndex = clampedIndex,
                checkSheetOpen = checkSheetOpen,
                checkAction = checkAction,
                checkMinutes = checkMinutes,
                checkInputTime = formatRaceTime(estimate.startTimeMinutes + checkMinutes),
            ),
            settings = SettingsUiState(
                liveRunLink = liveRunLink,
                lastLiveEventText = checkEvents.lastOrNull()?.toStatusText(estimate.startTimeMinutes),
            ),
        )
    }

    private fun AppUiState.appendLiveEvent(type: CheckEventType, raceMinutes: Int): List<CheckEvent> {
        val link = settings.liveRunLink
        if (!link.canPublish) return checkEvents

        val station = vp.projection.stations[vp.selectedIndex].station
        val createdAtEpochMillis = Clock.System.now().toEpochMilliseconds()
        val event = CheckEvent(
            id = "${link.runCode}-${station.section}-${type.name}-$createdAtEpochMillis",
            runCode = link.runCode,
            stationSection = station.section,
            stationName = station.name,
            type = type,
            raceMinutes = raceMinutes,
            createdAtEpochMillis = createdAtEpochMillis,
        )
        liveRaceRepository.publish(event)
        return checkEvents.filterNot {
            it.stationSection == station.section && it.type == type
        } + event
    }

    private fun persist(state: AppUiState) {
        saveSession(
            AppSessionState(
                estimate = state.setup.estimate,
                tab = state.tab.toSavedTab(),
                selectedIndex = state.vp.selectedIndex,
                checkIns = state.checkIns,
                liveRunLink = state.settings.liveRunLink,
                checkEvents = state.checkEvents,
            ),
        )
    }

    private fun syncLiveSubscription(link: LiveRunLink = _uiState.value.settings.liveRunLink) {
        val requestedCode = link.runCode.takeIf { link.canSubscribe }
        if (requestedCode == liveSubscriptionCode) return

        liveSubscription?.close()
        liveSubscription = null
        liveSubscriptionCode = null

        if (requestedCode == null) return

        liveSubscriptionCode = requestedCode
        liveSubscription = liveRaceRepository.subscribe(requestedCode) { remoteEvents ->
            applyRemoteEvents(requestedCode, remoteEvents)
        }
    }

    private fun applyRemoteEvents(runCode: String, remoteEvents: List<CheckEvent>) {
        updateState {
            if (settings.liveRunLink.runCode != runCode || !settings.liveRunLink.canSubscribe) return@updateState this

            val mergedEvents = (checkEvents + remoteEvents)
                .distinctBy { it.id }
                .sortedBy { it.createdAtEpochMillis }
            val remoteCheckIns = mergedEvents
                .groupBy { it.stationSection }
                .mapNotNull { (_, events) ->
                    val latestCheckIn = events
                        .filter { it.type == CheckEventType.CheckIn }
                        .maxByOrNull { it.createdAtEpochMillis }
                        ?: return@mapNotNull null
                    val latestCheckOut = events
                        .filter { it.type == CheckEventType.CheckOut }
                        .maxByOrNull { it.createdAtEpochMillis }
                    CheckIn(
                        stationSection = latestCheckIn.stationSection,
                        actualArrivalMinutes = latestCheckIn.raceMinutes,
                        actualDepartureMinutes = latestCheckOut?.raceMinutes,
                    )
                }
            copy(
                checkIns = remoteCheckIns,
                checkEvents = mergedEvents,
            )
        }
    }

    private fun CheckEvent.toStatusText(startTimeMinutes: Int): String {
        val action = when (type) {
            CheckEventType.CheckIn -> "Check-in"
            CheckEventType.CheckOut -> "Check-out"
        }
        return "$action ${stationName.removePrefix("Z$stationSection ")} um ${formatRaceTime(startTimeMinutes + raceMinutes)}"
    }

    private fun generateRunCode(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        return now.toString(36).takeLast(MAX_RUN_CODE_LENGTH).uppercase()
    }

    private companion object {
        const val MAX_RUN_CODE_LENGTH = 8
    }

    private fun AppUiState.currentCheckMinutes(): Int {
        val diff = currentMinutesOfDay() - setup.estimate.startTimeMinutes
        return if (diff < 0) diff + 24 * 60 else diff
    }
}

private fun systemMinutesOfDay(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.hour * 60 + now.minute
}
