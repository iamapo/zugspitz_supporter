package de.zugspitz.supporter.screens

import de.zugspitz.supporter.data.AidStation
import kotlin.test.Test
import kotlin.test.assertContains

class OfflineMapScreenTest {
    @Test
    fun `gpx parser reads track points regardless of attribute order`() {
        val geoJson = parseRouteLineGeoJson(
            """
            <gpx>
              <trk><trkseg>
                <trkpt lon="11.1" lat="47.1"></trkpt>
                <trkpt lat="47.2" lon="11.2"></trkpt>
              </trkseg></trk>
            </gpx>
            """.trimIndent(),
        )

        assertContains(geoJson, "[11.1,47.1]")
        assertContains(geoJson, "[11.2,47.2]")
    }

    @Test
    fun `fallback route geojson uses station coordinates`() {
        val geoJson = buildRouteFallbackGeoJson(
            listOf(
                AidStation(1, "VP 1", "Start", 10.0, 47.1, 11.1, 10.0, 100, 50, 0, 0, 0, 5),
                AidStation(2, "Finish", "VP 1", 20.0, 47.2, 11.2, 10.0, 0, 50, 0, 0, 0, 0),
            ),
        )

        assertContains(geoJson, "[11.1,47.1]")
        assertContains(geoJson, "[11.2,47.2]")
    }
}
