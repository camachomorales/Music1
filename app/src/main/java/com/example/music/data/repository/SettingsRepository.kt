package com.example.music.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.music.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Extension para DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repositorio de configuraciones
 * Guarda y carga preferencias del usuario usando DataStore
 */
class SettingsRepository(private val context: Context) {

    private val TAG = "SettingsRepository"

    // Keys para DataStore
    private object PreferencesKeys {
        // Account
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val PROFILE_IMAGE_URL = stringPreferencesKey("profile_image_url")
        val ACCOUNT_CREATED_AT = longPreferencesKey("account_created_at")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

        // Statistics
        val TOTAL_SONGS_PLAYED = longPreferencesKey("total_songs_played")
        val TOTAL_PLAYBACK_TIME = longPreferencesKey("total_playback_time")
        val FAVORITE_GENRE = stringPreferencesKey("favorite_genre")
        val MOST_PLAYED_SONG = stringPreferencesKey("most_played_song")
        val SONGS_DOWNLOADED = intPreferencesKey("songs_downloaded")
        val PLAYLISTS_CREATED = intPreferencesKey("playlists_created")

        // Appearance
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")

        // Playback
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val STREAMING_QUALITY = stringPreferencesKey("streaming_quality")
        val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val NORMALIZE_VOLUME = booleanPreferencesKey("normalize_volume")
        val EQUALIZER_PRESET = stringPreferencesKey("equalizer_preset")

        // Downloads
        val DOWNLOAD_ONLY_ON_WIFI = booleanPreferencesKey("download_only_on_wifi")
        val AUTO_DOWNLOAD_FAVORITES = booleanPreferencesKey("auto_download_favorites")
        val MAX_CACHE_SIZE = intPreferencesKey("max_cache_size")
        val AUTO_DELETE_CACHE = booleanPreferencesKey("auto_delete_cache")
        val CACHE_LOCATION = stringPreferencesKey("cache_location")

        // Notifications
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SHOW_PLAYBACK_CONTROLS = booleanPreferencesKey("show_playback_controls")
        val SHOW_ALBUM_ART = booleanPreferencesKey("show_album_art")

        // Advanced
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val SHOW_DEBUG_INFO = booleanPreferencesKey("show_debug_info")
        val ALLOW_EXPERIMENTAL_FEATURES = booleanPreferencesKey("allow_experimental_features")
    }

