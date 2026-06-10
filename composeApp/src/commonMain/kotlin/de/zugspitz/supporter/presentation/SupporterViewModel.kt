package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.LiveSharingLogger
import de.zugspitz.supporter.NoOpSupporterPushNotifications
import de.zugspitz.supporter.SupporterPushNotifications
import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.DefaultSessionRepository
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.RaceCalculator
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.SessionRepository
import de.zugspitz.supporter.data.START_LINE_SECTION
import de.zugspitz.supporter.data.TargetTimeMode
import de.zugspitz.supporter.data.formatRaceTime
import de.zugspitz.supporter.data.toAppTab
import de.zugspitz.supporter.data.toSavedTab
import de.zugspitz.supporter.domain.usecase.LoadSessionUseCase
import de.zugspitz.supporter.domain.usecase.ResetSessionUseCase
import de.zugspitz.supporter.domain.usecase.AutoCheckAction
import de.zugspitz.supporter.domain.usecase.AutoCheckInOutUseCase
import de.zugspitz.supporter.domain.usecase.ApplyCheckEventUseCase
import de.zugspitz.supporter.domain.usecase.RemoteSnapshotMergeUseCase
import de.zugspitz.supporter.domain.usecase.SaveCheckInUseCase
import de.zugspitz.supporter.domain.usecase.SaveSessionUseCase
import de.zugspitz.supporter.domain.usecase.SelectVpUseCase
import de.zugspitz.supporter.domain.usecase.UpdateEstimateUseCase
import de.zugspitz.supporter.presentation.state.AppUiState
import de.zugspitz.supporter.presentation.state.CheckAction
import de.zugspitz.supporter.presentation.state.LastLiveEventUiState
import de.zugspitz.supporter.presentation.state.OfflineMapUiState
import de.zugspitz.supporter.presentation.state.SettingsUiState
import de.zugspitz.supporter.presentation.state.SetupUiState
import de.zugspitz.supporter.presentation.state.VpUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class SupporterViewModel(
    sessionRepository: SessionRepository = DefaultSessionRepository(),
    private val liveRaceRepository: LiveRaceRepository = NoOpLiveRaceRepository(),
    private val liveSharingEnabled: Boolean = false,
    private val supporterPushNotifications: SupporterPushNotifications = NoOpSupporterPushNotifications,
    private val calculator: RaceCalculator = RaceCalculator(),
    private val currentMinutesOfDay: () -> Int = ::systemMinutesOfDay,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val loadSession = LoadSessionUseCase(sessionRepository)
    private val saveSession = SaveSessionUseCase(sessionRepository)
    private val resetSession = ResetSessionUseCase(sessionRepository)
    private val updateEstimate = UpdateEstimateUseCase()
    private val selectVp = SelectVpUseCase()
    private val saveCheckIn = SaveCheckInUseCase()
    private val autoCheckInOut = AutoCheckInOutUseCase()
    private val applyCheckEventUseCase = ApplyCheckEventUseCase(saveCheckIn)
    private val mergeRemoteSnapshot = RemoteSnapshotMergeUseCase()
    private val liveSharingService = LiveSharingService(
        repository = liveRaceRepository,
        pushNotifications = supporterPushNotifications,
        enabled = liveSharingEnabled,
    )

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val autoCheckedInSections = mutableSetOf<Int>()
    private val autoCheckedOutSections = mutableSetOf<Int>()

    init {
        syncLiveSubscription(_uiState.value.settings.liveRunLink)
        syncSupporterPushRegistration(_uiState.value.settings.liveRunLink)
    }

    fun onTabSelected(tab: AppTab) = updateState { copy(tab = tab) }

    fun onOfflineMapDownloadStart() = updateState {
        val totalTiles = vp.projection.stations.size * 240
        copy(
            offlineMap = offlineMap.copy(
                isDownloading = true,
                isReady = false,
                progressPercent = 15,
                downloadedTiles = (totalTiles * 0.15).toInt(),
                totalTiles = totalTiles,
                downloadedMegabytes = 28.0,
            ),
        )
    }

    fun onOfflineMapDownloadProgress(percent: Int) = updateState {
        val normalized = percent.coerceIn(0, 100)
        val totalTiles = offlineMap.totalTiles.coerceAtLeast(vp.projection.stations.size * 240)
        copy(
            offlineMap = offlineMap.copy(
                isDownloading = normalized < 100,
                isReady = normalized >= 100,
                progressPercent = normalized,
                downloadedTiles = (totalTiles * (normalized / 100.0)).toInt(),
                totalTiles = totalTiles,
                downloadedMegabytes = (totalTiles * (normalized / 100.0) * 0.012),
            ),
        )
    }

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
        syncSupporterPushRegistration()
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
        syncSupporterPushRegistration()
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
        syncSupporterPushRegistration()
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
        syncSupporterPushRegistration()
    }

    fun onEstimateChange(estimate: RaceEstimate) {
        updateState {
            val normalizedPauseMinutes = mergePauseMinutesBySection(
                raceId = estimate.raceId,
                overrides = setup.pauseMinutesBySection,
            )
            if (!estimate.hasValidTargetDurations(totalStopMinutes(estimate.raceId, normalizedPauseMinutes))) {
                return@updateState this
            }
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
            tab = AppTab.Vp,
            vp = vp.copy(selectedIndex = 0),
        )
    }

    fun onSetupBack() = updateState {
        copy(tab = AppTab.Race)
    }

    fun onPauseMinutesChanged(section: Int, minutes: Int) = updateState {
        val updatedPauseMinutes = setup.pauseMinutesBySection + (section to minutes.coerceAtLeast(0))
        val normalizedPauseMinutes = mergePauseMinutesBySection(
            raceId = setup.estimate.raceId,
            overrides = updatedPauseMinutes,
        )
        if (!setup.estimate.hasValidTargetDurations(totalStopMinutes(setup.estimate.raceId, normalizedPauseMinutes))) {
            return@updateState this
        }
        copy(
            setup = setup.copy(
                pauseMinutesBySection = updatedPauseMinutes,
            ),
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

    fun onActualStartNowSave() = updateState {
        val startMinutes = currentCheckMinutes()
        val result = applyCheckEvent(
            stationSection = START_LINE_SECTION,
            stationName = START_LINE_NAME,
            type = CheckEventType.CheckIn,
            raceMinutes = startMinutes,
        )
        copy(checkIns = result.checkIns, checkEvents = result.checkEvents)
    }

    fun onCheckInNowSave(stationIndex: Int = uiState.value.vp.selectedIndex) = updateState {
        val selectedIndex = stationIndex.coerceIn(0, vp.projection.stations.lastIndex)
        val station = vp.projection.stations[selectedIndex].station
        val checkMinutes = currentCheckMinutes()
        val result = applyCheckEvent(
            stationSection = station.section,
            stationName = station.name,
            type = CheckEventType.CheckIn,
            raceMinutes = checkMinutes,
        )
        copy(
            checkIns = result.checkIns,
            checkEvents = result.checkEvents,
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
        val result = applyCheckEvent(
            stationSection = selected.station.section,
            stationName = selected.station.name,
            type = CheckEventType.CheckOut,
            raceMinutes = checkMinutes,
        )
        val nextIndex = selectVp(selectedIndex + 1, vp.projection.stations.lastIndex)
        copy(
            checkIns = result.checkIns,
            checkEvents = result.checkEvents,
            vp = vp.copy(selectedIndex = nextIndex, checkSheetOpen = false),
        )
    }

    fun onCheckInDismiss() = updateState { copy(vp = vp.copy(checkSheetOpen = false)) }

    fun onCheckInSave() = updateState {
        val station = vp.projection.stations[vp.selectedIndex].station
        val result = applyCheckEvent(
            stationSection = station.section,
            stationName = station.name,
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
            checkIns = result.checkIns,
            checkEvents = result.checkEvents,
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
        syncSupporterPushRegistration()
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
        syncSupporterPushRegistration()
    }

    fun onRunnerNameChanged(runnerName: String) {
        if (!liveSharingEnabled) return
        updateState {
            copy(
                settings = settings.copy(
                    liveRunLink = settings.liveRunLink.copy(
                        runnerName = runnerName.normalizeRunnerName(),
                    ),
                ),
            )
        }
        publishCurrentRunInfo()
    }

    fun onCreateRunCode() {
        if (!liveSharingEnabled) return
        val estimate = _uiState.value.setup.estimate
        scope.launch {
            val liveRunInfo = liveSharingService.createRun(estimate)

            if (liveRunInfo == null) {
                return@launch
            }

            updateState {
                copy(
                    settings = settings.copy(
                        liveRunLink = settings.liveRunLink.copy(
                            role = LiveRole.Runner,
                            runCode = liveRunInfo.runCode,
                            isEnabled = true,
                        ),
                    ),
                )
            }
            publishCurrentRunInfo()
            syncLiveSubscription()
            syncSupporterPushRegistration()
        }
    }

    fun onLiveSharingToggle(enabled: Boolean) {
        if (!liveSharingEnabled) return
        updateState {
            copy(settings = settings.copy(liveRunLink = settings.liveRunLink.copy(isEnabled = enabled)))
        }
        publishCurrentRunInfo()
        syncLiveSubscription()
        syncSupporterPushRegistration()
    }

    fun onAutoCheckInOutToggle(enabled: Boolean) {
        if (!liveSharingEnabled) return
        updateState {
            copy(settings = settings.copy(autoCheckInOutEnabled = enabled))
        }
    }

    fun onRunnerLocationChanged(location: LiveRunnerLocation) {
        if (!liveSharingEnabled) return
        updateState {
            val link = settings.liveRunLink
            if (!link.canPublish || location.runCode != link.runCode) return@updateState this
            liveSharingService.publishRunnerLocation(location)
            copy(runnerLocation = location).applyAutomaticCheckInOut(location)
        }
    }

    fun onResetAllData() {
        val liveRunLink = _uiState.value.settings.liveRunLink
        liveSharingService.deleteRun(liveRunLink)
        syncLiveSubscription(LiveRunLink())
        syncSupporterPushRegistration(LiveRunLink())
        resetSession()
        _uiState.value = createInitialState()
    }

    fun onAppForegrounded() {
        val link = _uiState.value.settings.liveRunLink
        if (!link.canSubscribe) return
        LiveSharingLogger.d("App foregrounded, reconnecting live subscription for runCode=${link.runCode}")
        syncLiveSubscription(link = link, forceReconnect = true)
        syncSupporterPushRegistration(link)
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
            autoCheckInOutEnabled = session.autoCheckInOutEnabled,
            checkSheetOpen = false,
            checkAction = CheckAction.CheckIn,
            checkMinutesOverride = null,
            offlineMap = OfflineMapUiState(),
            runnerLocation = null,
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
                autoCheckInOutEnabled = mutated.settings.autoCheckInOutEnabled,
                checkSheetOpen = mutated.vp.checkSheetOpen,
                checkAction = mutated.vp.checkAction,
                checkMinutesOverride = mutated.vp.checkMinutes,
                offlineMap = mutated.offlineMap,
                runnerLocation = mutated.runnerLocation,
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
        autoCheckInOutEnabled: Boolean,
        checkSheetOpen: Boolean,
        checkAction: CheckAction,
        checkMinutesOverride: Int?,
        offlineMap: OfflineMapUiState,
        runnerLocation: LiveRunnerLocation?,
    ): AppUiState {
        val normalizedPauseMinutes = mergePauseMinutesBySection(
            raceId = estimate.raceId,
            overrides = pauseMinutesBySection,
        )
        val stationsWithPauseOverrides = RaceDefinitions.byId(estimate.raceId).stations.map { station ->
            station.copy(stopMinutes = normalizedPauseMinutes[station.section] ?: station.stopMinutes)
        }
        val safeEstimate = estimate.coerceToValidTargetDurations(
            totalStopMinutes = stationsWithPauseOverrides.sumOf { it.stopMinutes },
        )
        val projection = calculator.project(safeEstimate, checkIns, selectedIndex, stationsWithPauseOverrides)
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
                estimate = safeEstimate,
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
            offlineMap = offlineMap,
            settings = SettingsUiState(
                liveRunLink = liveRunLink,
                autoCheckInOutEnabled = autoCheckInOutEnabled,
                lastLiveEvent = checkEvents.lastOrNull()?.toLastLiveEventUiState(estimate.startTimeMinutes),
            ),
            runnerLocation = runnerLocation?.takeIf { it.raceId == safeEstimate.raceId },
        )
    }

    private fun AppUiState.applyCheckEvent(
        stationSection: Int,
        stationName: String,
        type: CheckEventType,
        raceMinutes: Int,
    ) = applyCheckEventUseCase(
        checkIns = checkIns,
        checkEvents = checkEvents,
        runCode = settings.liveRunLink.runCode.takeIf { settings.liveRunLink.canPublish },
        stationSection = stationSection,
        stationName = stationName,
        type = type,
        raceMinutes = raceMinutes,
        createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
        runnerLocation = runnerLocation?.takeIf {
            it.runCode == settings.liveRunLink.runCode && it.raceId == setup.estimate.raceId
        },
    ).also { result ->
        result.event?.let(liveSharingService::publishEvent)
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
                autoCheckInOutEnabled = state.settings.autoCheckInOutEnabled,
                checkEvents = state.checkEvents,
            ),
        )
    }

    private fun AppUiState.applyAutomaticCheckInOut(location: LiveRunnerLocation): AppUiState {
        val raceMinutes = raceMinutesForEpochMillis(location.updatedAtEpochMillis)
        return when (
            val action = autoCheckInOut(
                projection = vp.projection,
                location = location,
                enabled = settings.autoCheckInOutEnabled,
                canPublish = settings.liveRunLink.canPublish,
                alreadyAutoCheckedIn = autoCheckedInSections,
                alreadyAutoCheckedOut = autoCheckedOutSections,
                raceMinutes = raceMinutes,
            )
        ) {
            AutoCheckAction.None -> this
            is AutoCheckAction.SkippedLowAccuracy -> {
                LiveSharingLogger.d(
                    "Skipping automatic check-in/out because accuracy=${action.accuracyMeters.formatForLog()}m is too low.",
                )
                this
            }
            is AutoCheckAction.CheckIn -> applyAutomaticCheckIn(location, action)
            is AutoCheckAction.CheckOut -> applyAutomaticCheckOut(location, action)
        }
    }

    private fun AppUiState.applyAutomaticCheckIn(
        location: LiveRunnerLocation,
        action: AutoCheckAction.CheckIn,
    ): AppUiState {
        val station = vp.projection.stations[action.stationIndex].station
        LiveSharingLogger.d(
            "Automatic check-in station=${action.stationSection} km=${location.distanceKm.formatForLog()} " +
                "deltaMeters=${(action.distanceDeltaKm * 1000.0).formatForLog()}",
        )
        autoCheckedInSections.add(action.stationSection)
        val result = applyCheckEvent(
            stationSection = station.section,
            stationName = station.name,
            type = CheckEventType.CheckIn,
            raceMinutes = action.raceMinutes,
        )
        return copy(
            checkIns = result.checkIns,
            checkEvents = result.checkEvents,
            vp = vp.copy(selectedIndex = action.stationIndex, checkSheetOpen = false),
        )
    }

    private fun AppUiState.applyAutomaticCheckOut(
        location: LiveRunnerLocation,
        action: AutoCheckAction.CheckOut,
    ): AppUiState {
        val station = vp.projection.stations[action.stationIndex]
        LiveSharingLogger.d(
            "Automatic check-out station=${action.stationSection} km=${location.distanceKm.formatForLog()} " +
                "pastMeters=${(action.distancePastStationKm * 1000.0).formatForLog()}",
        )
        autoCheckedOutSections.add(action.stationSection)
        val result = applyCheckEvent(
            stationSection = station.station.section,
            stationName = station.station.name,
            type = CheckEventType.CheckOut,
            raceMinutes = action.raceMinutes,
        )
        val nextIndex = selectVp(action.stationIndex + 1, vp.projection.stations.lastIndex)
        return copy(
            checkIns = result.checkIns,
            checkEvents = result.checkEvents,
            vp = vp.copy(selectedIndex = nextIndex, checkSheetOpen = false),
        )
    }

    private fun AppUiState.raceMinutesForEpochMillis(epochMillis: Long): Int {
        val localDateTime = kotlin.time.Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val locationMinutesOfDay = localDateTime.hour * 60 + localDateTime.minute
        val diff = locationMinutesOfDay - setup.estimate.startTimeMinutes
        return when {
            diff < -HALF_DAY_MINUTES -> diff + FULL_DAY_MINUTES
            diff > HALF_DAY_MINUTES -> diff - FULL_DAY_MINUTES
            else -> diff
        }
    }

    private fun syncLiveSubscription(
        link: LiveRunLink = _uiState.value.settings.liveRunLink,
        forceReconnect: Boolean = false,
    ) {
        liveSharingService.syncSubscription(
            link = link,
            onSnapshot = ::applyRemoteSnapshot,
            forceReconnect = forceReconnect,
        )
    }

    private fun syncSupporterPushRegistration(link: LiveRunLink = _uiState.value.settings.liveRunLink) {
        liveSharingService.syncSupporterPushRegistration(
            link = link,
            currentLink = { _uiState.value.settings.liveRunLink },
        )
    }

    private fun applyRemoteSnapshot(runCode: String, remoteSnapshot: LiveRunSnapshot) {
        updateState {
            if (settings.liveRunLink.runCode != runCode || !settings.liveRunLink.canSubscribe) return@updateState this

            val currentStationSection = vp.projection.stations.getOrNull(vp.selectedIndex)?.station?.section
            val merged = mergeRemoteSnapshot(
                currentTab = tab,
                currentEstimate = setup.estimate,
                currentEvents = checkEvents,
                currentSelectedIndex = vp.selectedIndex,
                currentStationSection = currentStationSection,
                currentLastStationIndex = vp.projection.stations.lastIndex,
                remoteSnapshot = remoteSnapshot,
            )
            copy(
                tab = merged.tab,
                setup = setup.copy(
                    estimate = merged.estimate,
                    hasSelectedRace = true,
                    pauseMinutesBySection = mergePauseMinutesBySection(
                        raceId = merged.estimate.raceId,
                        overrides = setup.pauseMinutesBySection,
                    ),
                ),
                checkIns = merged.checkIns,
                checkEvents = merged.checkEvents,
                runnerLocation = merged.runnerLocation,
                vp = vp.copy(
                    selectedIndex = merged.selectedIndex,
                ),
            )
        }
    }

    private fun publishCurrentRunInfo() {
        val state = _uiState.value
        liveSharingService.publishRunInfo(
            link = state.settings.liveRunLink,
            estimate = state.setup.estimate,
        )
    }

    private fun CheckEvent.toLastLiveEventUiState(startTimeMinutes: Int) = LastLiveEventUiState(
        type = type,
        stationName = stationName.removePrefix("Z$stationSection "),
        raceTime = formatRaceTime(startTimeMinutes + raceMinutes),
    )

    private companion object {
        const val MAX_RUN_CODE_LENGTH = 8
        const val START_LINE_NAME = "Start"
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
        return when {
            diff < -HALF_DAY_MINUTES -> diff + FULL_DAY_MINUTES
            diff > HALF_DAY_MINUTES -> diff - FULL_DAY_MINUTES
            else -> diff
        }
    }
}

