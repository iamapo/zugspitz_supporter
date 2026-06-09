package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.LiveSharingLogger
import de.zugspitz.supporter.NoOpSupporterPushNotifications
import de.zugspitz.supporter.SupporterPushNotifications
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRaceSubscription
import de.zugspitz.supporter.data.LiveRunInfo
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.NoOpLiveRaceRepository
import de.zugspitz.supporter.data.PushPlatform
import de.zugspitz.supporter.data.RaceEstimate
import kotlin.time.Clock

class LiveSharingService(
    private val repository: LiveRaceRepository = NoOpLiveRaceRepository(),
    private val pushNotifications: SupporterPushNotifications = NoOpSupporterPushNotifications,
    private val enabled: Boolean = false,
) {
    private var liveSubscription: LiveRaceSubscription? = null
    private var liveSubscriptionCode: String? = null
    private var registeredPushRunCode: String? = null

    suspend fun createRun(estimate: RaceEstimate): LiveRunInfo? {
        if (!enabled) return null
        return runCatching {
            repository.createRun(estimate)
        }.onSuccess { liveRunInfo ->
            if (liveRunInfo != null) {
                LiveSharingLogger.d("Run code created runCode=${liveRunInfo.runCode}")
            }
        }.onFailure { throwable ->
            LiveSharingLogger.e("Creating run code failed", throwable)
        }.getOrNull()
    }

    fun publishRunInfo(link: LiveRunLink, estimate: RaceEstimate) {
        if (!enabled || !link.canPublish) return
        repository.publishRunInfo(
            LiveRunInfo(
                runCode = link.runCode,
                estimate = estimate,
                createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                runnerName = link.runnerName,
            ),
        )
    }

    fun publishEvent(event: CheckEvent) {
        if (!enabled) return
        repository.publish(event)
    }

    fun publishRunnerLocation(location: LiveRunnerLocation) {
        if (!enabled) return
        repository.publishRunnerLocation(location)
    }

    fun deleteRun(link: LiveRunLink) {
        if (!enabled || !link.canPublish) return
        repository.deleteRun(link.runCode)
    }

    fun syncSubscription(
        link: LiveRunLink,
        onSnapshot: (runCode: String, snapshot: LiveRunSnapshot) -> Unit,
    ) {
        if (!enabled) return
        val requestedCode = link.runCode.takeIf { link.canSubscribe }
        if (requestedCode == liveSubscriptionCode) return

        liveSubscription?.close()
        liveSubscription = null
        liveSubscriptionCode = null

        if (requestedCode == null) return

        liveSubscriptionCode = requestedCode
        liveSubscription = repository.subscribe(requestedCode) { remoteSnapshot ->
            onSnapshot(requestedCode, remoteSnapshot)
        }
    }

    fun syncSupporterPushRegistration(
        link: LiveRunLink,
        currentLink: () -> LiveRunLink,
    ) {
        if (!enabled) return

        val requestedCode = link.runCode.takeIf { link.canSubscribe }
        val previousCode = registeredPushRunCode
        if (previousCode != null && previousCode != requestedCode) {
            pushNotifications.currentToken()?.let { token ->
                repository.unregisterSupporterPushToken(
                    runCode = previousCode,
                    platform = PushPlatform.Ios,
                    deviceToken = token,
                )
            }
            registeredPushRunCode = null
        }

        if (requestedCode == null || requestedCode == registeredPushRunCode) return

        pushNotifications.requestToken { token ->
            val latestLink = currentLink()
            if (latestLink.runCode != requestedCode || !latestLink.canSubscribe) return@requestToken
            if (requestedCode == registeredPushRunCode) return@requestToken
            repository.registerSupporterPushToken(
                runCode = requestedCode,
                platform = PushPlatform.Ios,
                deviceToken = token,
            )
            registeredPushRunCode = requestedCode
        }
    }
}
