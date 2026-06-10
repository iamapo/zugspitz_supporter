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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

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
    private val postgrest = supabase.pluginManager.getPlugin(Postgrest)

    private val authMutex = Mutex()
    private val pendingMutex = Mutex()
    private val pendingWrites = linkedMapOf<String, PendingWrite>()
    private var senderJob: Job? = null

    override suspend fun createRun(estimate: RaceEstimate): LiveRunInfo? {
        ensureSignedIn()
        val createdAtEpochMillis = Clock.System.now().toEpochMilliseconds()
        LiveSharingLogger.d("Requesting reserved run code from Supabase RPC")
        return postgrest.rpc(
            CREATE_LIVE_RUN_RPC,
            buildJsonObject {
                put("p_estimate", Json.encodeToJsonElement(RaceEstimate.serializer(), estimate))
                put("p_created_at_epoch_millis", JsonPrimitive(createdAtEpochMillis))
            }
        ).decodeAs<SupabaseRunRow>()
            .toModel()
            .also { info ->
                LiveSharingLogger.d("Supabase RPC created run code=${info.runCode}")
            }
    }

    override fun publishRunInfo(info: LiveRunInfo) {
        enqueue(
            key = "run-info:${info.runCode}",
            write = PendingWrite.RunInfo(info),
        )
    }

    override fun deleteRun(runCode: String) {
        enqueue(
            key = "delete-run:$runCode",
            write = PendingWrite.DeleteRun(runCode),
        )
    }

    override fun publish(event: CheckEvent) {
        enqueue(
            key = "event:${event.id}",
            write = PendingWrite.Event(event),
        )
    }

    override fun publishRunnerLocation(location: LiveRunnerLocation) {
        enqueue(
            key = "runner-location:${location.runCode}",
            write = PendingWrite.RunnerLocation(location),
        )
    }

    override fun registerSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        enqueue(
            key = "push-token:$runCode:${platform.name}:$deviceToken",
            write = PendingWrite.PushToken(
                runCode = runCode,
                platform = platform,
                deviceToken = deviceToken,
                isEnabled = true,
            ),
        )
    }

    override fun unregisterSupporterPushToken(runCode: String, platform: PushPlatform, deviceToken: String) {
        enqueue(
            key = "push-token:$runCode:${platform.name}:$deviceToken",
            write = PendingWrite.PushToken(
                runCode = runCode,
                platform = platform,
                deviceToken = deviceToken,
                isEnabled = false,
            ),
        )
    }

    override fun subscribe(runCode: String, onSnapshotChanged: (LiveRunSnapshot) -> Unit): LiveRaceSubscription {
        LiveSharingLogger.d("Subscribing to live updates for runCode=$runCode")
        val channel = supabase.channel("live:$runCode")
        var latestInfo: LiveRunInfo? = null
        var latestEvents: List<CheckEvent> = emptyList()
        var latestRunnerLocation: LiveRunnerLocation? = null

        fun emitSnapshot() {
            onSnapshotChanged(
                LiveRunSnapshot(
                    info = latestInfo,
                    events = latestEvents,
                    runnerLocation = latestRunnerLocation,
                ),
            )
        }

        val bootstrapJob = scope.launch {
            runCatching {
                ensureSignedIn()
                latestInfo = fetchRunInfo(runCode)
                latestEvents = fetchEvents(runCode)
                latestRunnerLocation = fetchRunnerLocation(runCode)
                LiveSharingLogger.d(
                    "Initial snapshot loaded for runCode=$runCode info=${latestInfo != null} " +
                        "events=${latestEvents.size} location=${latestRunnerLocation != null}",
                )
                emitSnapshot()
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
                    latestInfo = fetchRunInfo(runCode)
                    LiveSharingLogger.d("Realtime run sync runCode=$runCode action=${action::class.simpleName} info=${latestInfo != null}")
                    emitSnapshot()
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
                    latestEvents = fetchEvents(runCode)
                    LiveSharingLogger.d("Realtime event sync runCode=$runCode action=${action::class.simpleName} events=${latestEvents.size}")
                    emitSnapshot()
                }
            }.onFailure { throwable ->
                logError("Realtime event subscription failed for runCode=$runCode", throwable)
            }
        }

        val locationJob = scope.launch {
            runCatching {
                channel.postgresChangeFlow<PostgresAction>(schema = PUBLIC_SCHEMA) {
                    table = RUNNER_LOCATIONS_TABLE
                    filter(RUNNER_LOCATIONS_RUN_CODE_COLUMN, FilterOperator.EQ, runCode)
                }.collectLatest { action ->
                    latestRunnerLocation = fetchRunnerLocation(runCode)
                    LiveSharingLogger.d(
                        "Realtime runner location sync runCode=$runCode " +
                            "action=${action::class.simpleName} location=${latestRunnerLocation != null}",
                    )
                    emitSnapshot()
                }
            }.onFailure { throwable ->
                logError("Realtime runner location subscription failed for runCode=$runCode", throwable)
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
                listOf(bootstrapJob, runInfoJob, eventsJob, locationJob, subscribeJob).forEach { it.cancel() }
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
        scope.launch {
            pendingMutex.withLock {
                if (write is PendingWrite.DeleteRun) {
                    pendingWrites.entries.removeAll { (_, pendingWrite) ->
                        pendingWrite.runCode == write.runCode
                    }
                }
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
                    is PendingWrite.RunnerLocation -> {
                        LiveSharingLogger.d("Publishing runner location runCode=${write.location.runCode}")
                        supabase
                            .from(RUNNER_LOCATIONS_TABLE)
                            .upsert(write.location.toRow()) {
                                onConflict = RUNNER_LOCATIONS_RUN_CODE_COLUMN
                            }
                    }
                    is PendingWrite.PushToken -> {
                        val now = Clock.System.now().toEpochMilliseconds()
                        LiveSharingLogger.d(
                            "Saving supporter push token runCode=${write.runCode} " +
                                "platform=${write.platform.databaseValue()} enabled=${write.isEnabled}",
                        )
                        postgrest.rpc(
                            UPSERT_SUPPORTER_PUSH_TOKEN_RPC,
                            buildJsonObject {
                                put("p_run_code", write.runCode)
                                put("p_platform", write.platform.databaseValue())
                                put("p_device_token", write.deviceToken)
                                put("p_is_enabled", write.isEnabled)
                                put("p_now_epoch_millis", JsonPrimitive(now))
                            },
                        )
                    }
                    is PendingWrite.DeleteRun -> {
                        LiveSharingLogger.d("Deleting live run runCode=${write.runCode}")
                        supabase
                            .from(RUNS_TABLE)
                            .delete {
                                filter {
                                    eq(RUNS_RUN_CODE_COLUMN, write.runCode)
                                }
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

    private suspend fun fetchRunnerLocation(runCode: String): LiveRunnerLocation? =
        supabase
            .from(RUNNER_LOCATIONS_TABLE)
            .select {
                filter {
                    eq(RUNNER_LOCATIONS_RUN_CODE_COLUMN, runCode)
                }
            }
            .decodeList<SupabaseRunnerLocationRow>()
            .firstOrNull()
            ?.toModel()

    private fun logError(message: String, throwable: Throwable) {
        LiveSharingLogger.e(message, throwable)
        onError(throwable)
    }

    private companion object {
        const val CREATE_LIVE_RUN_RPC = "create_live_run"
        const val UPSERT_SUPPORTER_PUSH_TOKEN_RPC = "upsert_supporter_push_token"
        const val PUBLIC_SCHEMA = "public"
        const val RUNS_TABLE = "runs"
        const val EVENTS_TABLE = "events"
        const val RUNNER_LOCATIONS_TABLE = "runner_locations"
        const val RUNS_RUN_CODE_COLUMN = "run_code"
        const val EVENTS_ID_COLUMN = "id"
        const val EVENTS_RUN_CODE_COLUMN = "run_code"
        const val EVENTS_CREATED_AT_COLUMN = "created_at_epoch_millis"
        const val RUNNER_LOCATIONS_RUN_CODE_COLUMN = "run_code"
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
    @SerialName("runner_name")
    val runnerName: String = "",
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
    @SerialName("runner_location")
    val runnerLocation: LiveRunnerLocation? = null,
)

@Serializable
private data class SupabaseRunnerLocationRow(
    @SerialName("run_code")
    val runCode: String,
    @SerialName("race_id")
    val raceId: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("distance_km")
    val distanceKm: Double,
    @SerialName("elevation_meters")
    val elevationMeters: Double,
    @SerialName("distance_from_route_meters")
    val distanceFromRouteMeters: Double,
    @SerialName("accuracy_meters")
    val accuracyMeters: Double? = null,
    @SerialName("is_on_route")
    val isOnRoute: Boolean,
    @SerialName("updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
)

private fun LiveRunInfo.toRow() = SupabaseRunRow(
    runCode = runCode,
    estimate = estimate,
    createdAtEpochMillis = createdAtEpochMillis,
    runnerName = runnerName,
)

private fun SupabaseRunRow.toModel() = LiveRunInfo(
    runCode = runCode,
    estimate = estimate,
    createdAtEpochMillis = createdAtEpochMillis,
    runnerName = runnerName,
)

private fun CheckEvent.toRow() = SupabaseEventRow(
    id = id,
    runCode = runCode,
    stationSection = stationSection,
    stationName = stationName,
    type = type,
    raceMinutes = raceMinutes,
    createdAtEpochMillis = createdAtEpochMillis,
    runnerLocation = runnerLocation,
)

private fun SupabaseEventRow.toModel() = CheckEvent(
    id = id,
    runCode = runCode,
    stationSection = stationSection,
    stationName = stationName,
    type = type,
    raceMinutes = raceMinutes,
    createdAtEpochMillis = createdAtEpochMillis,
    runnerLocation = runnerLocation,
)

private fun LiveRunnerLocation.toRow() = SupabaseRunnerLocationRow(
    runCode = runCode,
    raceId = raceId,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    elevationMeters = elevationMeters,
    distanceFromRouteMeters = distanceFromRouteMeters,
    accuracyMeters = accuracyMeters,
    isOnRoute = isOnRoute,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun SupabaseRunnerLocationRow.toModel() = LiveRunnerLocation(
    runCode = runCode,
    raceId = raceId,
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    elevationMeters = elevationMeters,
    distanceFromRouteMeters = distanceFromRouteMeters,
    accuracyMeters = accuracyMeters,
    isOnRoute = isOnRoute,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private sealed interface PendingWrite {
    val runCode: String

    data class RunInfo(val info: LiveRunInfo) : PendingWrite {
        override val runCode: String
            get() = info.runCode
    }

    data class DeleteRun(override val runCode: String) : PendingWrite

    data class Event(val event: CheckEvent) : PendingWrite {
        override val runCode: String
            get() = event.runCode
    }

    data class RunnerLocation(val location: LiveRunnerLocation) : PendingWrite {
        override val runCode: String
            get() = location.runCode
    }

    data class PushToken(
        override val runCode: String,
        val platform: PushPlatform,
        val deviceToken: String,
        val isEnabled: Boolean,
    ) : PendingWrite
}

private fun PendingWrite.logLabel(): String = when (this) {
    is PendingWrite.RunInfo -> "run-info:${info.runCode}"
    is PendingWrite.DeleteRun -> "delete-run:$runCode"
    is PendingWrite.Event -> "event:${event.id}"
    is PendingWrite.RunnerLocation -> "runner-location:$runCode"
    is PendingWrite.PushToken -> "push-token:$runCode:${platform.databaseValue()}:$isEnabled"
}

private fun PushPlatform.databaseValue(): String = when (this) {
    PushPlatform.Ios -> "ios"
}
