package de.zugspitz.supporter.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class RaceEstimateStoreTest {
    @Test
    fun `load returns default when settings empty`() {
        val store = RaceEstimateStore(settings = MapSettings())

        assertEquals(RaceEstimate(), store.load())
    }

    @Test
    fun `save then load returns same estimate`() {
        val settings = MapSettings()
        val store = RaceEstimateStore(settings = settings)
        val estimate = RaceEstimate(
            startTimeMinutes = 21 * 60 + 30,
            targetMode = TargetTimeMode.Fixed,
            fixedDurationMinutes = 16 * 60 + 45,
            minDurationMinutes = 16 * 60,
            maxDurationMinutes = 18 * 60,
        )

        store.save(estimate)

        assertEquals(estimate, store.load())
    }

    @Test
    fun `load returns default when stored json is invalid`() {
        val settings = MapSettings().apply {
            putString("race_estimate_v1", "{invalid")
        }
        val store = RaceEstimateStore(settings = settings)

        assertEquals(RaceEstimate(), store.load())
    }
}
