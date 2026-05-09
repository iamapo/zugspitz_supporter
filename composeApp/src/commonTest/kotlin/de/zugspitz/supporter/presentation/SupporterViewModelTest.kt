package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.SavedTab
import de.zugspitz.supporter.data.SessionRepository
import de.zugspitz.supporter.data.TargetTimeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupporterViewModelTest {
    @Test
    fun `first start defaults to setup and Z1`() {
        val repository = FakeSessionRepository()
        val viewModel = SupporterViewModel(sessionRepository = repository)

        val state = viewModel.uiState.value
        assertEquals(AppTab.Setup, state.tab)
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
    fun `reset clears state and routes to setup`() {
        val repository = FakeSessionRepository().apply {
            save(AppSessionState(tab = SavedTab.Vp, selectedIndex = 5))
        }
        val viewModel = SupporterViewModel(sessionRepository = repository)

        viewModel.onResetAllData()
        val state = viewModel.uiState.value

        assertEquals(AppTab.Setup, state.tab)
        assertEquals(0, state.vp.selectedIndex)
        assertTrue(state.checkIns.isEmpty())
        assertEquals(AppTab.Setup, SupporterViewModel(sessionRepository = repository).uiState.value.tab)
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

        viewModel.onCheckOutOpen()
        viewModel.onCheckInNow()
        viewModel.onCheckInSave()

        val saved = repository.load().checkIns.first { it.stationSection == 3 }
        assertEquals(281, saved.actualArrivalMinutes)
        assertEquals(105, saved.actualDepartureMinutes)
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
