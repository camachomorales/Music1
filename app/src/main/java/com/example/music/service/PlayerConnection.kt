package com.example.music.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tunombre.music1.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton que conecta la UI con el MusicService
 * Similar a la implementación de InnerTune
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Estado de conexión
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Estado de reproducción expuesto a la UI
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Eventos del reproductor
    private val _playerEvents = MutableSharedFlow<PlayerEvent>()
    val playerEvents: SharedFlow<PlayerEvent> = _playerEvents.asSharedFlow()

    /**
     * Conectar al servicio de música
     */
    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener(
            {
                try {
                    mediaController = controllerFuture?.get()
                    _isConnected.value = true
                    setupListeners()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _isConnected.value = false
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Desconectar del servicio
     */
    fun disconnect() {
        mediaController?.release()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        controllerFuture = null
        _isConnected.value = false
    }

    /**
     * Configurar listeners del MediaController
     */
    private fun setupListeners() {
        mediaController?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
                updatePlaybackState(isBuffering = isBuffering)
            }

            override fun onMediaItemTransition(
                mediaItem: androidx.media3.common.MediaItem?,
                reason: Int
            ) {
                // Actualizar canción actual
                mediaItem?.let {
                    // Emitir evento de cambio de canción
                }
            }
        })
    }

    // ============================================================
    // Comandos de reproducción
    // ============================================================

    /**
     * Reproducir una canción
     */
    fun playSong(song: Song) {
        // En una implementación real, llamarías a un método del servicio
        // Por ahora, usamos el MediaController directamente
        mediaController?.let { controller ->
            val mediaItem = song.toMediaItem()
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    /**
     * Reproducir una cola de canciones
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        mediaController?.let { controller ->
            val mediaItems = songs.map { it.toMediaItem() }
            controller.setMediaItems(mediaItems, startIndex, 0)
            controller.prepare()
            controller.play()
        }
    }

    /**
     * Play/Pause toggle
     */
    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    /**
     * Pausar reproducción
     */
    fun pause() {
        mediaController?.pause()
    }

    /**
     * Reanudar reproducción
     */
    fun play() {
        mediaController?.play()
    }

    /**
     * Siguiente canción
     */
    fun skipToNext() {
        mediaController?.seekToNext()
    }

    /**
     * Canción anterior
     */
    fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    /**
     * Buscar a posición específica
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    /**
     * Establecer modo de repetición
     */
    fun setRepeatMode(mode: RepeatMode) {
        val exoRepeatMode = when (mode) {
            RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
        }
        mediaController?.repeatMode = exoRepeatMode
    }

    /**
     * Activar/desactivar shuffle
     */
    fun setShuffleEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
    }

    /**
     * Establecer velocidad de reproducción
     */
    fun setPlaybackSpeed(speed: Float) {
        mediaController?.setPlaybackSpeed(speed)
    }

    /**
     * Agregar canciones a la cola
     */
    fun addToQueue(songs: List<Song>) {
        mediaController?.let { controller ->
            val mediaItems = songs.map { it.toMediaItem() }
            controller.addMediaItems(mediaItems)
        }
    }

    /**
     * Remover canción de la cola
     */
    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    /**
     * Mover item en la cola
     */
    fun moveQueueItem(from: Int, to: Int) {
        mediaController?.moveMediaItem(from, to)
    }

    /**
     * Limpiar cola
     */
    fun clearQueue() {
        mediaController?.clearMediaItems()
    }

    // ============================================================
    // Getters de estado actual
    // ============================================================

    /**
     * Obtener posición actual de reproducción
     */
    val currentPosition: Long
        get() = mediaController?.currentPosition ?: 0L

    /**
     * Obtener duración total
     */
    val duration: Long
        get() = mediaController?.duration ?: 0L

    /**
     * Verificar si está reproduciendo
     */
    val isPlaying: Boolean
        get() = mediaController?.isPlaying ?: false

    /**
     * Obtener canción actual
     */
    val currentSong: Song?
        get() = _playbackState.value.currentSong

    /**
     * Obtener cola actual
     */
    val currentQueue: List<Song>
        get() = _playbackState.value.queue

    /**
     * Obtener índice actual
     */
    val currentIndex: Int
        get() = mediaController?.currentMediaItemIndex ?: -1

    // ============================================================
    // Métodos privados
    // ============================================================

    private fun updatePlaybackState(
        currentSong: Song? = _playbackState.value.currentSong,
        isPlaying: Boolean = _playbackState.value.isPlaying,
        isBuffering: Boolean = _playbackState.value.isBuffering,
        currentPosition: Long = this.currentPosition,
        duration: Long = this.duration,
        repeatMode: RepeatMode = _playbackState.value.repeatMode,
        shuffleEnabled: Boolean = _playbackState.value.shuffleEnabled,
        queue: List<Song> = _playbackState.value.queue,
        currentIndex: Int = this.currentIndex,
        playbackSpeed: Float = _playbackState.value.playbackSpeed,
        error: PlaybackError? = null
    ) {
        _playbackState.value = PlaybackState(
            currentSong = currentSong,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            currentPosition = currentPosition,
            duration = duration,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            queue = queue,
            currentIndex = currentIndex,
            playbackSpeed = playbackSpeed,
            error = error
        )
    }
}

/**
 * Extension para convertir Song a MediaItem
 */
fun Song.toMediaItem(): androidx.media3.common.MediaItem {
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(this.id)
        .setUri(this.streamUrl ?: "")
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(this.title)
                .setArtist(this.artistName)
                .setAlbumTitle(this.albumName)
                .setArtworkUri(android.net.Uri.parse(this.thumbnailUrl))
                .setIsPlayable(true)
                .build()
        )
        .build()
}