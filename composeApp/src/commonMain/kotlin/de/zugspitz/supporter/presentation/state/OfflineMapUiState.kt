package de.zugspitz.supporter.presentation.state

data class OfflineMapUiState(
    val isDownloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedTiles: Int = 0,
    val totalTiles: Int = 0,
    val downloadedMegabytes: Double = 0.0,
    val isReady: Boolean = false,
)
