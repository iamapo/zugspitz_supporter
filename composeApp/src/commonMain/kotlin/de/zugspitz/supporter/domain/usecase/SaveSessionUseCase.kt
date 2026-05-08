package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.AppSessionState
import de.zugspitz.supporter.data.SessionRepository

class SaveSessionUseCase(private val repository: SessionRepository) {
    operator fun invoke(state: AppSessionState) = repository.save(state)
}
