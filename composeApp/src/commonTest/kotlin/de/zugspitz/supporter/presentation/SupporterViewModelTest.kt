package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.SupporterPushNotifications
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRaceSubscription
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.data.LiveRunInfo
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.PushPlatform
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.START_LINE_SECTION
import de.zugspitz.supporter.data.SavedTab
import de.zugspitz.supporter.data.SessionRepository
import de.zugspitz.supporter.data.TargetTimeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupporterViewModelTest {
    @Test
    fun `first start defaults to race selection when live sharing is disabled`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Race, state.tab)
        assertEquals(0, state.vp.selectedIndex)
    }

    @Test
    fun `runner mode routes to race selection`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRunnerModeSelected()

        val state = viewModel.uiState.value
        assertEquals(AppTab.Race, state.tab)
        assertEquals(LiveRole.Runner, state.settings.liveRunLink.role)
    }

    @Test
    fun `supporter mode routes directly to support code entry`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            liveSharingEnabled = true,
        )

        viewModel.onSupporterModeSelected()

        val state = viewModel.uiState.value
        assertEquals(AppTab.SupportCode, state.tab)
        assertEquals(LiveRole.Supporter, state.settings.liveRunLink.role)
    }

    @Test
    fun `supporter without code can continue to race selection`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onSupporterModeSelected()
        viewModel.onContinueWithoutSupportCode()

        assertEquals(AppTab.Race, viewModel.uiState.value.tab)
    }

    @Test
    fun `restores last session including vp card index`() {
        val repository = FakeSessionRepository()
        repository.save(
            AppSessionState(
                estimate = RaceEstimate(
                    targetMode = TargetTimeMode.Fixed,
                    fixedDurationMinutes = 16 * 60 + 30,
                ),
                tab = SavedTab.Vp,
                selectedIndex = 4,
                checkIns = listOf(CheckIn(stationSection = 3, actualArrivalMinutes = 500)),
            ),
        )

        val viewModel = SupporterViewModel(sessionRepository = repository)
        val state = viewModel.uiState.value

        assertEquals(AppTab.Vp, state.tab)
        assertEquals(4, state.vp.selectedIndex)
        assertEquals(1, state.checkIns.size)
        assertEquals(TargetTimeMode.Fixed, state.setup.estimate.targetMode)
    }

    @Test
    fun `reset clears state and routes to race selection when live sharing is disabled`() {
        val repository = FakeSessionRepository().apply {
            save(AppSessionState(tab = SavedTab.Vp, selectedIndex = 5))
        }
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onResetAllData()
        val state = viewModel.uiState.value

        assertEquals(AppTab.Race, state.tab)
        assertEquals(0, state.vp.selectedIndex)
        assertTrue(state.checkIns.isEmpty())
        assertEquals(AppTab.Race, SupporterViewModel(sessionRepository = repository).uiState.value.tab)
    }

    @Test
    fun `runner reset deletes live run in supabase repository`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onRunCodeChanged("run42")
        viewModel.onLiveSharingToggle(true)
        viewModel.onResetAllData()

        assertEquals(listOf("RUN42"), liveRepository.deletedRuns)
        assertEquals("", viewModel.uiState.value.settings.liveRunLink.runCode)
    }

    @Test
    fun `supporter reset keeps remote live run`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onSupporterModeSelected()
        viewModel.onRunCodeChanged("run42")
        viewModel.onSupportCodeConnect()
        viewModel.onResetAllData()

        assertTrue(liveRepository.deletedRuns.isEmpty())
    }

    @Test
    fun `check in now uses current time of day relative to start`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 15 },
        )

        viewModel.onCalculateClick()
        viewModel.onCheckInOpen()
        viewModel.onCheckInNow()

        val state = viewModel.uiState.value
        assertEquals(75, state.vp.checkMinutes)
        assertEquals("23:15", state.vp.checkInputTime)
    }

    @Test
    fun `selecting ehrwald trail updates start time stations and clears check ins`() {
        val repository = FakeSessionRepository(
            AppSessionState(
                tab = SavedTab.Setup,
                selectedIndex = 4,
                checkIns = listOf(CheckIn(stationSection = 3, actualArrivalMinutes = 281)),
            ),
        )
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.EhrwaldTrailId)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.EhrwaldTrailId, state.setup.estimate.raceId)
        assertEquals(23 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(0, state.vp.selectedIndex)
        assertTrue(state.checkIns.isEmpty())
        assertEquals("Z3 Pestkapelle", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `ehrwald check in now uses 23 o clock start`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 15 },
        )

        viewModel.onRaceSelected(RaceDefinitions.EhrwaldTrailId)
        viewModel.onCalculateClick()
        viewModel.onCheckInOpen()
        viewModel.onCheckInNow()

        val state = viewModel.uiState.value
        assertEquals(15, state.vp.checkMinutes)
        assertEquals("23:15", state.vp.checkInputTime)
    }

    @Test
    fun `selecting leutasch trail uses 9 o clock start and leutasch stations`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.LeutaschTrailId)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.LeutaschTrailId, state.setup.estimate.raceId)
        assertEquals(9 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(7, state.vp.projection.stations.size)
        assertEquals("Z5 Hubertushof", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `selecting mittenwald trail uses 7 o clock start and mittenwald stations`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.MittenwaldTrailId)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.MittenwaldTrailId, state.setup.estimate.raceId)
        assertEquals(7 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(5, state.vp.projection.stations.size)
        assertEquals("Z7 Schloss Elmau", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `selecting garmisch partenkirchen trail uses 10 o clock start and garmisch stations`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.GarmischPartenkirchenTrailId)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.GarmischPartenkirchenTrailId, state.setup.estimate.raceId)
        assertEquals(10 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(4, state.vp.projection.stations.size)
        assertEquals("Z8 Laubhütte", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `selecting grainau trail uses 18 o clock start and grainau stations`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.GrainauTrailId)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.GrainauTrailId, state.setup.estimate.raceId)
        assertEquals(18 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(2, state.vp.projection.stations.size)
        assertEquals("Z10 Tröglift", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `selecting zut 100 uses 20 o clock start and long course stations`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.Zut100Id)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
        assertEquals(RaceDefinitions.Zut100Id, state.setup.estimate.raceId)
        assertEquals(20 * 60, state.setup.estimate.startTimeMinutes)
        assertEquals(16, state.vp.projection.stations.size)
        assertEquals("Z1 Eibsee", state.vp.projection.stations.first().station.name)
    }

    @Test
    fun `check in now save stores arrival without opening sheet`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 15 },
        )

        viewModel.onCalculateClick()
        viewModel.onCheckInNowSave()

        val state = viewModel.uiState.value
        assertEquals(AppTab.Vp, state.tab)
        assertEquals(1, state.checkIns.size)
        assertEquals(1, state.checkIns.first().stationSection)
        assertEquals(75, state.checkIns.first().actualArrivalMinutes)
        assertTrue(state.vp.projection.stations.first().isCheckedIn)
        assertEquals("23:15", state.vp.projection.stations.first().actualArrival)
    }

    @Test
    fun `actual start button stores start offset and shifts first station`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 22 * 60 + 9 },
        )
        val firstPlannedBefore = viewModel.uiState.value.vp.projection.stations.first().station.plannedArrivalMinutes

        viewModel.onCalculateClick()
        viewModel.onActualStartNowSave()

        val state = viewModel.uiState.value
        assertEquals(START_LINE_SECTION, state.checkIns.first().stationSection)
        assertEquals(9, state.checkIns.first().actualArrivalMinutes)
        assertEquals(9, state.vp.projection.actualStartMinutes)
        assertEquals(firstPlannedBefore + 9, state.vp.projection.stations.first().station.plannedArrivalMinutes)
    }

    @Test
    fun `actual start before planned start remains a negative offset`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 15 * 60 },
        )

        viewModel.onCalculateClick()
        viewModel.onActualStartNowSave()

        val state = viewModel.uiState.value
        assertEquals(-7 * 60, state.checkIns.first().actualArrivalMinutes)
        assertEquals(-7 * 60, state.vp.projection.actualStartMinutes)
    }

    @Test
    fun `check in after midnight is treated as next race day`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 1 * 60 },
        )

        viewModel.onCalculateClick()
        viewModel.onCheckInNowSave()

        assertEquals(3 * 60, viewModel.uiState.value.checkIns.first().actualArrivalMinutes)
    }

    @Test
    fun `check in on previous evening before morning start remains negative`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 22 * 60 },
        )

        viewModel.onRaceSelected(RaceDefinitions.LeutaschTrailId)
        viewModel.onCalculateClick()
        viewModel.onCheckInNowSave()

        assertEquals(-11 * 60, viewModel.uiState.value.checkIns.first().actualArrivalMinutes)
    }

    @Test
    fun `checkout stores departure on existing check in`() {
        val repository = FakeSessionRepository(
            AppSessionState(
                tab = SavedTab.Vp,
                selectedIndex = 2,
                checkIns = listOf(CheckIn(stationSection = 3, actualArrivalMinutes = 281)),
            ),
        )
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 45 },
        )

        viewModel.onCheckOutNowSave()

        val saved = repository.load().checkIns.first { it.stationSection == 3 }
        assertEquals(281, saved.actualArrivalMinutes)
        assertEquals(105, saved.actualDepartureMinutes)
        assertEquals(3, repository.load().selectedIndex)
    }

    @Test
    fun `checkout is ignored without existing check in`() {
        val repository = FakeSessionRepository(AppSessionState(tab = SavedTab.Vp, selectedIndex = 0))
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 45 },
        )

        viewModel.onCheckOutNowSave()

        assertTrue(repository.load().checkIns.isEmpty())
    }

    @Test
    fun `check in and checkout use explicit pager station`() {
        val repository = FakeSessionRepository(AppSessionState(tab = SavedTab.Vp, selectedIndex = 0))
        val viewModel = SupporterViewModel(
            sessionRepository = repository,
            currentMinutesOfDay = { 23 * 60 + 15 },
        )

        viewModel.onCheckInNowSave(stationIndex = 1)

        val state = viewModel.uiState.value
        assertEquals(1, state.vp.selectedIndex)
        assertEquals(2, state.checkIns.single().stationSection)
    }

    @Test
    fun `live sharing publishes check in and check out events for runner`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
            currentMinutesOfDay = { 23 * 60 + 20 },
        )

        viewModel.onRunCodeChanged("abc-123!")
        viewModel.onLiveSharingToggle(true)
        viewModel.onCalculateClick()
        viewModel.onCheckInOpen()
        viewModel.onCheckInNow()
        viewModel.onCheckInSave()
        viewModel.onCheckOutNow()

        assertEquals("ABC123", viewModel.uiState.value.settings.liveRunLink.runCode)
        assertEquals(listOf(CheckEventType.CheckIn, CheckEventType.CheckOut), liveRepository.events.map { it.type })
        assertEquals("ABC123", liveRepository.events.first().runCode)
    }

    @Test
    fun `creating support code publishes selected race estimate`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onRaceSelected(RaceDefinitions.EhrwaldTrailId)
        viewModel.onCreateRunCode()
        waitUntil { liveRepository.runInfos.isNotEmpty() }

        val publishedInfo = liveRepository.runInfos.single()
        assertEquals(viewModel.uiState.value.settings.liveRunLink.runCode, publishedInfo.runCode)
        assertEquals(RaceDefinitions.EhrwaldTrailId, publishedInfo.estimate.raceId)
    }

    @Test
    fun `runner name is normalized and published with run info`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onRunCodeChanged("run42")
        viewModel.onLiveSharingToggle(true)
        liveRepository.runInfos.clear()
        viewModel.onRunnerNameChanged("  Andre\nRedenius  ")

        assertEquals("Andre Redenius", viewModel.uiState.value.settings.liveRunLink.runnerName)
        assertEquals("Andre Redenius", liveRepository.runInfos.single().runnerName)
    }

    @Test
    fun `setup starts with default station stop minutes and can be changed`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.ZugspitzUltratrailId)
        viewModel.onCalculateClick()

        val setupState = viewModel.uiState.value
        assertEquals(AppTab.Vp, setupState.tab)
        assertEquals(2, setupState.setup.pauseMinutesBySection[1])

        viewModel.onPauseMinutesChanged(section = 1, minutes = 9)

        val updatedState = viewModel.uiState.value
        assertEquals(AppTab.Vp, updatedState.tab)
        assertEquals(9, updatedState.setup.pauseMinutesBySection[1])
        assertEquals(9, updatedState.vp.projection.stations.first().station.stopMinutes)
    }

    @Test
    fun `setup back returns to race selection`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onRaceSelected(RaceDefinitions.ZugspitzUltratrailId)
        viewModel.onSetupBack()

        assertEquals(AppTab.Race, viewModel.uiState.value.tab)
    }

    @Test
    fun `all race defaults use a one hour target range`() {
        RaceDefinitions.All.forEach { race ->
            assertEquals(
                60,
                race.defaultMaxDurationMinutes - race.defaultMinDurationMinutes,
                race.id,
            )
        }
    }

    @Test
    fun `supporter subscription applies remote race info and check in events`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onLiveRoleSelected(LiveRole.Supporter)
        viewModel.onRunCodeChanged("RUN42")
        viewModel.onLiveSharingToggle(true)

        liveRepository.emit(
            "RUN42",
            LiveRunSnapshot(
                info = LiveRunInfo(
                    runCode = "RUN42",
                    estimate = RaceDefinitions.byId(RaceDefinitions.EhrwaldTrailId).defaultEstimate(),
                    createdAtEpochMillis = 0L,
                ),
                events = listOf(
                    CheckEvent(
                        id = "RUN42-3-CheckIn-1",
                        runCode = "RUN42",
                        stationSection = 3,
                        stationName = "Z3 Test",
                        type = CheckEventType.CheckIn,
                        raceMinutes = 285,
                        createdAtEpochMillis = 1L,
                    ),
                ),
            ),
        )

        assertEquals(RaceDefinitions.EhrwaldTrailId, viewModel.uiState.value.setup.estimate.raceId)
        assertEquals(1, viewModel.uiState.value.checkIns.size)
        assertEquals(CheckIn(stationSection = 3, actualArrivalMinutes = 285), viewModel.uiState.value.checkIns.first())
        assertEquals(CheckEventType.CheckIn, viewModel.uiState.value.settings.lastLiveEvent?.type)
        assertEquals("Test", viewModel.uiState.value.settings.lastLiveEvent?.stationName)
        assertEquals("03:45", viewModel.uiState.value.settings.lastLiveEvent?.raceTime)
    }

    @Test
    fun `supporter subscription advances to next card after remote checkout`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(AppSessionState(tab = SavedTab.Vp, selectedIndex = 0)),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onLiveRoleSelected(LiveRole.Supporter)
        viewModel.onRunCodeChanged("RUN42")
        viewModel.onLiveSharingToggle(true)
        liveRepository.emit(
            "RUN42",
            LiveRunSnapshot(
                info = LiveRunInfo(
                    runCode = "RUN42",
                    estimate = RaceEstimate(),
                    createdAtEpochMillis = 0L,
                ),
                events = listOf(
                    CheckEvent(
                        id = "RUN42-1-CheckIn-1",
                        runCode = "RUN42",
                        stationSection = 1,
                        stationName = "Z1 Eibsee",
                        type = CheckEventType.CheckIn,
                        raceMinutes = 75,
                        createdAtEpochMillis = 1L,
                    ),
                    CheckEvent(
                        id = "RUN42-1-CheckOut-2",
                        runCode = "RUN42",
                        stationSection = 1,
                        stationName = "Z1 Eibsee",
                        type = CheckEventType.CheckOut,
                        raceMinutes = 82,
                        createdAtEpochMillis = 2L,
                    ),
                ),
            ),
        )

        assertEquals(1, viewModel.uiState.value.vp.selectedIndex)
    }

    @Test
    fun `support code entry enables supporter subscription and opens plan when info arrives`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
        )

        viewModel.onSupporterModeSelected()
        viewModel.onRunCodeChanged("run42")
        viewModel.onSupportCodeConnect()
        liveRepository.emit(
            "RUN42",
            LiveRunSnapshot(
                info = LiveRunInfo(
                    runCode = "RUN42",
                    estimate = RaceDefinitions.byId(RaceDefinitions.GrainauTrailId).defaultEstimate(),
                    createdAtEpochMillis = 0L,
                ),
            ),
        )

        val state = viewModel.uiState.value
        assertEquals(AppTab.Vp, state.tab)
        assertEquals(RaceDefinitions.GrainauTrailId, state.setup.estimate.raceId)
    }

    @Test
    fun `support code connect registers supporter push token`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
            supporterPushNotifications = FakeSupporterPushNotifications(token = "ios-token"),
        )

        viewModel.onSupporterModeSelected()
        viewModel.onRunCodeChanged("run42")
        viewModel.onSupportCodeConnect()

        assertEquals(listOf("RUN42:ios:ios-token:true"), liveRepository.pushTokens)
    }

    @Test
    fun `leaving supporter live mode unregisters supporter push token`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            liveSharingEnabled = true,
            supporterPushNotifications = FakeSupporterPushNotifications(token = "ios-token"),
        )

        viewModel.onSupporterModeSelected()
        viewModel.onRunCodeChanged("run42")
        viewModel.onSupportCodeConnect()
        viewModel.onContinueWithoutSupportCode()

        assertEquals(
            listOf("RUN42:ios:ios-token:true", "RUN42:ios:ios-token:false"),
            liveRepository.pushTokens,
        )
    }

    @Test
    fun `too short fixed target time is ignored instead of crashing during typing`() {
        val viewModel = SupporterViewModel(sessionRepository = FakeSessionRepository())

        viewModel.onEstimateChange(
            viewModel.uiState.value.setup.estimate.copy(
                targetMode = TargetTimeMode.Fixed,
                fixedDurationMinutes = 1,
            ),
        )

        val state = viewModel.uiState.value
        assertTrue(state.setup.estimate.fixedDurationMinutes > 1)
        assertTrue(state.vp.projection.stations.isNotEmpty())
    }
}

