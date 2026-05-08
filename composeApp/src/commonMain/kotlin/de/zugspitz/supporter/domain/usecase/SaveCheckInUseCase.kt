package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckIn

class SaveCheckInUseCase {
    operator fun invoke(
        existingCheckIns: List<CheckIn>,
        stationSection: Int,
        actualArrivalMinutes: Int,
    ): List<CheckIn> {
        return existingCheckIns.filterNot { it.stationSection == stationSection } +
            CheckIn(stationSection, actualArrivalMinutes)
    }
}
