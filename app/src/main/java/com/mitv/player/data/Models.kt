package com.mitv.player.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Channel ────────────────────────────────────────────────────────────────
data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String = "",
    val group: String = "Uncategorized",
    val tvgId: String = "",
    val tvgName: String = "",
    val playlistId: String = ""
)

// ─── Category ───────────────────────────────────────────────────────────────
data class Category(
    val name: String,
    val channels: List<Channel>
)

// ─── Playlist (Room Entity) ──────────────────────────────────────────────────
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// ─── App Settings ─────────────────────────────────────────────────────────────
data class AppSettings(
    val theme: AppTheme = AppTheme.DARK_GOLD,
    val playerGestures: Boolean = true,
    val autoRetry: Boolean = true,
    val bufferSize: BufferSize = BufferSize.MEDIUM
)

enum class AppTheme(val displayName: String) {
    DARK_GOLD("Dark Gold (Premium)"),
    MIDNIGHT_BLUE("Midnight Blue"),
    AMOLED_BLACK("AMOLED Black"),
    CRIMSON_DARK("Crimson Dark")
}

enum class BufferSize(val displayName: String, val ms: Int) {
    SMALL("Fast (2s)", 2000),
    MEDIUM("Balanced (5s)", 5000),
    LARGE("Stable (10s)", 10000)
}

// ─── Playback State ──────────────────────────────────────────────────────────
sealed class PlaybackState {
    object Idle : PlaybackState()
    object Loading : PlaybackState()
    data class Playing(val channel: Channel) : PlaybackState()
    data class Paused(val channel: Channel) : PlaybackState()
    data class Error(val channel: Channel, val message: String) : PlaybackState()
    data class Buffering(val channel: Channel, val percent: Int) : PlaybackState()
}

// ─── UI State ────────────────────────────────────────────────────────────────
data class DashboardUiState(
    val playlists: List<Playlist> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentChannel: Channel? = null,
    val showControls: Boolean = true,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val isFullscreen: Boolean = false,
    val showFallback: Boolean = false
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false
)
