package de.zugspitz.supporter.data

val ZugspitzStations = listOf(
    AidStation(1, "Z1 Eibsee", "Start Garmisch", 10.7, 47.455253, 10.995159, 10.7, 433, 112, 86, 83, 88, 2),
    AidStation(2, "Z2 Gamsalm", "Z1 Eibsee", 19.4, 47.414055, 10.941956, 8.7, 712, 421, 184, 178, 189, 3),
    AidStation(3, "Z3 Pestkapelle", "Z2 Gamsalm", 27.3, 47.380021, 10.982417, 7.9, 605, 301, 271, 263, 278, 3),
    AidStation(4, "Z4 Hämmermoosalm", "Z3 Pestkapelle", 41.1, 47.370897, 11.082222, 13.8, 898, 1095, 422, 409, 434, 5),
    AidStation(5, "Z5 Hubertushof", "Z4 Hämmermoosalm", 54.6, 47.400458, 11.181670, 13.5, 708, 1052, 563, 546, 579, 12),
    AidStation(6, "Z6 Mittenwald", "Z5 Hubertushof", 63.1, 47.425571, 11.257963, 8.5, 38, 181, 628, 610, 645, 6),
    AidStation(7, "Z7 Schloss Elmau", "Z6 Mittenwald", 73.3, 47.462605, 11.188721, 10.2, 248, 152, 708, 687, 728, 4),
    AidStation(8, "Z8 Laubhütte", "Z7 Schloss Elmau", 86.8, 47.443889, 11.098065, 13.5, 686, 706, 839, 815, 862, 4),
    AidStation(9, "Z9 Hochalm", "Z8 Laubhütte", 91.7, 47.439385, 11.061616, 4.9, 713, 0, 910, 884, 935, 3),
    AidStation(10, "Z10 Tröglift", "Z9 Hochalm", 100.1, 47.461641, 11.089332, 8.4, 364, 712, 994, 965, 1022, 2),
    AidStation(11, "Ziel Garmisch", "Z10 Tröglift", 107.4, 47.494648, 11.092212, 7.3, 0, 660, 1050, 1020, 1080, 0),
)

val EhrwaldTrailStations = listOf(
    AidStation(1, "Z3 Pestkapelle", "Start Ehrwald", 6.0, 6.0, 613, 0, 0, 0, 0, 3),
    AidStation(2, "Z4 Hämmermoosalm", "Z3 Pestkapelle", 19.0, 13.0, 923, 1128, 0, 0, 0, 5),
    AidStation(3, "Z5 Hubertushof", "Z4 Hämmermoosalm", 32.0, 13.0, 639, 972, 0, 0, 0, 12),
    AidStation(4, "Z6 Mittenwald", "Z5 Hubertushof", 40.5, 8.5, 37, 190, 0, 0, 0, 6),
    AidStation(5, "Z7 Schloss Elmau", "Z6 Mittenwald", 50.5, 10.0, 248, 170, 0, 0, 0, 4),
    AidStation(6, "Z8 Laubhütte", "Z7 Schloss Elmau", 64.0, 13.5, 637, 651, 0, 0, 0, 4),
    AidStation(7, "Z9 Hochalm", "Z8 Laubhütte", 68.5, 4.5, 708, 0, 0, 0, 0, 3),
    AidStation(8, "Z10 Tröglift", "Z9 Hochalm", 77.0, 8.5, 412, 793, 0, 0, 0, 2),
    AidStation(9, "Ziel Garmisch", "Z10 Tröglift", 85.0, 8.0, 0, 628, 0, 0, 0, 0),
)

val LeutaschTrailStations = listOf(
    AidStation(1, "Z5 Hubertushof", "Start Leutasch", 15.2, 15.2, 1012, 1076, 0, 0, 0, 12),
    AidStation(2, "Z6 Mittenwald", "Z5 Hubertushof", 23.7, 8.5, 74, 218, 0, 0, 0, 6),
    AidStation(3, "Z7 Schloss Elmau", "Z6 Mittenwald", 33.9, 10.2, 344, 272, 0, 0, 0, 4),
    AidStation(4, "Z8 Laubhütte", "Z7 Schloss Elmau", 47.4, 13.5, 820, 772, 0, 0, 0, 4),
    AidStation(5, "Z9 Hochalm", "Z8 Laubhütte", 52.3, 4.9, 789, 13, 0, 0, 0, 3),
    AidStation(6, "Z10 Tröglift", "Z9 Hochalm", 60.7, 8.4, 297, 804, 0, 0, 0, 2),
    AidStation(7, "Ziel Garmisch", "Z10 Tröglift", 68.4, 7.7, 30, 645, 0, 0, 0, 0),
)

val MittenwaldTrailStations = listOf(
    AidStation(1, "Z7 Schloss Elmau", "Start Mittenwald", 9.4, 9.4, 317, 221, 0, 0, 0, 4),
    AidStation(2, "Z8 Laubhütte", "Z7 Schloss Elmau", 22.9, 13.5, 787, 787, 0, 0, 0, 4),
    AidStation(3, "Z9 Hochalm", "Z8 Laubhütte", 27.8, 4.9, 766, 13, 0, 0, 0, 3),
    AidStation(4, "Z10 Tröglift", "Z9 Hochalm", 36.1, 8.3, 354, 770, 0, 0, 0, 2),
    AidStation(5, "Ziel Garmisch", "Z10 Tröglift", 43.8, 7.7, 30, 679, 0, 0, 0, 0),
)

