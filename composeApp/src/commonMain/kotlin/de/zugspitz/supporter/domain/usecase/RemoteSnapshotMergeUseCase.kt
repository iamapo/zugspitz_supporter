package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.components.AppTab
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.CheckIn
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.RaceDefinitions
import de.zugspitz.supporter.data.RaceEstimate

class RemoteSnapshotMergeUseCase {
    operator fun invoke(
        currentTab: AppTab,
        currentEstimate: RaceEstimate,
        currentEvents: List<CheckEvent>,
        currentSelectedIndex: Int,
        currentStationSection: Int?,
        currentLastStationIndex: Int,
        remoteSnapshot: LiveRunSnapshot,
    ): RemoteSnapshotMergeResult {
        val activeEstimate = remoteSnapshot.info?.estimate ?: currentEstimate
        val mergedEvents = (currentEvents + remoteSnapshot.events)
            .distinctBy { it.id }
            .sortedBy { it.createdAtEpochMillis }
        val remoteCheckIns = mergedEvents.toCheckIns()
        val raceStations = RaceDefinitions.byId(activeEstimate.raceId).stations
        val firstOpenIndex = raceStations
            .indexOfFirst { station ->
                remoteCheckIns.none {
                    it.stationSection == station.section && it.actualDepartureMinutes != null
                }
            }
            .let { index -> if (index >= 0) index else raceStations.lastIndex }
        val shouldAdvanceAfterCheckout = currentStationSection != null &&
            remoteCheckIns.any {
                it.stationSection == currentStationSection && it.actualDepartureMinutes != null
            } &&
            currentSelectedIndex < currentLastStationIndex

        return RemoteSnapshotMergeResult(
            tab = if (remoteSnapshot.info != null && currentTab == AppTab.SupportCode) AppTab.Vp else currentTab,
            estimate = activeEstimate,
            checkIns = remoteCheckIns,
            checkEvents = mergedEvents,
            runnerLocation = remoteSnapshot.runnerLocation,
            selectedIndex = when {
                currentTab == AppTab.SupportCode -> firstOpenIndex
                shouldAdvanceAfterCheckout -> currentSelectedIndex + 1
                else -> currentSelectedIndex
            },
        )
    }
}

data class RemoteSnapshotMergeResult(
    val tab: AppTab,
    val estimate: RaceEstimate,
    val checkIns: List<CheckIn>,
    val checkEvents: List<CheckEvent>,
    val runnerLocation: LiveRunnerLocation?,
    val selectedIndex: Int,
)

private fun List<CheckEvent>.toCheckIns(): List<CheckIn> =
    groupBy { it.stationSection }
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
