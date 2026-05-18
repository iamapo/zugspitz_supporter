package de.zugspitz.supporter.data

import de.zugspitz.supporter.LiveSharingLogger
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseLiveRaceRepository(
    config: LiveSharingBackendConfig,
    private val onError: (Throwable) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            onError(throwable)
        },
    ),
) : LiveRaceRepository {
    private val supabase = createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.publishableKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
    }

    private val authMutex = Mutex()
    private val pendingMutex = Mutex()
    private val pendingWrites = linkedMapOf<String, PendingWrite>()
    private var senderJob: Job? = null

    override fun publishRunInfo(info: LiveRunInfo) {
        enqueue(
            key = "run-info:${info.runCode}",
            write = PendingWrite.RunInfo(info),
        )
    }

    override fun publish(event: CheckEvent) {
        enqueue(
            key = "event:${event.id}",
            write = PendingWrite.Event(event),
        )
    }

    override fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription {
        LiveSharingLogger.d("Subscribing to live updates for runCode=$runCode")
        val channel = supabase.channel("live:$runCode")
        var latestInfo: LiveRunInfo? = null
        var latestEvents: List<CheckEvent> = emptyList()

        fun emitSnapshot(reason: String) {
            LiveSharingLogger.d(
                "Emitting snapshot for runCode=$runCode reason=$reason info=${latestInfo != null} events=${latestEvents.size}"
            )
            onSnapshotChanged(LiveRunSnapshot(info = latestInfo, events = latestEvents))
        }

        val bootstrapJob = scope.launch {
            runCatching {
                ensureSignedIn()
                latestInfo = fetchRunInfo(runCode)
                latestEvents = fetchEvents(runCode)
                emitSnapshot("initial-load")
            }.onFailure { throwable ->
                logError("Initial snapshot load failed for runCode=$runCode", throwable)
            }
        }

        val runInfoJob = scope.launch {
            runCatching {
                channel.postgresChangeFlow<PostgresAction>(schema = PUBLIC_SCHEMA) {
                    table = RUNS_TABLE
                    filter(RUNS_RUN_CODE_COLUMN, FilterOperator.EQ, runCode)
                }.collectLatest { action ->
                    LiveSharingLogger.d("Realtime run change received for runCode=$runCode action=${action::class.simpleName}")
                    latestInfo = fetchRunInfo(runCode)
                    emitSnapshot("run-change")
                }
            }.onFailure { throwable ->
                logError("Realtime run subscription failed for runCode=$runCode", throwable)
            }
        }

        val eventsJob = scope.launch {
            runCatching {
                channel.postgresChangeFlow<PostgresAction>(schema = PUBLIC_SCHEMA) {
                    table = EVENTS_TABLE
                    filter(EVENTS_RUN_CODE_COLUMN, FilterOperator.EQ, runCode)
                }.collectLatest { action ->
                    LiveSharingLogger.d("Realtime event change received for runCode=$runCode action=${action::class.simpleName}")
                    latestEvents = fetchEvents(runCode)
                    emitSnapshot("event-change")
                }
            }.onFailure { throwable ->
                logError("Realtime event subscription failed for runCode=$runCode", throwable)
            }
        }

        val subscribeJob = scope.launch {
            runCatching {
                ensureSignedIn()
                channel.subscribe()
                LiveSharingLogger.d("Realtime channel subscribed for runCode=$runCode topic=${channel.topic}")
            }.onFailure { throwable ->
                logError("Realtime channel subscribe failed for runCode=$runCode", throwable)
            }
        }

        return object : LiveRaceSubscription {
            override fun close() {
                LiveSharingLogger.d("Closing live subscription for runCode=$runCode")
                listOf(bootstrapJob, runInfoJob, eventsJob, subscribeJob).forEach { it.cancel() }
                scope.launch {
                    runCatching {
                        channel.unsubscribe()
                        LiveSharingLogger.d("Realtime channel unsubscribed for runCode=$runCode")
                    }.onFailure { throwable ->
                        logError("Realtime channel unsubscribe failed for runCode=$runCode", throwable)
                    }
                }
            }
        }
    }

    fun close() {
        LiveSharingLogger.d("Closing Supabase live race repository")
        scope.cancel()
    }

    private fun enqueue(key: String, write: PendingWrite) {
        LiveSharingLogger.d("Queueing live write key=$key")
        scope.launch {
            pendingMutex.withLock {
                pendingWrites[key] = write
                if (senderJob?.isActive != true) {
                    senderJob = scope.launch { flushQueueLoop() }
                }
            }
        }
    }

    private suspend fun flushQueueLoop() {
        while (true) {
            val next = pendingMutex.withLock {
                pendingWrites.entries.firstOrNull()?.toPair()
            } ?: return

            val (key, write) = next
            val sent = sendWithRetry(write)
            if (sent) {
                pendingMutex.withLock {
                    pendingWrites.remove(key)
                }
            }
        }
    }

    private suspend fun sendWithRetry(write: PendingWrite): Boolean {
        var backoffMs = INITIAL_RETRY_DELAY_MS
        while (true) {
            val result = runCatching {
                ensureSignedIn()
                when (write) {
                    is PendingWrite.RunInfo -> {
                        LiveSharingLogger.d("Publishing run info for runCode=${write.info.runCode}")
                        supabase
                            .from(RUNS_TABLE)
                            .upsert(write.info.toRow()) {
                                onConflict = RUNS_RUN_CODE_COLUMN
                            }
                    }
                    is PendingWrite.Event -> {
                        LiveSharingLogger.d("Publishing event id=${write.event.id} runCode=${write.event.runCode}")
                        supabase
                            .from(EVENTS_TABLE)
                            .upsert(write.event.toRow()) {
                                onConflict = EVENTS_ID_COLUMN
                            }
                    }
                }
            }
            if (result.isSuccess) {
                LiveSharingLogger.d("Live write succeeded for ${write.logLabel()}")
                return true
            }

            val throwable = result.exceptionOrNull() ?: IllegalStateException("Unknown publish error")
            logError("Live write failed for ${write.logLabel()}, retrying in ${backoffMs}ms", throwable)
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }
    }

    private suspend fun ensureSignedIn() {
        authMutex.withLock {
            if (supabase.auth.currentUserOrNull() == null) {
                LiveSharingLogger.d("Signing in anonymously with Supabase")
                supabase.auth.signInAnonymously()
                LiveSharingLogger.d("Anonymous Supabase sign-in succeeded")
            }
        }
    }

    private suspend fun fetchRunInfo(runCode: String): LiveRunInfo? =
        supabase
            .from(RUNS_TABLE)
            .select {
                filter {
                    eq(RUNS_RUN_CODE_COLUMN, runCode)
                }
            }
            .decodeList<SupabaseRunRow>()
            .firstOrNull()
            ?.toModel()
            .also { info ->
                LiveSharingLogger.d("Fetched run info for runCode=$runCode found=${info != null}")
            }

    private suspend fun fetchEvents(runCode: String): List<CheckEvent> =
        supabase
            .from(EVENTS_TABLE)
            .select {
                filter {
                    eq(EVENTS_RUN_CODE_COLUMN, runCode)
                }
                order(column = EVENTS_CREATED_AT_COLUMN, order = Order.ASCENDING)
            }
            .decodeList<SupabaseEventRow>()
            .map(SupabaseEventRow::toModel)
            .also { events ->
                LiveSharingLogger.d("Fetched ${events.size} events for runCode=$runCode")
            }

    private fun logError(message: String, throwable: Throwable) {
        LiveSharingLogger.e(message, throwable)
        onError(throwable)
    }

    private companion object {
        const val PUBLIC_SCHEMA = "public"
        const val RUNS_TABLE = "runs"
        const val EVENTS_TABLE = "events"
        const val RUNS_RUN_CODE_COLUMN = "run_code"
        const val EVENTS_ID_COLUMN = "id"
        const val EVENTS_RUN_CODE_COLUMN = "run_code"
        const val EVENTS_CREATED_AT_COLUMN = "created_at_epoch_millis"
        const val INITIAL_RETRY_DELAY_MS = 2_000L
        const val MAX_RETRY_DELAY_MS = 60_000L
    }
}

