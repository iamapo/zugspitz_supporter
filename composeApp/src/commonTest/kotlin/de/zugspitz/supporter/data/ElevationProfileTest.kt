package de.zugspitz.supporter.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElevationProfileTest {
    @Test
    fun `gpx elevation parser reads track points with elevation`() {
        val profile = parseElevationProfile(
            """
            <gpx>
              <trk><trkseg>
                <trkpt lon="11.1" lat="47.1"><ele>700.5</ele></trkpt>
                <trkpt lat="47.2" lon="11.2"><ele>820.0</ele></trkpt>
              </trkseg></trk>
            </gpx>
            """.trimIndent(),
        )

        assertEquals(2, profile.size)
        assertEquals(0.0, profile.first().distanceKm)
        assertEquals(700.5, profile.first().elevationMeters)
        assertEquals(820.0, profile.last().elevationMeters)
        assertTrue(profile.last().distanceKm > 0.0)
    }

    @Test
    fun `section profile interpolates boundaries and resets distance`() {
        val section = sectionElevationProfile(
            routeProfile = listOf(
                ElevationSample(0.0, 700.0),
                ElevationSample(5.0, 900.0),
                ElevationSample(10.0, 800.0),
            ),
            fromKm = 2.5,
            toKm = 7.5,
        )

        assertEquals(3, section.size)
        assertEquals(0.0, section.first().distanceKm)
        assertEquals(800.0, section.first().elevationMeters)
        assertEquals(2.5, section[1].distanceKm)
        assertEquals(900.0, section[1].elevationMeters)
        assertEquals(5.0, section.last().distanceKm)
        assertEquals(850.0, section.last().elevationMeters)
    }
}