    /**
     * Flow de preferencias - se actualiza automáticamente
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToUserPreferences(preferences)
        }

    /**
     * Guardar preferencias completas
     */
    suspend fun savePreferences(userPreferences: UserPreferences) {
        context.dataStore.edit { preferences ->
            // Account
            userPreferences.userId?.let { preferences[PreferencesKeys.USER_ID] = it }
            preferences[PreferencesKeys.USER_NAME] = userPreferences.userName
            userPreferences.userEmail?.let { preferences[PreferencesKeys.USER_EMAIL] = it }
            preferences[PreferencesKeys.IS_LOGGED_IN] = userPreferences.isLoggedIn
            preferences[PreferencesKeys.IS_ADMIN] = userPreferences.isAdmin
            userPreferences.profileImageUrl?.let { preferences[PreferencesKeys.PROFILE_IMAGE_URL] = it }
            preferences[PreferencesKeys.ACCOUNT_CREATED_AT] = userPreferences.accountCreatedAt
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = userPreferences.lastSyncTimestamp

            // Statistics
            preferences[PreferencesKeys.TOTAL_SONGS_PLAYED] = userPreferences.totalSongsPlayed
            preferences[PreferencesKeys.TOTAL_PLAYBACK_TIME] = userPreferences.totalPlaybackTime
            userPreferences.favoriteGenre?.let { preferences[PreferencesKeys.FAVORITE_GENRE] = it }
            userPreferences.mostPlayedSong?.let { preferences[PreferencesKeys.MOST_PLAYED_SONG] = it }
            preferences[PreferencesKeys.SONGS_DOWNLOADED] = userPreferences.songsDownloaded
            preferences[PreferencesKeys.PLAYLISTS_CREATED] = userPreferences.playlistsCreated

            // Appearance
            preferences[PreferencesKeys.IS_DARK_THEME] = userPreferences.isDarkTheme
            preferences[PreferencesKeys.ACCENT_COLOR] = userPreferences.accentColor.name
            preferences[PreferencesKeys.FONT_SIZE] = userPreferences.fontSize.name
            preferences[PreferencesKeys.ENABLE_ANIMATIONS] = userPreferences.enableAnimations

            // Playback
            preferences[PreferencesKeys.AUDIO_QUALITY] = userPreferences.audioQuality.name
            preferences[PreferencesKeys.STREAMING_QUALITY] = userPreferences.streamingQuality.name
            preferences[PreferencesKeys.DOWNLOAD_QUALITY] = userPreferences.downloadQuality.name
            preferences[PreferencesKeys.CROSSFADE_DURATION] = userPreferences.crossfadeDuration
            preferences[PreferencesKeys.GAPLESS_PLAYBACK] = userPreferences.gaplessPlayback
            preferences[PreferencesKeys.NORMALIZE_VOLUME] = userPreferences.normalizeVolume
            preferences[PreferencesKeys.EQUALIZER_PRESET] = userPreferences.equalizerPreset.name

            // Downloads
            preferences[PreferencesKeys.DOWNLOAD_ONLY_ON_WIFI] = userPreferences.downloadOnlyOnWifi
            preferences[PreferencesKeys.AUTO_DOWNLOAD_FAVORITES] = userPreferences.autoDownloadFavorites
            preferences[PreferencesKeys.MAX_CACHE_SIZE] = userPreferences.maxCacheSize
            preferences[PreferencesKeys.AUTO_DELETE_CACHE] = userPreferences.autoDeleteCache
            preferences[PreferencesKeys.CACHE_LOCATION] = userPreferences.cacheLocation

            // Notifications
            preferences[PreferencesKeys.SHOW_NOTIFICATIONS] = userPreferences.showNotifications
            preferences[PreferencesKeys.SHOW_PLAYBACK_CONTROLS] = userPreferences.showPlaybackControls
            preferences[PreferencesKeys.SHOW_ALBUM_ART] = userPreferences.showAlbumArt

            // Advanced
            preferences[PreferencesKeys.DEVELOPER_MODE] = userPreferences.developerMode
            preferences[PreferencesKeys.SHOW_DEBUG_INFO] = userPreferences.showDebugInfo
            preferences[PreferencesKeys.ALLOW_EXPERIMENTAL_FEATURES] = userPreferences.allowExperimentalFeatures
        }

        Log.d(TAG, "✅ Preferences saved")
    }

    /**
     * Actualizar estadísticas
     */
    suspend fun updateStatistics(
        songsPlayed: Long? = null,
        playbackTime: Long? = null,
        favoriteGenre: String? = null,
        mostPlayedSong: String? = null
    ) {
        context.dataStore.edit { preferences ->
            songsPlayed?.let { preferences[PreferencesKeys.TOTAL_SONGS_PLAYED] = it }
            playbackTime?.let { preferences[PreferencesKeys.TOTAL_PLAYBACK_TIME] = it }
            favoriteGenre?.let { preferences[PreferencesKeys.FAVORITE_GENRE] = it }
            mostPlayedSong?.let { preferences[PreferencesKeys.MOST_PLAYED_SONG] = it }
        }
    }

