package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.SessionRepository

class ResetSessionUseCase(private val repository: SessionRepository) {
    operator fun invoke() = repository.clear()
}
