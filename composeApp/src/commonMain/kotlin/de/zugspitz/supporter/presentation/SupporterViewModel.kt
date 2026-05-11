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
import de.zugspitz.supporter.data.LiveRunInfo
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
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
    private val liveSharingEnabled: Boolean = false,
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

    fun onRunnerModeSelected() {
        updateState {
            copy(
                tab = AppTab.Race,
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(role = LiveRole.Runner, isEnabled = false),
                ),
            )
        }
        syncLiveSubscription()
    }

    fun onSupporterModeSelected() {
        if (!liveSharingEnabled) {
            onRunnerModeSelected()
            return
        }
        updateState {
            copy(
                tab = AppTab.SupportCode,
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(role = LiveRole.Supporter, isEnabled = false),
                ),
            )
        }
        syncLiveSubscription()
    }

    fun onSupportCodeConnect() {
        if (!liveSharingEnabled) return
        updateState {
            if (settings.liveRunLink.runCode.isBlank()) {
                return@updateState this
            }
            copy(
                tab = AppTab.SupportCode,
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(role = LiveRole.Supporter, isEnabled = true),
                ),
            )
        }
        syncLiveSubscription()
    }

    fun onContinueWithoutSupportCode() {
        updateState {
            copy(
                tab = AppTab.Race,
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(role = LiveRole.Supporter, isEnabled = false),
                ),
            )
        }
        syncLiveSubscription()
    }

    fun onEstimateChange(estimate: RaceEstimate) {
        updateState {
            copy(setup = setup.copy(estimate = updateEstimate(estimate)))
        }
        publishCurrentRunInfo()
    }

    fun onRaceSelected(raceId: String) {
        updateState {
            val selectedRace = RaceDefinitions.byId(raceId)
            copy(
                tab = AppTab.Setup,
                checkIns = emptyList(),
                setup = setup.copy(
                    estimate = selectedRace.defaultEstimate(),
                    hasSelectedRace = true,
                    pauseMinutesBySection = defaultPauseMinutesBySection(raceId),
                ),
                vp = vp.copy(selectedIndex = 0),
            )
        }
        publishCurrentRunInfo()
    }

    fun onCalculateClick() = updateState {
        copy(
            tab = AppTab.PauseSetup,
        )
    }

    fun onPauseMinutesChanged(section: Int, minutes: Int) = updateState {
        copy(
            setup = setup.copy(
                pauseMinutesBySection = setup.pauseMinutesBySection + (section to minutes.coerceAtLeast(0)),
            ),
        )
    }

    fun onPauseSetupContinue() = updateState {
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
            selectedIndex = selectedIndex,
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
            selectedIndex = selectedIndex,
            type = CheckEventType.CheckOut,
            raceMinutes = checkMinutes,
        )
        val nextIndex = selectVp(selectedIndex + 1, vp.projection.stations.lastIndex)
        copy(
            checkIns = newCheckIns,
            checkEvents = newEvents,
            vp = vp.copy(selectedIndex = nextIndex, checkSheetOpen = false),
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
        val newEvents = appendLiveEvent(
            selectedIndex = vp.selectedIndex,
            type = when (vp.checkAction) {
                CheckAction.CheckIn -> CheckEventType.CheckIn
                CheckAction.CheckOut -> CheckEventType.CheckOut
            },
            raceMinutes = vp.checkMinutes,
        )
        val nextIndex = when (vp.checkAction) {
            CheckAction.CheckIn -> vp.selectedIndex
            CheckAction.CheckOut -> selectVp(vp.selectedIndex + 1, vp.projection.stations.lastIndex)
        }
        copy(
            tab = if (vp.checkAction == CheckAction.CheckOut) AppTab.Vp else AppTab.List,
            checkIns = newCheckIns,
            checkEvents = newEvents,
            vp = vp.copy(selectedIndex = nextIndex, checkSheetOpen = false),
        )
    }

    fun onCheckOutNow() = onCheckOutNowSave()

    fun onLiveRoleSelected(role: LiveRole) {
        if (!liveSharingEnabled) return
        updateState {
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(role = role)))
        }
        syncLiveSubscription()
    }

    fun onRunCodeChanged(runCode: String) {
        if (!liveSharingEnabled) return
        updateState {
            val normalizedCode = runCode
                .uppercase()
                .filter { it.isLetterOrDigit() }
                .take(MAX_RUN_CODE_LENGTH)
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(runCode = normalizedCode)))
        }
        publishCurrentRunInfo()
        syncLiveSubscription()
    }

    fun onCreateRunCode() {
        if (!liveSharingEnabled) return
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
        publishCurrentRunInfo()
        syncLiveSubscription()
    }

    fun onLiveSharingToggle(enabled: Boolean) {
        if (!liveSharingEnabled) return
        updateState {
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(isEnabled = enabled)))
        }
        publishCurrentRunInfo()
        syncLiveSubscription()
    }

    fun onResetAllData() {
        resetSession()
        _uiState.value = createInitialState()
    }

    private fun createInitialState(): AppUiState {
        val session = loadSession()
        val liveRunLink = if (liveSharingEnabled) session.liveRunLink else LiveRunLink()
        val tab = session.tab.toAppTab().let { savedTab ->
            if (!liveSharingEnabled && (savedTab == AppTab.Role || savedTab == AppTab.SupportCode)) {
                AppTab.Race
            } else {
                savedTab
            }
        }
        val initialProjection = calculator.project(session.estimate, session.checkIns, session.selectedIndex)
        return buildUiState(
            tab = tab,
            estimate = session.estimate,
            hasSelectedRace = session.hasSelectedRace,
            pauseMinutesBySection = mergePauseMinutesBySection(
                raceId = session.estimate.raceId,
                overrides = session.pauseMinutesBySection,
            ),
            selectedIndex = session.selectedIndex.coerceIn(0, initialProjection.stations.lastIndex),
            checkIns = session.checkIns,
            checkEvents = session.checkEvents,
            liveRunLink = liveRunLink,
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
                hasSelectedRace = mutated.setup.hasSelectedRace,
                pauseMinutesBySection = mutated.setup.pauseMinutesBySection,
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
        hasSelectedRace: Boolean,
        pauseMinutesBySection: Map<Int, Int>,
        selectedIndex: Int,
        checkIns: List<CheckIn>,
        checkEvents: List<CheckEvent>,
        liveRunLink: LiveRunLink,
        checkSheetOpen: Boolean,
        checkAction: CheckAction,
        checkMinutesOverride: Int?,
    ): AppUiState {
        val normalizedPauseMinutes = mergePauseMinutesBySection(
            raceId = estimate.raceId,
            overrides = pauseMinutesBySection,
        )
        val stationsWithPauseOverrides = RaceDefinitions.byId(estimate.raceId).stations.map { station ->
            station.copy(stopMinutes = normalizedPauseMinutes[station.section] ?: station.stopMinutes)
        }
        val projection = calculator.project(estimate, checkIns, selectedIndex, stationsWithPauseOverrides)
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
            setup = SetupUiState(
                estimate = estimate,
                hasSelectedRace = hasSelectedRace,
                pauseMinutesBySection = normalizedPauseMinutes,
            ),
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

    private fun AppUiState.appendLiveEvent(selectedIndex: Int, type: CheckEventType, raceMinutes: Int): List<CheckEvent> {
        val link = settings.liveRunLink
        if (!link.canPublish) return checkEvents

        val station = vp.projection.stations[selectedIndex].station
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
                hasSelectedRace = state.setup.hasSelectedRace,
                pauseMinutesBySection = state.setup.pauseMinutesBySection,
                tab = state.tab.toSavedTab(),
                selectedIndex = state.vp.selectedIndex,
                checkIns = state.checkIns,
                liveRunLink = state.settings.liveRunLink,
                checkEvents = state.checkEvents,
            ),
        )
    }

    private fun syncLiveSubscription(link: LiveRunLink = _uiState.value.settings.liveRunLink) {
        if (!liveSharingEnabled) return
        val requestedCode = link.runCode.takeIf { link.canSubscribe }
        if (requestedCode == liveSubscriptionCode) return

        liveSubscription?.close()
        liveSubscription = null
        liveSubscriptionCode = null

        if (requestedCode == null) return

        liveSubscriptionCode = requestedCode
        liveSubscription = liveRaceRepository.subscribe(requestedCode) { remoteSnapshot ->
            applyRemoteSnapshot(requestedCode, remoteSnapshot)
        }
    }

    private fun applyRemoteSnapshot(runCode: String, remoteSnapshot: LiveRunSnapshot) {
        updateState {
            if (settings.liveRunLink.runCode != runCode || !settings.liveRunLink.canSubscribe) return@updateState this

            val activeEstimate = remoteSnapshot.info?.estimate ?: setup.estimate
            val mergedEvents = (checkEvents + remoteSnapshot.events)
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
            val currentStationSection = vp.projection.stations.getOrNull(vp.selectedIndex)?.station?.section
            val firstOpenIndex = RaceDefinitions.byId(activeEstimate.raceId).stations
                .indexOfFirst { station ->
                    remoteCheckIns.none {
                        it.stationSection == station.section && it.actualDepartureMinutes != null
                    }
                }
                .let { index -> if (index >= 0) index else RaceDefinitions.byId(activeEstimate.raceId).stations.lastIndex }
            val shouldAdvanceAfterCheckout = currentStationSection != null &&
                remoteCheckIns.any {
                    it.stationSection == currentStationSection && it.actualDepartureMinutes != null
                } &&
                vp.selectedIndex < vp.projection.stations.lastIndex
            copy(
                tab = if (remoteSnapshot.info != null && tab == AppTab.SupportCode) AppTab.Vp else tab,
                setup = setup.copy(
                    estimate = activeEstimate,
                    hasSelectedRace = true,
                    pauseMinutesBySection = mergePauseMinutesBySection(
                        raceId = activeEstimate.raceId,
                        overrides = setup.pauseMinutesBySection,
                    ),
                ),
                checkIns = remoteCheckIns,
                checkEvents = mergedEvents,
                vp = vp.copy(
                    selectedIndex = when {
                        tab == AppTab.SupportCode -> firstOpenIndex
                        shouldAdvanceAfterCheckout -> vp.selectedIndex + 1
                        else -> vp.selectedIndex
                    },
                ),
            )
        }
    }

    private fun publishCurrentRunInfo() {
        if (!liveSharingEnabled) return
        val state = _uiState.value
        val link = state.settings.liveRunLink
        if (!link.canPublish) return

        liveRaceRepository.publishRunInfo(
            LiveRunInfo(
                runCode = link.runCode,
                estimate = state.setup.estimate,
                createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            ),
        )
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

    private fun defaultPauseMinutesBySection(raceId: String): Map<Int, Int> =
        RaceDefinitions.byId(raceId).stations.associate { it.section to it.stopMinutes }

    private fun mergePauseMinutesBySection(
        raceId: String,
        overrides: Map<Int, Int>,
    ): Map<Int, Int> = RaceDefinitions.byId(raceId).stations.associate { station ->
        station.section to (overrides[station.section] ?: station.stopMinutes)
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