private class FakeSessionRepository(
    private var snapshot: AppSessionState = AppSessionState(),
) : SessionRepository {
    override fun load(): AppSessionState = snapshot

    override fun save(state: AppSessionState) {
        this.snapshot = state
    }

    override fun clear() {
        snapshot = AppSessionState()
    }
}

private class FakeLiveRaceRepository : LiveRaceRepository {
    val runInfos = mutableListOf<LiveRunInfo>()
    val events = mutableListOf<CheckEvent>()
    val pushTokens = mutableListOf<String>()
    val deletedRuns = mutableListOf<String>()
    private val listeners = mutableMapOf<String, (LiveRunSnapshot) -> Unit>()

    override suspend fun createRun(estimate: RaceEstimate): LiveRunInfo {
        return LiveRunInfo(
            runCode = "ABC12345",
            estimate = estimate,
            createdAtEpochMillis = 0L,
        )
    }

    override fun deleteRun(runCode: String) {
        deletedRuns += runCode
    }

    override fun publishRunInfo(info: LiveRunInfo) {
        runInfos += info
    }

    override fun publish(event: CheckEvent) {
        events += event
    }

    override fun registerSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        pushTokens += "$runCode:${platform.databaseValue()}:$deviceToken:true"
    }

    override fun unregisterSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        pushTokens += "$runCode:${platform.databaseValue()}:$deviceToken:false"
    }

    override fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription {
        listeners[runCode] = onSnapshotChanged
        return object : LiveRaceSubscription {
            override fun close() {
                listeners.remove(runCode)
            }
        }
    }

    fun emit(runCode: String, snapshot: LiveRunSnapshot) {
        listeners[runCode]?.invoke(snapshot)
    }
}

private class FakeSupporterPushNotifications(
    private val token: String?,
) : SupporterPushNotifications {
    override fun currentToken(): String? = token

    override fun requestToken(onToken: (String) -> Unit) {
        token?.let(onToken)
    }
}

private fun PushPlatform.databaseValue(): String = when (this) {
    PushPlatform.Ios -> "ios"
}

private fun waitUntil(timeoutMs: Long = 1_000L, condition: () -> Boolean) = runBlocking {
    val attempts = (timeoutMs / 25L).coerceAtLeast(1L).toInt()
    repeat(attempts) {
        if (condition()) return@runBlocking
        delay(25L)
    }
}
