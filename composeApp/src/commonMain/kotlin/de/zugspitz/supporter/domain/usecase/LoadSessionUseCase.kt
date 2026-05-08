package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.SessionRepository

class LoadSessionUseCase(private val repository: SessionRepository) {
    operator fun invoke(): AppSessionState = repository.load()
}
