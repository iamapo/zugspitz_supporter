package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.RaceEstimate

class UpdateEstimateUseCase {
    operator fun invoke(newEstimate: RaceEstimate): RaceEstimate = newEstimate
}
