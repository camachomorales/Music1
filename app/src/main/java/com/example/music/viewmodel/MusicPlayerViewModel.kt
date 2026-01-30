package com.example.music.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.cache.SongCacheManager
import com.example.music.data.cache.StreamUrlCache
import com.example.music.data.model.AppMode
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.MusicRepository
import com.example.music.data.repository.StreamingMusicRepository
import com.example.music.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.music.data.api.MusicProviderType

class MusicPlayerViewModel(
    application: Application,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {

    private val TAG = "MusicPlayerVM"
    private val streamingRepository = StreamingMusicRepository()
    private val streamUrlCache = StreamUrlCache(streamingRepository)
    private var musicService: MusicService? = null
    private var serviceBound = false
    private var searchJob: Job? = null

    private val songCacheManager = SongCacheManager(application)

    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                "com.example.music.ACTION_SKIP_NEXT" -> skipToNext()
                "com.example.music.ACTION_SKIP_PREVIOUS" -> skipToPrevious()
                "com.example.music.ACTION_TOGGLE_SHUFFLE" -> toggleShuffle()
                "com.example.music.ACTION_CYCLE_REPEAT" -> cycleRepeatMode()
            }
        }
    }

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _streamingSongs = MutableStateFlow<List<StreamingSong>>(emptyList())
    val streamingSongs: StateFlow<List<StreamingSong>> = _streamingSongs.asStateFlow()

    private val _localSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val localSearchResults: StateFlow<List<Song>> = _localSearchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _isOnlineMode = MutableStateFlow(false)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _appMode = MutableStateFlow(AppMode.OFFLINE)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private val _originalQueue = mutableListOf<Song>()
    private val _currentIndex = MutableStateFlow(0)

    private val streamingSongCache = mutableMapOf<String, StreamingSong>()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true
            Log.d(TAG, "✅ Servicio conectado")
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
            Log.d(TAG, "❌ Servicio desconectado")
        }
    }

    init {
        Log.d(TAG, "🚀🚀🚀 VIEWMODEL INIT COMENZADO 🚀🚀🚀")
        bindMusicService()
        loadLocalSongs()
        registerNotificationReceiver()
        Log.d(TAG, "🎵 ViewModel inicializado completamente")
    }

    private fun registerNotificationReceiver() {
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.music.ACTION_SKIP_NEXT")
            addAction("com.example.music.ACTION_SKIP_PREVIOUS")
            addAction("com.example.music.ACTION_TOGGLE_SHUFFLE")
            addAction("com.example.music.ACTION_CYCLE_REPEAT")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                notificationReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().registerReceiver(
                notificationReceiver,
                filter,
                android.content.Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(notificationReceiver, filter)
        }

        Log.d(TAG, "📻 BroadcastReceiver registrado")
    }

    private fun bindMusicService() {
        try {
            val intent = Intent(getApplication(), MusicService::class.java)

            // ✅ Usar startForegroundService en Android 8+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }

            getApplication<Application>().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.d(TAG, "🔗 Binding servicio")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error binding servicio", e)
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            musicService?.let { service ->
                launch { service.getCurrentSong().collect { _currentSong.value = it } }
                launch { service.getIsPlaying().collect { _isPlaying.value = it } }
                launch { service.getCurrentPosition().collect { _currentPosition.value = it } }
                launch { service.getDuration().collect { _duration.value = it } }
                launch { service.getRepeatMode().collect { _repeatMode.value = it } }
                launch { service.getShuffleEnabled().collect { _shuffleEnabled.value = it } }
                launch { service.getIsBuffering().collect { _isBuffering.value = it } }
            }
        }
    }

    fun loadLocalSongs() {
        viewModelScope.launch {
            try {
                val localSongs = musicRepository.getSongsFromDevice()
                _songs.value = localSongs
                _originalQueue.clear()
                _originalQueue.addAll(localSongs)
                _queue.value = localSongs
                Log.d(TAG, "📱 ${localSongs.size} canciones locales cargadas")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando canciones: ${e.message}", e)
            }
        }
    }

    fun playSong(song: Song, playlist: List<Song> = _songs.value) {
        Log.d(TAG, "▶️▶️▶️ playSong() LLAMADO ▶️▶️▶️")
        Log.d(TAG, "▶️ Canción: ${song.title}")
        Log.d(TAG, "▶️ Path: ${song.path}")
        Log.d(TAG, "▶️ isStreaming: ${song.isStreaming}")

        if (song.isStreaming && song.path.startsWith("streaming://")) {
            Log.d(TAG, "🌐 Detectado path streaming://, llamando playStreamingSongFromPath()")
            playStreamingSongFromPath(song, playlist)
            return
        }

        Log.d(TAG, "📀 Reproducción normal (no streaming://)")
        _originalQueue.clear()
        _originalQueue.addAll(playlist)

        val queue = if (_shuffleEnabled.value && _repeatMode.value != RepeatMode.ONE) {
            val shuffled = playlist.toMutableList()
            shuffled.shuffle()
            shuffled.remove(song)
            listOf(song) + shuffled
        } else {
            playlist
        }

        _queue.value = queue
        _currentIndex.value = queue.indexOf(song)

        Log.d(TAG, "🎵 Enviando a musicService.playSongWithQueue()")
        musicService?.playSongWithQueue(song, queue)
    }

    private fun playStreamingSongFromPath(song: Song, playlist: List<Song>) {
        Log.d(TAG, "🌐🌐🌐 playStreamingSongFromPath() INICIADO (ResolvingDataSource) 🌐🌐🌐")

        viewModelScope.launch {
            try {
                _isLoadingStream.value = true
                Log.d(TAG, "🔍 Path original: ${song.path}")

                val parts = song.path.removePrefix("streaming://").split("/")
                Log.d(TAG, "🔍 Parts: $parts")

                if (parts.size < 2) {
                    Log.e(TAG, "❌ Path inválido (partes < 2): ${song.path}")
                    _isLoadingStream.value = false
                    Toast.makeText(getApplication(), "Invalid streaming path", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val providerName = parts[0]
                val songId = parts[1]

                Log.d(TAG, "🎯 Provider name: $providerName")
                Log.d(TAG, "🎯 Song ID: $songId")

                val provider = when (providerName.lowercase()) {
                    "youtube" -> MusicProviderType.INNERTUBE
                    "innertube" -> MusicProviderType.INNERTUBE
                    "youtube_music" -> MusicProviderType.YOUTUBE_MUSIC
                    "jiosaavn" -> MusicProviderType.JIOSAAVN
                    "spotify" -> MusicProviderType.SPOTIFY
                    else -> {
                        Log.e(TAG, "❌ Provider desconocido: $providerName")
                        _isLoadingStream.value = false
                        Toast.makeText(getApplication(), "Unknown provider: $providerName", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                Log.d(TAG, "✅ Provider encontrado: ${provider.displayName}")

                // ✅ Crear StreamingSong
                val streamingSong = StreamingSong(
                    id = songId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration,
                    thumbnailUrl = song.albumArtUri,
                    provider = provider
                )

                // ✅ Registrar StreamingSong en MusicService ANTES de reproducir
                Log.d(TAG, "📝 Registrando StreamingSong en MusicService...")
                musicService?.registerStreamingSong(songId, streamingSong)

                // ✅ Crear Song con path streaming:// (ResolvingDataSource lo resolverá)
                val streamingSongWithPath = song.copy(
                    path = "streaming://${providerName}/${songId}",
                    isStreaming = true,
                    streamingId = songId,
                    streamingProvider = provider.name
                )

                Log.d(TAG, "✅ Song con URI streaming:// creado: ${streamingSongWithPath.path}")

                _originalQueue.clear()
                _originalQueue.addAll(playlist)

                val queue = if (_shuffleEnabled.value && _repeatMode.value != RepeatMode.ONE) {
                    val shuffled = playlist.toMutableList()
                    shuffled.shuffle()
                    shuffled.remove(song)
                    listOf(streamingSongWithPath) + shuffled
                } else {
                    playlist.map {
                        if (it.id == song.id) streamingSongWithPath else it
                    }
                }

                _queue.value = queue
                _currentIndex.value = queue.indexOf(streamingSongWithPath)

                Log.d(TAG, "🎵 Enviando a musicService.playSongWithQueue() con URI streaming://")
                Log.d(TAG, "🎵 ResolvingDataSource resolverá la URL automáticamente")
                musicService?.playSongWithQueue(streamingSongWithPath, queue)

                _isLoadingStream.value = false

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ EXCEPCIÓN EN playStreamingSongFromPath ❌❌❌", e)
                Log.e(TAG, "Mensaje: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                _isLoadingStream.value = false
                Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playStreamingSong(streamingSong: StreamingSong) {
        Log.d(TAG, "🎵🎵🎵 playStreamingSong() LLAMADO (ResolvingDataSource) 🎵🎵🎵")
        Log.d(TAG, "🎵 Título: ${streamingSong.title}")
        Log.d(TAG, "🎵 ID: ${streamingSong.id}")
        Log.d(TAG, "🎵 Provider: ${streamingSong.provider.displayName}")

        viewModelScope.launch {
            try {
                _isLoadingStream.value = true
                val songId = streamingSong.id

                // ✅ Verificar cache
                Log.d(TAG, "🔍 Verificando cache para: $songId")
                val isCached = songCacheManager.isCached(songId)
                Log.d(TAG, "📦 ¿En cache?: $isCached")

                if (isCached) {
                    val cachedPath = songCacheManager.getCachedPath(songId)
                    Log.d(TAG, "⚡ Cache path: $cachedPath")

                    if (cachedPath != null) {
                        Log.d(TAG, "⚡⚡⚡ CACHE HIT - Reproduciendo desde archivo ⚡⚡⚡")

                        val song = Song(
                            id = streamingSong.id.hashCode().toLong(),
                            title = streamingSong.title,
                            artist = streamingSong.artist,
                            album = streamingSong.album?.takeIf { it.isNotBlank() }
                                ?: streamingSong.artist,
                            duration = streamingSong.duration,
                            path = cachedPath,
                            albumArtUri = streamingSong.thumbnailUrl,
                            isStreaming = false,
                            streamingId = streamingSong.id,
                            streamingProvider = streamingSong.provider.name
                        )

                        Log.d(TAG, "📀 Llamando playSong() con archivo en cache")
                        playSong(song, listOf(song))
                        _isLoadingStream.value = false
                        return@launch
                    }
                }

                // ✅ Streaming con ResolvingDataSource
                Log.d(TAG, "🌐🌐🌐 CACHE MISS - Usando ResolvingDataSource 🌐🌐🌐")

                // ✅ Registrar StreamingSong en MusicService
                Log.d(TAG, "📝 Registrando StreamingSong en MusicService...")
                musicService?.registerStreamingSong(songId, streamingSong)

                // ✅ Crear Song con URI streaming://
                val providerName = streamingSong.provider.name.lowercase()
                val streamingUri = "streaming://${providerName}/${songId}"

                Log.d(TAG, "✅ URI streaming:// creado: $streamingUri")

                val song = Song(
                    id = streamingSong.id.hashCode().toLong(),
                    title = streamingSong.title,
                    artist = streamingSong.artist,
                    album = streamingSong.album?.takeIf { it.isNotBlank() }
                        ?: streamingSong.artist,
                    duration = streamingSong.duration,
                    path = streamingUri,
                    albumArtUri = streamingSong.thumbnailUrl,
                    isStreaming = true,
                    streamingId = streamingSong.id,
                    streamingProvider = streamingSong.provider.name
                )

                Log.d(TAG, "🎵 Llamando playSong() con URI streaming://")
                Log.d(TAG, "🎵 ResolvingDataSource resolverá la URL automáticamente")
                playSong(song, listOf(song))
                _isLoadingStream.value = false

                // ✅ Cache en background (después de obtener URL)
                launch(Dispatchers.IO) {
                    delay(3000) // Esperar a que empiece a reproducir
                    try {
                        Log.d(TAG, "📥 Iniciando cache en background...")

                        // Obtener URL real para cachear
                        val resolvedUrl = streamingRepository.getStreamUrl(streamingSong)

                        if (resolvedUrl != null) {
                            songCacheManager.downloadToCache(
                                url = resolvedUrl,
                                songId = songId,
                                onProgress = { progress ->
                                    if (progress % 25 == 0) {
                                        Log.d(TAG, "📥 Cache: ${streamingSong.title} - $progress%")
                                    }
                                }
                            )
                            Log.d(TAG, "✅ Cacheado exitoso")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Cache falló: ${e.message}", e)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌❌❌ EXCEPCIÓN EN playStreamingSong ❌❌❌", e)
                Log.e(TAG, "Mensaje: ${e.message}")
                Log.e(TAG, "Stack trace:", e)
                _isLoadingStream.value = false
                Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            songCacheManager.clearAll()
            Log.d(TAG, "🧹 Cache limpiado")
        }
    }

    fun getCacheInfo(): Pair<Int, Long> {
        val count = songCacheManager.getCachedSongsCount()
        val size = songCacheManager.getCacheSize()
        return Pair(count, size)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            musicService?.pause()
        } else {
            musicService?.play()
        }
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    fun seekTo(position: Float) {
        val seekPosition = (position * _duration.value).toLong()
        musicService?.seekTo(seekPosition)
    }

    fun skipToNext() {
        val currentMode = _repeatMode.value
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.w(TAG, "⚠️ Cola vacía")
            return
        }

        when (currentMode) {
            RepeatMode.ONE -> playCurrentSong()
            RepeatMode.ALL -> {
                _currentIndex.value = (_currentIndex.value + 1) % queue.size
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value < queue.size - 1) {
                    _currentIndex.value += 1
                    playCurrentSong()
                } else {
                    musicService?.pause()
                    _isPlaying.value = false
                }
            }
        }
    }

    fun skipToPrevious() {
        val currentMode = _repeatMode.value
        val queue = _queue.value

        if (queue.isEmpty()) {
            Log.w(TAG, "⚠️ Cola vacía")
            return
        }

        when (currentMode) {
            RepeatMode.ONE -> playCurrentSong()
            RepeatMode.ALL -> {
                _currentIndex.value = if (_currentIndex.value == 0) {
                    queue.size - 1
                } else {
                    _currentIndex.value - 1
                }
                playCurrentSong()
            }
            RepeatMode.OFF -> {
                if (_currentIndex.value > 0) {
                    _currentIndex.value -= 1
                    playCurrentSong()
                } else {
                    musicService?.seekTo(0)
                    _currentPosition.value = 0L
                }
            }
        }
    }

    fun toggleShuffle() {
        val newState = !_shuffleEnabled.value
        _shuffleEnabled.value = newState
        musicService?.setShuffle(newState)

        if (_repeatMode.value == RepeatMode.ONE) return

        val currentSong = _currentSong.value
        if (currentSong != null && _originalQueue.isNotEmpty()) {
            val newQueue = if (newState) {
                val shuffled = _originalQueue.toMutableList()
                shuffled.shuffle()
                shuffled.remove(currentSong)
                listOf(currentSong) + shuffled
            } else {
                _originalQueue.toList()
            }

            _queue.value = newQueue
            val newIndex = newQueue.indexOf(currentSong)
            _currentIndex.value = newIndex
            musicService?.updateQueue(newQueue, newIndex)
        }
    }

    fun cycleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        musicService?.setRepeat(newMode)

        if (newMode != RepeatMode.ONE && _shuffleEnabled.value && _originalQueue.isNotEmpty()) {
            val currentSong = _currentSong.value
            if (currentSong != null) {
                val shuffled = _originalQueue.toMutableList()
                shuffled.shuffle()
                shuffled.remove(currentSong)
                val newQueue = listOf(currentSong) + shuffled
                _queue.value = newQueue
                _currentIndex.value = 0
                musicService?.updateQueue(newQueue, 0)
            }
        }
    }

    private fun playCurrentSong() {
        val queue = _queue.value

        if (_currentIndex.value < 0 || _currentIndex.value >= queue.size) {
            _currentIndex.value = 0
            if (queue.isEmpty()) return
        }

        val song = queue[_currentIndex.value]

        // ✅ Si es streaming://, necesitamos registrar el StreamingSong
        if (song.path.startsWith("streaming://") && song.streamingId != null) {
            // Buscar en cache
            val streamingSong = streamingSongCache[song.streamingId!!]
            if (streamingSong != null) {
                musicService?.registerStreamingSong(song.streamingId!!, streamingSong)
            }
        }

        _currentSong.value = song
        musicService?.playSongWithQueue(song, queue)
        _isPlaying.value = true
    }

    fun toggleOnlineMode() {
        _isOnlineMode.value = !_isOnlineMode.value
        _appMode.value = if (_isOnlineMode.value) AppMode.STREAMING else AppMode.OFFLINE

        if (!_isOnlineMode.value) {
            loadLocalSongs()
            clearSearchCompletely()
            streamingSongCache.clear()
        } else {
            getTrending()
        }
    }

    fun toggleAppMode() = toggleOnlineMode()

    fun getTrending() {
        viewModelScope.launch {
            try {
                val trending = streamingRepository.getTrending(limit = 30)
                _streamingSongs.value = trending
                trending.forEach { streamingSongCache[it.id] = it }
                Log.d(TAG, "✅ ${trending.size} trending")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error trending: ${e.message}", e)
                _streamingSongs.value = emptyList()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            clearSearchResults()
        } else {
            if (_isOnlineMode.value) {
                searchStreamingSongs(query)
            } else {
                searchLocalSongs(query)
            }
        }
    }

    private fun clearSearchResults() {
        searchJob?.cancel()
        _streamingSongs.value = emptyList()
        _localSearchResults.value = emptyList()
        _isSearching.value = false
    }

    fun clearSearchCompletely() {
        _searchQuery.value = ""
        clearSearchResults()
    }

    fun searchLocalSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _localSearchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)

            try {
                fun normalize(text: String): String {
                    return text.trim()
                        .lowercase()
                        .replace(Regex("\\s+"), " ")
                        .replace(Regex("[^a-z0-9\\s]"), "")
                }

                val normalizedQuery = normalize(query)
                val allSongs = _songs.value

                val results = allSongs.filter { song ->
                    val title = normalize(song.title)
                    val artist = normalize(song.artist)
                    val album = normalize(song.album)

                    title.contains(normalizedQuery) ||
                            artist.contains(normalizedQuery) ||
                            album.contains(normalizedQuery)
                }

                _localSearchResults.value = results
                Log.d(TAG, "🔍 Búsqueda local: '$query' → ${results.size} resultados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda local: ${e.message}", e)
                _localSearchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun searchStreamingSongs(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _streamingSongs.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(800)

            try {
                Log.d(TAG, "🔍 Búsqueda streaming: '$query'")
                val results = streamingRepository.search(query, limit = 30)
                _streamingSongs.value = results
                results.forEach { streamingSongCache[it.id] = it }
                Log.d(TAG, "✅ ${results.size} resultados")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error búsqueda streaming: ${e.message}", e)
                _streamingSongs.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        clearSearchCompletely()
    }

    fun cacheStreamingSong(streamingSong: StreamingSong) {
        streamingSongCache[streamingSong.id] = streamingSong
        Log.d(TAG, "💾 StreamingSong cacheado: ${streamingSong.id} - ${streamingSong.title}")
    }

    fun playMixedPlaylist(
        song: Song,
        playlist: List<Song>,
        streamingSongs: List<StreamingSong>,
        playlistName: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎵 Reproduciendo playlist mixta: ${song.title}, playlist: $playlistName")

                streamingSongs.forEach { streamingSongCache[it.id] = it }

                val playlistWithAlbum = if (!playlistName.isNullOrBlank()) {
                    playlist.map { it.copy(album = playlistName) }
                } else {
                    playlist
                }

                _originalQueue.clear()
                _originalQueue.addAll(playlistWithAlbum)
                _queue.value = playlistWithAlbum

                val startIndex = playlistWithAlbum.indexOfFirst { it.id == song.id }
                if (startIndex != -1) {
                    _currentIndex.value = startIndex
                }

                val songWithPlaylistName = if (!playlistName.isNullOrBlank()) {
                    song.copy(album = playlistName)
                } else {
                    song
                }

                if (songWithPlaylistName.isStreaming && songWithPlaylistName.path.startsWith("streaming://")) {
                    playStreamingSongFromPath(songWithPlaylistName, playlistWithAlbum)
                } else {
                    playSong(songWithPlaylistName, playlistWithAlbum)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error reproduciendo playlist mixta", e)
            }
        }
    }

    fun toggleFavoriteBySongId(songId: Long) {
        val songIdStr = songId.toString()
        val currentFavorites = _favorites.value.toMutableSet()
        if (currentFavorites.contains(songIdStr)) {
            currentFavorites.remove(songIdStr)
        } else {
            currentFavorites.add(songIdStr)
        }
        _favorites.value = currentFavorites
    }

    fun toggleFavorite(songId: Long) {
        toggleFavoriteBySongId(songId)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "🧹🧹🧹 onCleared() LLAMADO 🧹🧹🧹")
        searchJob?.cancel()
        streamUrlCache.cleanup()

        try {
            getApplication<Application>().unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }

        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
        Log.d(TAG, "🧹 ViewModel limpiado completamente")
    }
}