    /**
     * Limpiar todas las preferencias (para logout)
     */
    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
        Log.d(TAG, "✅ Preferences cleared")
    }

    /**
     * Mapear Preferences a UserPreferences
     */
    private fun mapPreferencesToUserPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            // Account
            userId = preferences[PreferencesKeys.USER_ID],
            userName = preferences[PreferencesKeys.USER_NAME] ?: "Guest",
            userEmail = preferences[PreferencesKeys.USER_EMAIL],
            isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false,
            isAdmin = preferences[PreferencesKeys.IS_ADMIN] ?: false,
            profileImageUrl = preferences[PreferencesKeys.PROFILE_IMAGE_URL],
            accountCreatedAt = preferences[PreferencesKeys.ACCOUNT_CREATED_AT] ?: System.currentTimeMillis(),
            lastSyncTimestamp = preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] ?: 0,

            // Statistics
            totalSongsPlayed = preferences[PreferencesKeys.TOTAL_SONGS_PLAYED] ?: 0,
            totalPlaybackTime = preferences[PreferencesKeys.TOTAL_PLAYBACK_TIME] ?: 0,
            favoriteGenre = preferences[PreferencesKeys.FAVORITE_GENRE],
            mostPlayedSong = preferences[PreferencesKeys.MOST_PLAYED_SONG],
            songsDownloaded = preferences[PreferencesKeys.SONGS_DOWNLOADED] ?: 0,
            playlistsCreated = preferences[PreferencesKeys.PLAYLISTS_CREATED] ?: 0,

            // Appearance
            isDarkTheme = preferences[PreferencesKeys.IS_DARK_THEME] ?: true,
            accentColor = preferences[PreferencesKeys.ACCENT_COLOR]?.let {
                try { AccentColor.valueOf(it) } catch (e: Exception) { AccentColor.CYAN }
            } ?: AccentColor.CYAN,
            fontSize = preferences[PreferencesKeys.FONT_SIZE]?.let {
                try { FontSize.valueOf(it) } catch (e: Exception) { FontSize.MEDIUM }
            } ?: FontSize.MEDIUM,
            enableAnimations = preferences[PreferencesKeys.ENABLE_ANIMATIONS] ?: true,

            // Playback
            audioQuality = preferences[PreferencesKeys.AUDIO_QUALITY]?.let {
                try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
            } ?: AudioQuality.HIGH,
            streamingQuality = preferences[PreferencesKeys.STREAMING_QUALITY]?.let {
                try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.NORMAL }
            } ?: AudioQuality.NORMAL,
            downloadQuality = preferences[PreferencesKeys.DOWNLOAD_QUALITY]?.let {
                try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
            } ?: AudioQuality.HIGH,
            crossfadeDuration = preferences[PreferencesKeys.CROSSFADE_DURATION] ?: 0,
            gaplessPlayback = preferences[PreferencesKeys.GAPLESS_PLAYBACK] ?: true,
            normalizeVolume = preferences[PreferencesKeys.NORMALIZE_VOLUME] ?: false,
            equalizerPreset = preferences[PreferencesKeys.EQUALIZER_PRESET]?.let {
                try { EqualizerPreset.valueOf(it) } catch (e: Exception) { EqualizerPreset.FLAT }
            } ?: EqualizerPreset.FLAT,

            // Downloads
            downloadOnlyOnWifi = preferences[PreferencesKeys.DOWNLOAD_ONLY_ON_WIFI] ?: true,
            autoDownloadFavorites = preferences[PreferencesKeys.AUTO_DOWNLOAD_FAVORITES] ?: false,
            maxCacheSize = preferences[PreferencesKeys.MAX_CACHE_SIZE] ?: 500,
            autoDeleteCache = preferences[PreferencesKeys.AUTO_DELETE_CACHE] ?: true,
            cacheLocation = preferences[PreferencesKeys.CACHE_LOCATION] ?: "internal",

            // Notifications
            showNotifications = preferences[PreferencesKeys.SHOW_NOTIFICATIONS] ?: true,
            showPlaybackControls = preferences[PreferencesKeys.SHOW_PLAYBACK_CONTROLS] ?: true,
            showAlbumArt = preferences[PreferencesKeys.SHOW_ALBUM_ART] ?: true,

            // Advanced
            developerMode = preferences[PreferencesKeys.DEVELOPER_MODE] ?: false,
            showDebugInfo = preferences[PreferencesKeys.SHOW_DEBUG_INFO] ?: false,
            allowExperimentalFeatures = preferences[PreferencesKeys.ALLOW_EXPERIMENTAL_FEATURES] ?: false
        )
    }
}