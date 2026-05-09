package de.zugspitz.supporter.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KmpFirebaseLiveRaceRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : LiveRaceRepository {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    override fun publish(event: CheckEvent) {
        scope.launch {
            ensureSignedIn()
            firestore
                .collection(RUNS_COLLECTION)
                .document(event.runCode)
                .collection(EVENTS_COLLECTION)
                .document(event.id)
                .set(event)
        }
    }

    override fun subscribe(runCode: String, onEventsChanged: (List<CheckEvent>) -> Unit): LiveRaceSubscription {
        val job = scope.launch {
            ensureSignedIn()
            firestore
                .collection(RUNS_COLLECTION)
                .document(runCode)
                .collection(EVENTS_COLLECTION)
                .orderBy(FIELD_CREATED_AT_EPOCH_MILLIS, Direction.ASCENDING)
                .snapshots
                .collectLatest { snapshot ->
                    onEventsChanged(snapshot.documents.map { it.data<CheckEvent>() })
                }
        }

        return object : LiveRaceSubscription {
            override fun close() {
                job.cancel()
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
