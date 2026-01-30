package com.example.music.ui.theme.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tunombre.music1.data.model.*
import com.tunombre.music1.data.repository.PlayerRepository
import com.tunombre.music1.data.repository.SongRepository
import com.tunombre.music1.service.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla del reproductor
 * Gestiona el estado y los comandos de reproducción
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val playerRepository: PlayerRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    // Estado de conexión con el servicio
    val isConnected = playerConnection.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Estado de reproducción
    val playbackState = playerConnection.playbackState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlaybackState()
        )

    // Eventos del reproductor
    val playerEvents = playerConnection.playerEvents

    // UI State específico del player screen
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Progress bar state (actualizado más frecuentemente)
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    init {
        // Conectar al servicio cuando se crea el ViewModel
        playerConnection.connect()

        // Observar cambios en el playback state para actualizar UI
        observePlaybackState()

        // Actualizar progreso
        updateProgress()

        // Cargar estado guardado si existe
        restoreSavedState()
    }

    // ============================================================
    // Métodos de control de reproducción
    // ============================================================

    /**
     * Reproducir una canción
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                playerConnection.playSong(song)

                // Guardar en historial
                songRepository.addToHistory(song)

                // Incrementar play count
                songRepository.incrementPlayCount(song.id)

            } catch (e: Exception) {
                showError("Error al reproducir: ${e.message}")
            }
        }
    }

    /**
     * Reproducir una cola de canciones
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            try {
                playerConnection.playQueue(songs, startIndex)

                // Guardar cola en la base de datos
                playerRepository.saveQueue(songs)

            } catch (e: Exception) {
                showError("Error al reproducir cola: ${e.message}")
            }
        }
    }

    /**
     * Play/Pause
     */
    fun playPause() {
        playerConnection.playPause()
    }

    /**
     * Siguiente canción
     */
    fun skipToNext() {
        playerConnection.skipToNext()
    }

    /**
     * Canción anterior
     */
    fun skipToPrevious() {
        playerConnection.skipToPrevious()
    }

    /**
     * Buscar a una posición específica
     */
    fun seekTo(position: Float) {
        val duration = playbackState.value.duration
        if (duration > 0) {
            val positionMs = (position * duration).toLong()
            playerConnection.seekTo(positionMs)
        }
    }

    /**
     * Toggle modo de repetición
     */
    fun toggleRepeatMode() {
        val currentMode = playbackState.value.repeatMode
        val nextMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerConnection.setRepeatMode(nextMode)
    }

    /**
     * Toggle shuffle
     */
    fun toggleShuffle() {
        val current = playbackState.value.shuffleEnabled
        playerConnection.setShuffleEnabled(!current)
    }

    /**
     * Establecer velocidad de reproducción
     */
    fun setPlaybackSpeed(speed: Float) {
        playerConnection.setPlaybackSpeed(speed)
    }

    /**
     * Agregar canción a favoritos
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            playbackState.value.currentSong?.let { song ->
                val newStatus = if (song.likeStatus == LikeStatus.LIKE) {
                    LikeStatus.INDIFFERENT
                } else {
                    LikeStatus.LIKE
                }

                songRepository.updateLikeStatus(song.id, newStatus)
            }
        }
    }

    /**
     * Mostrar/ocultar cola
     */
    fun toggleQueueSheet() {
        _uiState.update { it.copy(showQueueSheet = !it.showQueueSheet) }
    }

    /**
     * Mostrar/ocultar opciones de velocidad
     */
    fun toggleSpeedSheet() {
        _uiState.update { it.copy(showSpeedSheet = !it.showSpeedSheet) }
    }

    /**
     * Mostrar/ocultar letra
     */
    fun toggleLyrics() {
        _uiState.update { it.copy(showLyrics = !it.showLyrics) }

        // Cargar letra si no está cargada
        if (_uiState.value.showLyrics && _uiState.value.lyrics == null) {
            loadLyrics()
        }
    }

    // ============================================================
    // Gestión de cola
    // ============================================================

    /**
     * Agregar canciones a la cola
     */
    fun addToQueue(songs: List<Song>) {
        playerConnection.addToQueue(songs)
    }

    /**
     * Remover canción de la cola
     */
    fun removeFromQueue(index: Int) {
        playerConnection.removeFromQueue(index)
    }

    /**
     * Mover canción en la cola
     */
    fun moveQueueItem(from: Int, to: Int) {
        playerConnection.moveQueueItem(from, to)
    }

    /**
     * Limpiar cola
     */
    fun clearQueue() {
        playerConnection.clearQueue()
    }

    // ============================================================
    // Métodos privados
    // ============================================================

    private fun observePlaybackState() {
        viewModelScope.launch {
            playbackState.collect { state ->
                // Actualizar UI state basado en playback state
                _uiState.update {
                    it.copy(
                        isFavorite = state.currentSong?.likeStatus == LikeStatus.LIKE,
                        canSkipNext = state.currentIndex < state.queue.size - 1,
                        canSkipPrevious = state.currentIndex > 0
                    )
                }

                // Manejar errores
                state.error?.let { error ->
                    showError(error.message)
                }
            }
        }
    }

    private fun updateProgress() {
        viewModelScope.launch {
            while (true) {
                val state = playbackState.value
                if (state.isPlaying && state.duration > 0) {
                    _progress.value = state.currentPosition.toFloat() / state.duration
                }
                kotlinx.coroutines.delay(100) // Actualizar cada 100ms
            }
        }
    }

    private fun restoreSavedState() {
        viewModelScope.launch {
            try {
                val savedState = playerRepository.getLastPlaybackState()
                savedState?.let { state ->
                    // Restaurar cola si existe
                    if (state.queue.isNotEmpty()) {
                        // Opcional: preguntar al usuario si quiere continuar
                        // donde se quedó
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLyrics() {
        viewModelScope.launch {
            try {
                playbackState.value.currentSong?.let { song ->
                    _uiState.update { it.copy(isLoadingLyrics = true) }

                    val lyrics = songRepository.getLyrics(song.id)

                    _uiState.update {
                        it.copy(
                            lyrics = lyrics,
                            isLoadingLyrics = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        lyrics = null,
                        isLoadingLyrics = false
                    )
                }
                showError("No se pudieron cargar las letras")
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(errorMessage = message)
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    // ============================================================
    // Cleanup
    // ============================================================

    override fun onCleared() {
        super.onCleared()
        // No desconectamos aquí porque el servicio debe seguir corriendo
        // playerConnection.disconnect()
    }
}

/**
 * Estado específico de la UI del player
 */
data class PlayerUiState(
    val showQueueSheet: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showLyrics: Boolean = false,
    val lyrics: String? = null,
    val isLoadingLyrics: Boolean = false,
    val isFavorite: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val errorMessage: String? = null
)