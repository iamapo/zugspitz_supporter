package de.zugspitz.supporter.data

interface SessionRepository {
    fun load(): AppSessionState
    fun save(state: AppSessionState)
    fun clear()
}

class DefaultSessionRepository(
    private val store: AppSessionStore = AppSessionStore(),
) : SessionRepository {
    override fun load(): AppSessionState = store.load()

    override fun save(state: AppSessionState) = store.save(state)

    override fun clear() = store.clear()
}
