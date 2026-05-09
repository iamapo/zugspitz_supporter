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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    override fun publishRunInfo(info: LiveRunInfo) {
        scope.launch {
            runCatching {
                ensureSignedIn()
                firestore
                    .collection(RUNS_COLLECTION)
                    .document(info.runCode)
                    .set(info)
            }.onFailure(onError)
        }
    }

    override fun publish(event: CheckEvent) {
        scope.launch {
            runCatching {
                ensureSignedIn()
                firestore
                    .collection(RUNS_COLLECTION)
                    .document(event.runCode)
                    .collection(EVENTS_COLLECTION)
                    .document(event.id)
                    .set(event)
            }.onFailure(onError)
        }
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

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }

    private companion object {
        const val RUNS_COLLECTION = "runs"
        const val EVENTS_COLLECTION = "events"
        const val FIELD_CREATED_AT_EPOCH_MILLIS = "createdAtEpochMillis"
    }
}
