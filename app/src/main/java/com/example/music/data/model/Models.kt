package com.example.music.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo de datos para una canción
 * Compatible con YouTube Music / InnerTube API
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: String,                    // Video ID de YouTube
    val title: String,
    val artistName: String? = null,
    val artistId: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val durationSeconds: Int,          // Duración en segundos
    val thumbnailUrl: String? = null,  // URL de la miniatura
    val streamUrl: String? = null,     // URL del stream (puede expirar)
    val streamUrlExpiry: Long = 0L,    // Timestamp de expiración
    val isExplicit: Boolean = false,
    val year: Int? = null,
    val likeStatus: LikeStatus = LikeStatus.INDIFFERENT,
    val totalPlayTimeMs: Long = 0L,    // Tiempo total reproducido
    val playCount: Int = 0,
    val isCached: Boolean = false,     // Si está descargado para offline
    val cacheSize: Long = 0L,          // Tamaño del archivo cacheado
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null
)

enum class LikeStatus {
    LIKE,
    DISLIKE,
    INDIFFERENT
}

/**
 * Información de formato de stream
 */
data class StreamFormat(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val audioChannels: Int = 2,
    val audioSampleRate: Int = 44100,
    val contentLength: Long? = null
)

/**
 * Metadata completa de una canción con URLs de stream
 */
data class SongWithStreams(
    val song: Song,
    val formats: List<StreamFormat>,
    val expiresInSeconds: Long = 21600 // 6 horas por defecto
)

/**
 * Estado de una canción en la cola de reproducción
 */
@Entity(tableName = "queue")
data class QueueItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Información de reproducción actual
 */
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val error: PlaybackError? = null
)

enum class RepeatMode {
    OFF,    // No repetir
    ONE,    // Repetir canción actual
    ALL     // Repetir toda la cola
}

data class PlaybackError(
    val code: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Comando para el servicio de música
 */
sealed class PlayerCommand {
    data class Play(val song: Song? = null) : PlayerCommand()
    object Pause : PlayerCommand()
    object PlayPause : PlayerCommand()
    object Next : PlayerCommand()
    object Previous : PlayerCommand()
    data class SeekTo(val positionMs: Long) : PlayerCommand()
    data class SetRepeatMode(val mode: RepeatMode) : PlayerCommand()
    data class SetShuffleEnabled(val enabled: Boolean) : PlayerCommand()
    data class SetPlaybackSpeed(val speed: Float) : PlayerCommand()
    data class AddToQueue(val songs: List<Song>) : PlayerCommand()
    data class RemoveFromQueue(val index: Int) : PlayerCommand()
    data class MoveQueueItem(val from: Int, val to: Int) : PlayerCommand()
    object ClearQueue : PlayerCommand()
    data class PlayQueue(val songs: List<Song>, val startIndex: Int = 0) : PlayerCommand()
}

/**
 * Evento del reproductor (one-time events)
 */
sealed class PlayerEvent {
    data class Error(val error: PlaybackError) : PlayerEvent()
    data class SongChanged(val song: Song) : PlayerEvent()
    object PlaybackEnded : PlayerEvent()
    data class BufferingUpdate(val percent: Int) : PlayerEvent()
}