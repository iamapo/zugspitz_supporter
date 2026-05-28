package de.zugspitz.supporter.data

internal fun gpxPathForRace(raceId: String): String? = when (raceId) {
    RaceDefinitions.ZugspitzUltratrailId -> "files/gpx/Ultratrail_ZUT_2025_3b6cbaa510.gpx"
    RaceDefinitions.MittenwaldTrailId -> "files/gpx/Mittenwald_Trail_ZUT_2025_fa6c0d4010.gpx"
    RaceDefinitions.LeutaschTrailId -> "files/gpx/Leutasch_Trail_ZUT_2025_620e36ae36.gpx"
    RaceDefinitions.Zut100Id -> "files/gpx/ZUT_100_2026_Start_Ga_Pa_5bcee57cdf.gpx"
    RaceDefinitions.EhrwaldTrailId -> "files/gpx/Ehrwald_Trail_ZUT_2025_85a841b963.gpx"
    RaceDefinitions.GarmischPartenkirchenTrailId -> "files/gpx/Garmisch_Partenkirchen_Trail_ZUT_2025_1d59df01ee.gpx"
    else -> null
}
