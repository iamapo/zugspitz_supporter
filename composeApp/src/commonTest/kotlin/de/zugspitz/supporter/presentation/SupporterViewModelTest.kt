package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRaceSubscription
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.SavedTab
import de.zugspitz.supporter.data.SessionRepository
import de.zugspitz.supporter.data.TargetTimeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupporterViewModelTest {
    @Test
    fun `first start defaults to race selection and Z1`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Race, state.tab)
        assertEquals(0, state.vp.selectedIndex)
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
    fun `reset clears state and routes to race selection`() {
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
    }

    @Test
    fun `live sharing publishes check in and check out events for runner`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
            currentMinutesOfDay = { 23 * 60 + 20 },
        )

        viewModel.onRunCodeChanged("abc-123!")
        viewModel.onLiveSharingToggle(true)
        viewModel.onCalculateClick()
        viewModel.onCheckInOpen()
        viewModel.onCheckInNow()
        viewModel.onCheckInSave()
        viewModel.onCheckOutNowSave()

        assertEquals("ABC123", viewModel.uiState.value.settings.liveRunLink.runCode)
        assertEquals(listOf(CheckEventType.CheckIn, CheckEventType.CheckOut), liveRepository.events.map { it.type })
        assertEquals("ABC123", liveRepository.events.first().runCode)
    }

    @Test
    fun `supporter subscription applies remote check in events`() {
        val liveRepository = FakeLiveRaceRepository()
        val viewModel = SupporterViewModel(
            sessionRepository = FakeSessionRepository(),
            liveRaceRepository = liveRepository,
        )

        viewModel.onLiveRoleSelected(LiveRole.Supporter)
        viewModel.onRunCodeChanged("RUN42")
        viewModel.onLiveSharingToggle(true)

        liveRepository.emit(
            "RUN42",
            listOf(
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
        )

        assertEquals(1, viewModel.uiState.value.checkIns.size)
        assertEquals(CheckIn(stationSection = 3, actualArrivalMinutes = 285), viewModel.uiState.value.checkIns.first())
        assertEquals("Check-in Test um 02:45", viewModel.uiState.value.settings.lastLiveEventText)
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
    val events = mutableListOf<CheckEvent>()
    private val listeners = mutableMapOf<String, (List<CheckEvent>) -> Unit>()

    override fun publish(event: CheckEvent) {
        events += event
    }

    override fun subscribe(runCode: String, onEventsChanged: (List<CheckEvent>) -> Unit): LiveRaceSubscription {
        listeners[runCode] = onEventsChanged
        return object : LiveRaceSubscription {
            override fun close() {
                listeners.remove(runCode)
            }
        }
    }

    fun emit(runCode: String, events: List<CheckEvent>) {
        listeners[runCode]?.invoke(events)
    }
}