val GarmischPartenkirchenTrailStations = listOf(
    AidStation(1, "Z8 Laubhütte", "Start Garmisch", 8.4, 8.4, 480, 185, 0, 0, 0, 4),
    AidStation(2, "Z9 Hochalm", "Z8 Laubhütte", 13.3, 4.9, 736, 13, 0, 0, 0, 3),
    AidStation(3, "Z10 Tröglift", "Z9 Hochalm", 21.7, 8.4, 402, 766, 0, 0, 0, 2),
    AidStation(4, "Ziel Garmisch", "Z10 Tröglift", 29.3, 7.6, 32, 686, 0, 0, 0, 0),
)

val GrainauTrailStations = listOf(
    AidStation(1, "Z10 Tröglift", "Start Grainau", 8.4, 8.4, 764, 152, 0, 0, 0, 2),
    AidStation(2, "Ziel Garmisch", "Z10 Tröglift", 15.9, 7.5, 33, 693, 0, 0, 0, 0),
)

object RaceDefinitions {
    const val ZugspitzUltratrailId = "zugspitz-ultratrail"
    const val EhrwaldTrailId = "ehrwald-trail"
    const val LeutaschTrailId = "leutasch-trail"
    const val MittenwaldTrailId = "mittenwald-trail"
    const val GarmischPartenkirchenTrailId = "garmisch-partenkirchen-trail"
    const val GrainauTrailId = "grainau-trail"

    val All = listOf(
        RaceDefinition(
            id = ZugspitzUltratrailId,
            name = "Zugspitz Ultratrail",
            distanceLabel = "107 km",
            startLocation = "Garmisch-Partenkirchen",
            startTimeMinutes = 22 * 60,
            defaultMinDurationMinutes = 17 * 60,
            defaultMaxDurationMinutes = 18 * 60,
            defaultFixedDurationMinutes = 17 * 60,
            stations = ZugspitzStations,
            segmentFactors = mapOf(
                1 to 0.82,
                2 to 0.81,
                3 to 0.77,
                4 to 0.99,
                5 to 0.98,
                6 to 1.08,
                7 to 1.06,
                8 to 1.12,
                9 to 1.23,
                10 to 1.21,
                11 to 1.02,
            ),
        ),
        RaceDefinition(
            id = EhrwaldTrailId,
            name = "Ehrwald Trail",
            distanceLabel = "86 km",
            startLocation = "Ehrwald",
            startTimeMinutes = 23 * 60,
            defaultMinDurationMinutes = 18 * 60,
            defaultMaxDurationMinutes = 21 * 60,
            defaultFixedDurationMinutes = 20 * 60,
            stations = EhrwaldTrailStations,
            segmentFactors = mapOf(
                1 to 0.78,
                2 to 1.02,
                3 to 0.98,
                4 to 1.08,
                5 to 1.06,
                6 to 1.12,
                7 to 1.23,
                8 to 1.21,
                9 to 1.02,
            ),
        ),
        RaceDefinition(
            id = LeutaschTrailId,
            name = "Leutasch Trail",
            distanceLabel = "68 km",
            startLocation = "Leutasch",
            startTimeMinutes = 9 * 60,
            defaultMinDurationMinutes = 12 * 60,
            defaultMaxDurationMinutes = 16 * 60,
            defaultFixedDurationMinutes = 16 * 60,
            stations = LeutaschTrailStations,
            segmentFactors = mapOf(
                1 to 1.0,
                2 to 1.08,
                3 to 1.06,
                4 to 1.12,
                5 to 1.23,
                6 to 1.21,
                7 to 1.02,
            ),
        ),
        RaceDefinition(
            id = MittenwaldTrailId,
            name = "Mittenwald Trail",
            distanceLabel = "44 km",
            startLocation = "Mittenwald",
            startTimeMinutes = 7 * 60,
            defaultMinDurationMinutes = 8 * 60,
            defaultMaxDurationMinutes = 10 * 60,
            defaultFixedDurationMinutes = 10 * 60,
            stations = MittenwaldTrailStations,
            segmentFactors = mapOf(
                1 to 1.06,
                2 to 1.12,
                3 to 1.23,
                4 to 1.21,
                5 to 1.02,
            ),
        ),
        RaceDefinition(
            id = GarmischPartenkirchenTrailId,
            name = "Garmisch-Partenkirchen Trail",
            distanceLabel = "29 km",
            startLocation = "Garmisch-Partenkirchen",
            startTimeMinutes = 10 * 60,
            defaultMinDurationMinutes = 5 * 60,
            defaultMaxDurationMinutes = 7 * 60,
            defaultFixedDurationMinutes = 7 * 60,
            stations = GarmischPartenkirchenTrailStations,
            segmentFactors = mapOf(
                1 to 1.12,
                2 to 1.23,
                3 to 1.21,
                4 to 1.02,
            ),
        ),
        RaceDefinition(
            id = GrainauTrailId,
            name = "Grainau Trail",
            distanceLabel = "16 km",
            startLocation = "Grainau",
            startTimeMinutes = 18 * 60,
            defaultMinDurationMinutes = 2 * 60,
            defaultMaxDurationMinutes = 4 * 60,
            defaultFixedDurationMinutes = 4 * 60,
            stations = GrainauTrailStations,
            segmentFactors = mapOf(
                1 to 1.21,
                2 to 1.02,
            ),
        ),
    )

    fun byId(id: String): RaceDefinition = All.firstOrNull { it.id == id } ?: All.first()
}
