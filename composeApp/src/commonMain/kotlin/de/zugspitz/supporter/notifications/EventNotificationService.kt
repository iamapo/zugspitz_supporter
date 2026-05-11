package de.zugspitz.supporter.notifications

import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.CheckEventType
import de.zugspitz.supporter.data.RaceEstimate
import de.zugspitz.supporter.data.formatRaceTime

interface EventNotificationService {
    fun notifyRemoteEvent(event: CheckEvent, estimate: RaceEstimate)
}

object NoOpEventNotificationService : EventNotificationService {
    override fun notifyRemoteEvent(event: CheckEvent, estimate: RaceEstimate) = Unit
}

fun CheckEvent.notificationTitle(): String = when (type) {
    CheckEventType.CheckIn -> "Neuer Check-in"
    CheckEventType.CheckOut -> "Neuer Check-out"
}

fun CheckEvent.notificationBody(estimate: RaceEstimate): String {
    val stationLabel = stationName.removePrefix("Z$stationSection ")
    return "$stationLabel um ${formatRaceTime(estimate.startTimeMinutes + raceMinutes)}"
}
