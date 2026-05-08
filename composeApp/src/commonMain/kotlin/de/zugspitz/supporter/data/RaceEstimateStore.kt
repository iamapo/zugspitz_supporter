package de.zugspitz.supporter.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

class RaceEstimateStore(
    private val settings: Settings = Settings(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(): RaceEstimate {
        val raw = settings.getStringOrNull(KEY_ESTIMATE) ?: return RaceEstimate()
        return runCatching { json.decodeFromString<RaceEstimate>(raw) }.getOrElse { RaceEstimate() }
    }

    fun save(estimate: RaceEstimate) {
        settings.putString(KEY_ESTIMATE, json.encodeToString(estimate))
    }

    private companion object {
        const val KEY_ESTIMATE = "race_estimate_v1"
    }
}
