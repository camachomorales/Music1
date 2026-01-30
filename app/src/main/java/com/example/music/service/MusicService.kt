package com.tunombre.music1.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import com.tunombre.music1.data.model.*
import com.tunombre.music1.data.repository.PlayerRepository
import com.tunombre.music1.utils.toMediaItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.io.File

/**
 * Servicio de música principal basado en MediaSessionService
 * Implementación similar a InnerTune
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject
    lateinit var playerRepository: PlayerRepository

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Cache para reproducción offline
    private var simpleCache: SimpleCache? = null

    // Estado actual
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _playerEvents = MutableSharedFlow<PlayerEvent>()
    val playerEvents: SharedFlow<PlayerEvent> = _playerEvents.asSharedFlow()

    // Cola de reproducción
    private var currentQueue = mutableListOf<Song>()
    private var currentIndex = -1
    private var shuffleMode = false
    private var originalQueue = mutableListOf<Song>()

    override fun onCreate() {
        super.onCreate()

        initializeCache()
        initializePlayer()
        initializeMediaSession()
        observePlayerEvents()
    }

    /**
     * Inicializar caché para reproducción offline
     */
    private fun initializeCache() {
        val cacheDir = File(cacheDir, "media_cache")
        val cacheSize = 500L * 1024 * 1024 // 500 MB

        simpleCache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(cacheSize),
            androidx.media3.database.StandaloneDatabaseProvider(this)
        )
    }

    /**
     * Inicializar ExoPlayer con configuración óptima
     */
    private fun initializePlayer() {
        // Configurar data source factory con caché
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
            .setUpstreamDataSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent("Mozilla/5.0")
                    .setConnectTimeoutMs(10000)
                    .setReadTimeoutMs(10000)
            )
            .setCacheWriteDataSinkFactory(null) // Solo lectura de caché por ahora
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheDataSourceFactory)
            )
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        15000,  // Min buffer
                        50000,  // Max buffer
                        2500,   // Playback buffer
                        5000    // Playback rebuffer
                    )
                    .build()
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // Handle audio focus automatically
            )
            .setHandleAudioBecomingNoisy(true) // Pause cuando se desconectan auriculares
            .setWakeMode(C.WAKE_MODE_LOCAL) // Mantener CPU despierta durante reproducción
            .build()
            .apply {
                // Configurar listeners
                addListener(PlayerListener())

                // Configurar parámetros de reproducción
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = false
            }
    }

    /**
     * Inicializar MediaSession para notificaciones y Android Auto
     */
    private fun initializeMediaSession() {
        // Intent para abrir la app cuando se toca la notificación
        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(MediaSessionCallback())
            .build()
    }

    /**
     * Observar eventos del reproductor
     */
    private fun observePlayerEvents() {
        serviceScope.launch {
            // Actualizar estado cada 100ms durante reproducción
            while (isActive) {
                player?.let { p ->
                    if (p.isPlaying) {
                        updatePlaybackState(
                            currentPosition = p.currentPosition,
                            isPlaying = true,
                            isBuffering = p.playbackState == Player.STATE_BUFFERING
                        )
                    }
                }
                delay(100)
            }
        }
    }

    /**
     * Listener de eventos de ExoPlayer
     */
    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    updatePlaybackState(isBuffering = true)
                }
                Player.STATE_READY -> {
                    updatePlaybackState(
                        isBuffering = false,
                        duration = player?.duration ?: 0L
                    )
                }
                Player.STATE_ENDED -> {
                    handlePlaybackEnded()
                }
                Player.STATE_IDLE -> {
                    // No hacer nada
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState(isPlaying = isPlaying)

            // Actualizar notificación
            if (isPlaying) {
                // La notificación se mantiene automáticamente con MediaSession
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val playbackError = PlaybackError(
                code = error.errorCode,
                message = error.message ?: "Unknown error"
            )

            updatePlaybackState(error = playbackError)

            serviceScope.launch {
                _playerEvents.emit(PlayerEvent.Error(playbackError))
            }

            // Intentar saltar a la siguiente canción si hay error
            handlePlaybackError(error)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                val songId = it.mediaId
                currentQueue.getOrNull(currentIndex)?.let { song ->
                    if (song.id == songId) {
                        serviceScope.launch {
                            _playerEvents.emit(PlayerEvent.SongChanged(song))
                        }
                        updateCurrentSong(song)
                    }
                }
            }
        }
    }

    /**
     * Callback de MediaSession para manejar comandos
     */
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onPlay(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            player?.play()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onPause(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            player?.pause()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onSkipToNext(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            skipToNext()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onSkipToPrevious(session: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<SessionResult> {
            skipToPrevious()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onSeek(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            positionMs: Long
        ): ListenableFuture<SessionResult> {
            player?.seekTo(positionMs)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onSetRepeatMode(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            repeatMode: Int
        ): ListenableFuture<SessionResult> {
            player?.repeatMode = repeatMode
            updateRepeatMode(repeatMode)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onSetShuffleMode(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            shuffleMode: Int
        ): ListenableFuture<SessionResult> {
            setShuffleEnabled(shuffleMode == Player.SHUFFLE_MODE_ON)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    // ============================================================
    // Métodos públicos para controlar la reproducción
    // ============================================================

    /**
     * Reproducir una canción específica
     */
    fun playSong(song: Song) {
        serviceScope.launch {
            try {
                // Obtener URL de stream actualizada si es necesario
                val songWithStream = if (song.streamUrl == null || isStreamExpired(song)) {
                    playerRepository.refreshStreamUrl(song)
                } else {
                    song
                }

                // Crear MediaItem
                val mediaItem = songWithStream.toMediaItem()

                // Configurar reproductor
                player?.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    play()
                }

                currentQueue.clear()
                currentQueue.add(songWithStream)
                currentIndex = 0

                updateCurrentSong(songWithStream)

            } catch (e: Exception) {
                e.printStackTrace()
                val error = PlaybackError(
                    code = -1,
                    message = "Failed to play song: ${e.message}"
                )
                updatePlaybackState(error = error)
                _playerEvents.emit(PlayerEvent.Error(error))
            }
        }
    }

    /**
     * Reproducir una cola de canciones
     */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        serviceScope.launch {
            try {
                currentQueue.clear()
                currentQueue.addAll(songs)
                originalQueue.clear()
                originalQueue.addAll(songs)
                currentIndex = startIndex

                // Preparar MediaItems
                val mediaItems = songs.map { it.toMediaItem() }

                player?.apply {
                    setMediaItems(mediaItems, startIndex, 0)
                    prepare()
                    play()
                }

                songs.getOrNull(startIndex)?.let { updateCurrentSong(it) }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Pausar reproducción
     */
    fun pause() {
        player?.pause()
    }

    /**
     * Reanudar reproducción
     */
    fun play() {
        player?.play()
    }

    /**
     * Toggle play/pause
     */
    fun playPause() {
        player?.let {
            if (it.isPlaying) pause() else play()
        }
    }

    /**
     * Siguiente canción
     */
    fun skipToNext() {
        if (currentIndex < currentQueue.size - 1) {
            player?.seekToNext()
            currentIndex++
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            player?.seekTo(0, 0)
            currentIndex = 0
        }
    }

    /**
     * Canción anterior
     */
    fun skipToPrevious() {
        if (player?.currentPosition ?: 0 > 3000) {
            // Si llevamos más de 3 segundos, reiniciar canción actual
            player?.seekTo(0)
        } else if (currentIndex > 0) {
            player?.seekToPrevious()
            currentIndex--
        }
    }

    /**
     * Seek a posición específica
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    /**
     * Activar/desactivar shuffle
     */
    fun setShuffleEnabled(enabled: Boolean) {
        shuffleMode = enabled

        if (enabled) {
            // Guardar cola original y mezclar
            if (originalQueue.isEmpty()) {
                originalQueue.addAll(currentQueue)
            }

            val currentSong = currentQueue.getOrNull(currentIndex)
            currentQueue.shuffle()

            // Asegurar que la canción actual quede primera
            currentSong?.let { song ->
                currentQueue.remove(song)
                currentQueue.add(0, song)
                currentIndex = 0
            }
        } else {
            // Restaurar cola original
            if (originalQueue.isNotEmpty()) {
                val currentSong = currentQueue.getOrNull(currentIndex)
                currentQueue.clear()
                currentQueue.addAll(originalQueue)

                // Encontrar índice de la canción actual en la cola original
                currentSong?.let { song ->
                    currentIndex = currentQueue.indexOf(song)
                }
            }
        }

        updatePlaybackState(shuffleEnabled = enabled)
    }

    /**
     * Establecer modo de repetición
     */
    fun setRepeatMode(mode: RepeatMode) {
        player?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        updateRepeatMode(mode)
    }

    /**
     * Establecer velocidad de reproducción
     */
    fun setPlaybackSpeed(speed: Float) {
        player?.setPlaybackSpeed(speed)
        updatePlaybackState(playbackSpeed = speed)
    }

    /**
     * Agregar canciones a la cola
     */
    fun addToQueue(songs: List<Song>) {
        currentQueue.addAll(songs)

        val mediaItems = songs.map { it.toMediaItem() }
        player?.addMediaItems(mediaItems)

        updatePlaybackState(queue = currentQueue)
    }

    /**
     * Remover canción de la cola
     */
    fun removeFromQueue(index: Int) {
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            player?.removeMediaItem(index)

            if (index < currentIndex) {
                currentIndex--
            }

            updatePlaybackState(queue = currentQueue, currentIndex = currentIndex)
        }
    }

    // ============================================================
    // Métodos privados auxiliares
    // ============================================================

    private fun isStreamExpired(song: Song): Boolean {
        return song.streamUrlExpiry > 0 &&
                System.currentTimeMillis() > song.streamUrlExpiry
    }

    private fun updateCurrentSong(song: Song) {
        updatePlaybackState(currentSong = song)

        // Guardar en repositorio para persistencia
        serviceScope.launch {
            playerRepository.updateCurrentSong(song)
        }
    }

    private fun updatePlaybackState(
        currentSong: Song? = _playbackState.value.currentSong,
        isPlaying: Boolean = _playbackState.value.isPlaying,
        isBuffering: Boolean = _playbackState.value.isBuffering,
        currentPosition: Long = _playbackState.value.currentPosition,
        duration: Long = _playbackState.value.duration,
        repeatMode: RepeatMode = _playbackState.value.repeatMode,
        shuffleEnabled: Boolean = _playbackState.value.shuffleEnabled,
        queue: List<Song> = _playbackState.value.queue,
        currentIndex: Int = _playbackState.value.currentIndex,
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

    private fun updateRepeatMode(mode: RepeatMode) {
        updatePlaybackState(repeatMode = mode)
    }

    private fun updateRepeatMode(exoRepeatMode: Int) {
        val mode = when (exoRepeatMode) {
            Player.REPEAT_MODE_OFF -> RepeatMode.OFF
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }
        updateRepeatMode(mode)
    }

    private fun handlePlaybackEnded() {
        serviceScope.launch {
            _playerEvents.emit(PlayerEvent.PlaybackEnded)
        }

        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                player?.seekTo(0)
                player?.play()
            }
            RepeatMode.ALL -> {
                if (currentIndex >= currentQueue.size - 1) {
                    player?.seekTo(0, 0)
                    currentIndex = 0
                } else {
                    skipToNext()
                }
            }
            RepeatMode.OFF -> {
                // No hacer nada, dejar que termine
            }
        }
    }

    private fun handlePlaybackError(error: PlaybackException) {
        // Intentar siguiente canción después de un error
        serviceScope.launch {
            delay(1000) // Esperar 1 segundo antes de intentar
            if (currentIndex < currentQueue.size - 1) {
                skipToNext()
            }
        }
    }

    // ============================================================
    // Lifecycle de MediaSessionService
    // ============================================================

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Si no está reproduciendo, detener el servicio
        if (player?.isPlaying != true) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Guardar estado antes de destruir
        serviceScope.launch {
            saveState()
        }

        // Liberar recursos
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }

        player = null

        simpleCache?.release()
        simpleCache = null

        super.onDestroy()
    }

    private suspend fun saveState() {
        _playbackState.value.let { state ->
            playerRepository.savePlaybackState(state)
        }
    }
}