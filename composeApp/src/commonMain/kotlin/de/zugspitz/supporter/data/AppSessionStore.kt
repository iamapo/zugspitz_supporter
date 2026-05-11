package de.zugspitz.supporter.data

import com.russhwolf.settings.Settings
import de.zugspitz.supporter.components.AppTab
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AppSessionStore(
    private val settings: Settings = Settings(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(): AppSessionState {
        val raw = settings.getStringOrNull(KEY_SESSION) ?: return AppSessionState()
        return runCatching { json.decodeFromString<AppSessionState>(raw) }.getOrElse { AppSessionState() }
    }

    fun save(state: AppSessionState) {
        settings.putString(KEY_SESSION, json.encodeToString(state))
    }

    fun clear() {
        settings.remove(KEY_SESSION)
    }

    private companion object {
        const val KEY_SESSION = "app_session_v1"
    }
}

@Serializable
data class AppSessionState(
    val estimate: RaceEstimate = RaceEstimate(),
    val hasSelectedRace: Boolean = false,
    val tab: SavedTab = SavedTab.Role,
    val selectedIndex: Int = 0,
    val checkIns: List<CheckIn> = emptyList(),
    val liveRunLink: LiveRunLink = LiveRunLink(),
    val checkEvents: List<CheckEvent> = emptyList(),
)

@Serializable
enum class SavedTab {
    Role,
    SupportCode,
    Race,
    Setup,
    Vp,
    Summary,
    List,
    Settings,
}

fun SavedTab.toAppTab(): AppTab = when (this) {
    SavedTab.Role -> AppTab.Role
    SavedTab.SupportCode -> AppTab.SupportCode
    SavedTab.Race -> AppTab.Race
    SavedTab.Setup -> AppTab.Setup
    SavedTab.Vp -> AppTab.Vp
    SavedTab.Summary -> AppTab.Summary
    SavedTab.List -> AppTab.List
    SavedTab.Settings -> AppTab.Settings
}

fun AppTab.toSavedTab(): SavedTab = when (this) {
    AppTab.Role -> SavedTab.Role
    AppTab.SupportCode -> SavedTab.SupportCode
    AppTab.Race -> SavedTab.Race
    AppTab.Setup -> SavedTab.Setup
    AppTab.Vp -> SavedTab.Vp
    AppTab.Summary -> SavedTab.Summary
    AppTab.List -> SavedTab.List
    AppTab.Settings -> SavedTab.Settings
}