private fun systemMinutesOfDay(): Int {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.hour * 60 + now.minute
}

private fun RaceEstimate.hasValidTargetDurations(totalStopMinutes: Int): Boolean = when (targetMode) {
    TargetTimeMode.Fixed -> fixedDurationMinutes > totalStopMinutes
    TargetTimeMode.Range -> minDurationMinutes > totalStopMinutes && maxDurationMinutes > totalStopMinutes
}

private fun RaceEstimate.coerceToValidTargetDurations(totalStopMinutes: Int): RaceEstimate {
    val minimumDurationMinutes = totalStopMinutes + 1
    return when (targetMode) {
        TargetTimeMode.Fixed -> copy(
            fixedDurationMinutes = fixedDurationMinutes.coerceAtLeast(minimumDurationMinutes),
        )
        TargetTimeMode.Range -> {
            val safeMinDuration = minDurationMinutes.coerceAtLeast(minimumDurationMinutes)
            copy(
                minDurationMinutes = safeMinDuration,
                maxDurationMinutes = maxDurationMinutes.coerceAtLeast(safeMinDuration),
            )
        }
    }
}

private fun Double.formatForLog(): String = (this * 10.0).toInt().let { roundedTenths ->
    "${roundedTenths / 10}.${roundedTenths % 10}"
}

private fun String.normalizeRunnerName(): String =
    trim()
        .replace(Regex("\\s+"), " ")
        .take(MAX_RUNNER_NAME_LENGTH)

private fun totalStopMinutes(
    raceId: String,
    pauseMinutesBySection: Map<Int, Int>,
): Int = RaceDefinitions.byId(raceId).stations.sumOf { station ->
    pauseMinutesBySection[station.section] ?: station.stopMinutes
}

private const val FULL_DAY_MINUTES = 24 * 60
private const val HALF_DAY_MINUTES = 12 * 60
private const val MAX_RUNNER_NAME_LENGTH = 40
