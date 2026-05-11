package de.zugspitz.supporter.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KmpFirebaseLiveRaceRepository(
    private val onError: (Throwable) -> Unit = {},
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            onError(throwable)
        },
    ),
) : LiveRaceRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
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
        var latestInfo: LiveRunInfo? = null
        var latestEvents: List<CheckEvent> = emptyList()

        fun emitSnapshot() {
            onSnapshotChanged(LiveRunSnapshot(info = latestInfo, events = latestEvents))
        }

        val infoJob = scope.launch {
            runCatching {
                ensureSignedIn()
                firestore
                    .collection(RUNS_COLLECTION)
                    .document(runCode)
                    .snapshots
                    .collectLatest { snapshot ->
                        latestInfo = if (snapshot.exists) snapshot.data<LiveRunInfo>() else null
                        emitSnapshot()
                    }
            }.onFailure(onError)
        }

        val eventsJob = scope.launch {
            runCatching {
                ensureSignedIn()
                firestore
                    .collection(RUNS_COLLECTION)
                    .document(runCode)
                    .collection(EVENTS_COLLECTION)
                    .orderBy(FIELD_CREATED_AT_EPOCH_MILLIS, Direction.ASCENDING)
                    .snapshots
                    .collectLatest { snapshot ->
                        latestEvents = snapshot.documents.map { it.data<CheckEvent>() }
                        emitSnapshot()
                    }
            }.onFailure(onError)
        }

        return object : LiveRaceSubscription {
            override fun close() {
                listOf<Job>(infoJob, eventsJob).forEach { it.cancel() }
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun enqueue(key: String, write: PendingWrite) {
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
                    is PendingWrite.RunInfo -> firestore
                        .collection(RUNS_COLLECTION)
                        .document(write.info.runCode)
                        .set(write.info)
                    is PendingWrite.Event -> firestore
                        .collection(RUNS_COLLECTION)
                        .document(write.event.runCode)
                        .collection(EVENTS_COLLECTION)
                        .document(write.event.id)
                        .set(write.event)
                }
            }
            if (result.isSuccess) return true

            onError(result.exceptionOrNull() ?: IllegalStateException("Unknown publish error"))
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }
    }

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    private companion object {
        const val RUNS_COLLECTION = "runs"
        const val EVENTS_COLLECTION = "events"
        const val FIELD_CREATED_AT_EPOCH_MILLIS = "createdAtEpochMillis"
        const val INITIAL_RETRY_DELAY_MS = 2_000L
        const val MAX_RETRY_DELAY_MS = 60_000L
    }
}

private sealed interface PendingWrite {
    data class RunInfo(val info: LiveRunInfo) : PendingWrite
    data class Event(val event: CheckEvent) : PendingWrite
}