@Serializable
private data class SupabaseRunRow(
    @SerialName("run_code")
    val runCode: String,
    val estimate: RaceEstimate,
    @SerialName("created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)

@Serializable
private data class SupabaseEventRow(
    val id: String,
    @SerialName("run_code")
    val runCode: String,
    @SerialName("station_section")
    val stationSection: Int,
    @SerialName("station_name")
    val stationName: String,
    val type: CheckEventType,
    @SerialName("race_minutes")
    val raceMinutes: Int,
    @SerialName("created_at_epoch_millis")
    val createdAtEpochMillis: Long,
)

private fun LiveRunInfo.toRow() = SupabaseRunRow(
    runCode = runCode,
    estimate = estimate,
    createdAtEpochMillis = createdAtEpochMillis,
)

private fun SupabaseRunRow.toModel() = LiveRunInfo(
    runCode = runCode,
    estimate = estimate,
    createdAtEpochMillis = createdAtEpochMillis,
)

private fun CheckEvent.toRow() = SupabaseEventRow(
    id = id,
    runCode = runCode,
    stationSection = stationSection,
    stationName = stationName,
    type = type,
    raceMinutes = raceMinutes,
    createdAtEpochMillis = createdAtEpochMillis,
)

private fun SupabaseEventRow.toModel() = CheckEvent(
    id = id,
    runCode = runCode,
    stationSection = stationSection,
    stationName = stationName,
    type = type,
    raceMinutes = raceMinutes,
    createdAtEpochMillis = createdAtEpochMillis,
)

private sealed interface PendingWrite {
    data class RunInfo(val info: LiveRunInfo) : PendingWrite
    data class Event(val event: CheckEvent) : PendingWrite
}

private fun PendingWrite.logLabel(): String = when (this) {
    is PendingWrite.RunInfo -> "run-info:${info.runCode}"
    is PendingWrite.Event -> "event:${event.id}"
}
