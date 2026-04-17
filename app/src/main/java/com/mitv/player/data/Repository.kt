package com.mitv.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

// ─── DataStore ───────────────────────────────────────────────────────────────
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mitv_settings")

// ─── Room Database ────────────────────────────────────────────────────────────
@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Int, isActive: Boolean)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Int): Playlist?
}

@Database(entities = [Playlist::class], version = 1, exportSchema = false)
abstract class MiTVDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}

// ─── Settings Keys ────────────────────────────────────────────────────────────
object SettingsKeys {
    val THEME = stringPreferencesKey("app_theme")
    val GESTURES = booleanPreferencesKey("player_gestures")
    val AUTO_RETRY = booleanPreferencesKey("auto_retry")
    val BUFFER_SIZE = stringPreferencesKey("buffer_size")
}

// ─── Main Repository ──────────────────────────────────────────────────────────
@Singleton
class MiTVRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: M3uParser,
    private val database: MiTVDatabase
) {
    private val dao = database.playlistDao()

    // ── Playlist Operations ──────────────────────────────────────────────────
    val playlists: Flow<List<Playlist>> = dao.getAllPlaylists()

    suspend fun addPlaylist(name: String, url: String): Result<Long> {
        return try {
            val id = dao.insertPlaylist(Playlist(name = name, url = url))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlist: Playlist) = dao.deletePlaylist(playlist)

    // ── Channel Loading ──────────────────────────────────────────────────────
    suspend fun loadCategories(playlistUrl: String): Result<List<Category>> {
        return parser.parseFromUrl(playlistUrl)
    }

    suspend fun loadAllActiveCategories(): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val activePlaylists = dao.getAllPlaylists().first().filter { it.isActive }
                if (activePlaylists.isEmpty()) return@withContext Result.success(emptyList())

                val allCategories = mutableMapOf<String, MutableList<Channel>>()
                for (playlist in activePlaylists) {
                    val result = parser.parseFromUrl(playlist.url)
                    result.getOrNull()?.forEach { category ->
                        allCategories.getOrPut(category.name) { mutableListOf() }
                            .addAll(category.channels.map { it.copy(playlistId = playlist.id.toString()) })
                    }
                }
                val merged = allCategories.map { (name, channels) ->
                    Category(name = name, channels = channels)
                }.sortedBy { it.name }
                Result.success(merged)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Settings ─────────────────────────────────────────────────────────────
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                theme = AppTheme.entries.find { it.name == prefs[SettingsKeys.THEME] } ?: AppTheme.DARK_GOLD,
                playerGestures = prefs[SettingsKeys.GESTURES] ?: true,
                autoRetry = prefs[SettingsKeys.AUTO_RETRY] ?: true,
                bufferSize = BufferSize.entries.find { it.name == prefs[SettingsKeys.BUFFER_SIZE] } ?: BufferSize.MEDIUM
            )
        }

    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.THEME] = settings.theme.name
            prefs[SettingsKeys.GESTURES] = settings.playerGestures
            prefs[SettingsKeys.AUTO_RETRY] = settings.autoRetry
            prefs[SettingsKeys.BUFFER_SIZE] = settings.bufferSize.name
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        context.cacheDir.deleteRecursively()
        context.cacheDir.mkdirs()
    }
}
