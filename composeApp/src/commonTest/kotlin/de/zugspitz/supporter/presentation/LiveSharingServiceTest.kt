package de.zugspitz.supporter.presentation

import de.zugspitz.supporter.SupporterPushNotifications
import de.zugspitz.supporter.data.CheckEvent
import de.zugspitz.supporter.data.LiveRaceRepository
import de.zugspitz.supporter.data.LiveRaceSubscription
import de.zugspitz.supporter.data.LiveRole
import de.zugspitz.supporter.data.LiveRunInfo
import de.zugspitz.supporter.data.LiveRunLink
import de.zugspitz.supporter.data.LiveRunSnapshot
import de.zugspitz.supporter.data.LiveRunnerLocation
import de.zugspitz.supporter.data.PushPlatform
import de.zugspitz.supporter.data.RaceEstimate
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveSharingServiceTest {
    @Test
    fun `create run is ignored when live sharing is disabled`() = runBlocking {
        val repository = FakeLiveSharingRepository()
        val service = LiveSharingService(repository = repository, enabled = false)

        val runInfo = service.createRun(RaceEstimate())

        assertEquals(null, runInfo)
        assertEquals(0, repository.createRunCalls)
    }

    @Test
    fun `publishes runner data only for publishable runner link`() {
        val repository = FakeLiveSharingRepository()
        val service = LiveSharingService(repository = repository, enabled = true)

        service.publishRunInfo(
            link = LiveRunLink(role = LiveRole.Runner, runCode = "RUN123", runnerName = "Andre", isEnabled = true),
            estimate = RaceEstimate(),
        )
        service.deleteRun(LiveRunLink(role = LiveRole.Runner, runCode = "RUN123", isEnabled = true))
        service.publishRunInfo(
            link = LiveRunLink(role = LiveRole.Supporter, runCode = "RUN123", isEnabled = true),
            estimate = RaceEstimate(),
        )

        assertEquals(listOf("RUN123"), repository.runInfos.map { it.runCode })
        assertEquals("Andre", repository.runInfos.single().runnerName)
        assertEquals(listOf("RUN123"), repository.deletedRuns)
    }

    @Test
    fun `sync subscription switches active run code and closes previous subscription`() {
        val repository = FakeLiveSharingRepository()
        val service = LiveSharingService(repository = repository, enabled = true)
        val receivedCodes = mutableListOf<String>()

        service.syncSubscription(
            link = LiveRunLink(role = LiveRole.Supporter, runCode = "FIRST", isEnabled = true),
            onSnapshot = { runCode, _ -> receivedCodes += runCode },
        )
        service.syncSubscription(
            link = LiveRunLink(role = LiveRole.Supporter, runCode = "SECOND", isEnabled = true),
            onSnapshot = { runCode, _ -> receivedCodes += runCode },
        )

        repository.emit("FIRST", LiveRunSnapshot(info = null))
        repository.emit("SECOND", LiveRunSnapshot(info = null))

        assertEquals(listOf("FIRST", "SECOND"), repository.subscribedCodes)
        assertEquals(listOf("FIRST"), repository.closedCodes)
        assertEquals(listOf("SECOND"), receivedCodes)
    }

    @Test
    fun `sync supporter push registration registers new code and unregisters previous code`() {
        val repository = FakeLiveSharingRepository()
        val service = LiveSharingService(
            repository = repository,
            pushNotifications = FakeLiveSharingPushNotifications("token-1"),
            enabled = true,
        )
        var currentLink = LiveRunLink(role = LiveRole.Supporter, runCode = "FIRST", isEnabled = true)

        service.syncSupporterPushRegistration(
            link = currentLink,
            currentLink = { currentLink },
        )
        currentLink = LiveRunLink(role = LiveRole.Supporter, runCode = "SECOND", isEnabled = true)
        service.syncSupporterPushRegistration(
            link = currentLink,
            currentLink = { currentLink },
        )

        assertEquals(
            listOf("FIRST:ios:token-1:true", "FIRST:ios:token-1:false", "SECOND:ios:token-1:true"),
            repository.pushTokens,
        )
    }

    @Test
    fun `sync supporter push registration ignores stale async token callback`() {
        val repository = FakeLiveSharingRepository()
        val pushNotifications = FakeLiveSharingPushNotifications("token-1", deliverImmediately = false)
        val service = LiveSharingService(
            repository = repository,
            pushNotifications = pushNotifications,
            enabled = true,
        )
        var currentLink = LiveRunLink(role = LiveRole.Supporter, runCode = "FIRST", isEnabled = true)

        service.syncSupporterPushRegistration(
            link = currentLink,
            currentLink = { currentLink },
        )
        currentLink = LiveRunLink(role = LiveRole.Supporter, runCode = "SECOND", isEnabled = true)
        pushNotifications.deliverToken()

        assertTrue(repository.pushTokens.isEmpty())
    }
}

private class FakeLiveSharingRepository : LiveRaceRepository {
    var createRunCalls = 0
    val runInfos = mutableListOf<LiveRunInfo>()
    val deletedRuns = mutableListOf<String>()
    val subscribedCodes = mutableListOf<String>()
    val closedCodes = mutableListOf<String>()
    val pushTokens = mutableListOf<String>()
    private val listeners = mutableMapOf<String, (LiveRunSnapshot) -> Unit>()

    override suspend fun createRun(estimate: RaceEstimate): LiveRunInfo {
        createRunCalls += 1
        return LiveRunInfo(runCode = "RUN123", estimate = estimate, createdAtEpochMillis = 0L)
    }

    override fun deleteRun(runCode: String) {
        deletedRuns += runCode
    }

    override fun publishRunInfo(info: LiveRunInfo) {
        runInfos += info
    }

    override fun publish(event: CheckEvent) = Unit

    override fun publishRunnerLocation(location: LiveRunnerLocation) = Unit

    override fun registerSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        pushTokens += "$runCode:${platform.databaseValue()}:$deviceToken:true"
    }

    override fun unregisterSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        pushTokens += "$runCode:${platform.databaseValue()}:$deviceToken:false"
    }

    override fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription {
        subscribedCodes += runCode
        listeners[runCode] = onSnapshotChanged
        return object : LiveRaceSubscription {
            override fun close() {
                closedCodes += runCode
                listeners.remove(runCode)
            }
        }
    }

    fun emit(runCode: String, snapshot: LiveRunSnapshot) {
        listeners[runCode]?.invoke(snapshot)
    }
}

private class FakeLiveSharingPushNotifications(
    private val token: String?,
    private val deliverImmediately: Boolean = true,
) : SupporterPushNotifications {
    private var pendingCallback: ((String) -> Unit)? = null

    override fun currentToken(): String? = token

    override fun requestToken(onToken: (String) -> Unit) {
        if (deliverImmediately) {
            token?.let(onToken)
        } else {
            pendingCallback = onToken
        }
    }

    fun deliverToken() {
        val callback = pendingCallback
        pendingCallback = null
        if (callback != null) {
            token?.let(callback)
        }
    }
}

private fun PushPlatform.databaseValue(): String = when (this) {
    PushPlatform.Ios -> "ios"
}
