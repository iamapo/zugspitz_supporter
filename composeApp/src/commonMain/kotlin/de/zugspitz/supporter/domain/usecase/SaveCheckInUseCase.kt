package de.zugspitz.supporter.domain.usecase

import de.zugspitz.supporter.data.CheckIn

class SaveCheckInUseCase {
    operator fun invoke(
        existingCheckIns: List<CheckIn>,
        stationSection: Int,
        actualArrivalMinutes: Int? = null,
        actualDepartureMinutes: Int? = null,
    ): List<CheckIn> {
        val previous = existingCheckIns.firstOrNull { it.stationSection == stationSection }
        val merged = CheckIn(
            stationSection = stationSection,
            actualArrivalMinutes = actualArrivalMinutes ?: previous?.actualArrivalMinutes ?: 0,
            actualDepartureMinutes = actualDepartureMinutes ?: previous?.actualDepartureMinutes,
        )
        return existingCheckIns.filterNot { it.stationSection == stationSection } + merged
    }
}
