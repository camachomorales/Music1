/*package com.example.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.StreamingMusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URL

class MusicService : Service() {

    private val TAG = "MusicService"
    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicBinder()

    // ✅ Cache de ExoPlayer (100MB)
    private lateinit var simpleCache: SimpleCache

    // ✅ Repositorio para obtener URLs frescas
    private val streamingRepository by lazy { StreamingMusicRepository() }

    // ✅ Cache de StreamingSong para poder regenerar URLs
    // Guardamos el StreamingSong original para poder obtener URLs frescas
    private val streamingSongCache = mutableMapOf<String, StreamingSong>()

    // Estados
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    private val _shuffleEnabled = MutableStateFlow(false)
    private val _isBuffering = MutableStateFlow(false)

    // Cola de reproducción
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private var currentIndex = 0
    private var originalQueue = listOf<Song>()

    private val handler = Handler(Looper.getMainLooper())
    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration.coerceAtLeast(0)

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastNotificationUpdate > 1000) {
                            _currentSong.value?.let { updateNotification(it, true) }
                            lastNotificationUpdate = currentTime
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando posición: ${e.message}")
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    private var lastNotificationUpdate = 0L

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun getCurrentSong(): StateFlow<Song?> = _currentSong.asStateFlow()
    fun getIsPlaying(): StateFlow<Boolean> = _isPlaying.asStateFlow()
    fun getCurrentPosition(): StateFlow<Long> = _currentPosition.asStateFlow()
    fun getDuration(): StateFlow<Long> = _duration.asStateFlow()
    fun getRepeatMode(): StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    fun getShuffleEnabled(): StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    fun getIsBuffering(): StateFlow<Boolean> = _isBuffering.asStateFlow()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeExoPlayer()
        Log.d(TAG, "✅ MusicService creado con ExoPlayer + ResolvingDataSource")
    }

    /**
     * ✨ SOLUCIÓN INNERTUNE: ResolvingDataSource
     * Esta función se ejecuta CADA VEZ que ExoPlayer necesita datos
     * Si la URL expiró → Se obtiene automáticamente una nueva
     */
    @OptIn(UnstableApi::class)
    private fun createResolvingDataSourceFactory(): ResolvingDataSource.Factory {
        val baseDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(16000)
            .setReadTimeoutMs(8000)
            .setAllowCrossProtocolRedirects(true)

        return ResolvingDataSource.Factory(baseDataSourceFactory) { dataSpec ->
            val uri = dataSpec.uri
            val uriString = uri.toString()

            Log.d(TAG, "🔍 ResolvingDataSource: Procesando URI: $uriString")

            // ✅ Solo resolver si es una URL de streaming de YouTube
            if (!uriString.startsWith("streaming://youtube/")) {
                Log.d(TAG, "✅ URI normal (no streaming), pasando directo")
                return@Factory dataSpec
            }

            // ✅ Extraer video ID del URI
            val videoId = uriString.removePrefix("streaming://youtube/")

            // ✅ Buscar el StreamingSong en el cache
            val streamingSong = streamingSongCache[videoId]

            if (streamingSong == null) {
                Log.e(TAG, "❌ ResolvingDataSource: StreamingSong no encontrado en cache para videoId=$videoId")
                throw IOException("StreamingSong no encontrado en cache para videoId=$videoId")
            }

            Log.d(TAG, "🔄 ResolvingDataSource: Obteniendo URL fresca para: ${streamingSong.title}")

            try {
                // ✅ OBTENER URL FRESCA usando el StreamingSong completo
                val freshUrl = runBlocking {
                    withContext(Dispatchers.IO) {
                        streamingRepository.getStreamUrl(streamingSong)
                    }
                }

                if (freshUrl != null) {
                    Log.d(TAG, "✅ ResolvingDataSource: URL fresca obtenida: ${freshUrl.take(100)}...")
                    dataSpec.withUri(Uri.parse(freshUrl))
                } else {
                    Log.e(TAG, "❌ ResolvingDataSource: No se pudo obtener URL")
                    throw IOException("No se pudo obtener URL para: ${streamingSong.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ ResolvingDataSource error: ${e.message}", e)
                throw IOException("Error obteniendo URL: ${e.message}", e)
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializeExoPlayer() {
        // ✅ Configurar cache (100MB)
        val cacheSize = 100L * 1024 * 1024 // 100MB
        val cacheDir = File(cacheDir, "exoplayer-cache")
        simpleCache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(cacheSize),
            androidx.media3.database.StandaloneDatabaseProvider(this)
        )

        // ✅ ResolvingDataSource para streaming de YouTube (URLs auto-renovables)
        val resolvingDataSourceFactory = createResolvingDataSourceFactory()

        // ✅ DefaultDataSource para archivos locales (file://, content://)
        val defaultDataSourceFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(3000)
                .setReadTimeoutMs(3000)
                .setAllowCrossProtocolRedirects(true)
        )

        // ✅ Combinar ambos: ResolvingDataSource para streaming, Default para local
        val upstreamFactory = DefaultDataSource.Factory(
            this,
            resolvingDataSourceFactory
        )

        // ✅ DataSource con cache
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // ✅ ExoPlayer optimizado
        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        500,    // minBufferMs - 0.5s
                        5000,   // maxBufferMs - 5s
                        200,    // bufferForPlaybackMs - 0.2s para empezar ⚡
                        500     // bufferForPlaybackAfterRebufferMs
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(cacheDataSourceFactory)
            )
            .build()

        // ✅ Listeners de ExoPlayer
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "⏳ Buffering...")
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        Log.d(TAG, "✅ Listo para reproducir")
                        _isBuffering.value = false
                        _duration.value = exoPlayer?.duration ?: 0L
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "🎵 Canción terminó")
                        handleAutoCompletion()
                    }
                    Player.STATE_IDLE -> {
                        Log.d(TAG, "💤 Idle")
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    handler.post(updatePositionRunnable)
                } else {
                    handler.removeCallbacks(updatePositionRunnable)
                }
                _currentSong.value?.let { updateNotification(it, isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "❌ ExoPlayer error: ${error.message}", error)

                // ✅ Detectar error 403 (URL expirada)
                val cause = error.cause
                if (cause is HttpDataSource.InvalidResponseCodeException) {
                    if (cause.responseCode == 403) {
                        val currentSong = _currentSong.value
                        if (currentSong?.isStreaming == true) {
                            Log.w(TAG, "⚠️ Error 403 detectado - URL expirada")
                            Log.w(TAG, "💡 ResolvingDataSource debería regenerar automáticamente en el próximo intento")
                            // ResolvingDataSource debería manejar esto automáticamente en el próximo request
                            return
                        }
                    }
                }

                _isPlaying.value = false
                _isBuffering.value = false
                _currentSong.value?.let { updateNotification(it, false) }
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📢 onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d(TAG, "⏯️ ACTION_PLAY_PAUSE desde notificación")
                playPause()
                handler.postDelayed({
                    _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
                }, 100)
            }
            ACTION_NEXT -> {
                Log.d(TAG, "⏭️ ACTION_NEXT desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
            }
            ACTION_PREVIOUS -> {
                Log.d(TAG, "⏮️ ACTION_PREVIOUS desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_SKIP_PREVIOUS"))
            }
            ACTION_SHUFFLE -> {
                Log.d(TAG, "🔀 ACTION_SHUFFLE desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_TOGGLE_SHUFFLE"))
            }
            ACTION_REPEAT -> {
                Log.d(TAG, "🔁 ACTION_REPEAT desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_CYCLE_REPEAT"))
            }
            ACTION_DISMISS -> {
                Log.d(TAG, "🗑️ ACTION_DISMISS")
                dismissNotificationAndStop()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    // ==================== PLAYBACK ====================

    fun playSongWithQueue(song: Song, queue: List<Song>) {
        Log.d(TAG, "🎵 playSongWithQueue: ${song.title}, queue: ${queue.size}")
        originalQueue = queue
        _queue.value = queue
        currentIndex = queue.indexOf(song)
        playSong(song)
    }

    /**
     * ✅ Función para registrar StreamingSong en cache antes de reproducir
     * Esto permite que ResolvingDataSource pueda regenerar URLs
     */
    fun registerStreamingSong(videoId: String, streamingSong: StreamingSong) {
        streamingSongCache[videoId] = streamingSong
        Log.d(TAG, "📝 StreamingSong registrado en cache: $videoId -> ${streamingSong.title}")
    }

    fun play() {
        Log.d(TAG, "▶️ play()")
        try {
            exoPlayer?.play()
            _isPlaying.value = true
            handler.post(updatePositionRunnable)

            _currentSong.value?.let {
                updateNotification(it, true)
                Log.d(TAG, "🔔 Notificación actualizada")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al reproducir", e)
        }
    }

    private fun playSong(song: Song) {
        try {
            Log.d(TAG, "▶️ playSong: ${song.title} (isStreaming: ${song.isStreaming})")
            Log.d(TAG, "📂 Path: ${song.path}")
            _currentSong.value = song

            // ✅ IMPORTANTE: Para streaming, el path ya debe ser "streaming://youtube/VIDEO_ID"
            // ResolvingDataSource lo convertirá automáticamente en URL fresca
            val uri = when {
                song.path.startsWith("streaming://youtube/") -> {
                    Log.d(TAG, "🌐 URI de streaming (ResolvingDataSource lo manejará): ${song.path}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("http") -> {
                    Log.d(TAG, "🌐 URL HTTP/HTTPS directa: ${song.path}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("content://") -> {
                    Log.d(TAG, "📱 Content URI: ${song.path}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("/") -> {
                    Log.d(TAG, "📁 Archivo local: file://${song.path}")
                    Uri.parse("file://${song.path}")
                }
                else -> {
                    Log.d(TAG, "❓ URI desconocido, parseando directo: ${song.path}")
                    Uri.parse(song.path)
                }
            }

            // ✅ Crear MediaItem para ExoPlayer
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .build()

            // ✅ Configurar y reproducir
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare() // ⚡ ExoPlayer prepara en background
                play()    // ⚡ Empieza apenas tenga buffer mínimo
            }

            Log.d(TAG, "🎵 ExoPlayer preparando: ${song.title}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing song", e)
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    fun pause() {
        Log.d(TAG, "⏸️ pause()")
        try {
            exoPlayer?.pause()
            _isPlaying.value = false
            handler.removeCallbacks(updatePositionRunnable)
            stopForeground(STOP_FOREGROUND_DETACH)

            _currentSong.value?.let { updateNotification(it, false) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al pausar", e)
        }
    }

    fun playPause() {
        Log.d(TAG, "⏯️ playPause() - isPlaying: ${_isPlaying.value}")
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(position: Long) {
        Log.d(TAG, "🎯 Seeking to: ${formatTime(position)}")
        exoPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    // ==================== AUTO COMPLETION ====================

    private fun handleAutoCompletion() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        Log.d(TAG, "🎵 Auto-completion: repeat=${_repeatMode.value}, index=$currentIndex/${queue.size}")

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                playSong(queue[currentIndex])
                Log.d(TAG, "🔂 Repeat One (auto) → Repitiendo ${queue[currentIndex].title}")
            }
            RepeatMode.ALL -> {
                currentIndex = (currentIndex + 1) % queue.size
                playSong(queue[currentIndex])
                Log.d(TAG, "🔁 Repeat All (auto) → [$currentIndex/${queue.size}] ${queue[currentIndex].title}")
            }
            RepeatMode.OFF -> {
                if (currentIndex < queue.size - 1) {
                    currentIndex++
                    playSong(queue[currentIndex])
                    Log.d(TAG, "▶️ Normal (auto) → [$currentIndex/${queue.size}] ${queue[currentIndex].title}")
                } else {
                    Log.d(TAG, "🛑 Final de la cola - Deteniendo")
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    // ==================== SHUFFLE & REPEAT ====================

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        Log.d(TAG, "🔀 setShuffle: $enabled")
        _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
    }

    fun updateQueue(newQueue: List<Song>, newIndex: Int) {
        _queue.value = newQueue
        currentIndex = newIndex
        Log.d(TAG, "📝 Cola actualizada: ${newQueue.size} canciones, índice: $newIndex")
    }

    fun setRepeat(mode: RepeatMode) {
        _repeatMode.value = mode
        Log.d(TAG, "🔁 setRepeat: $mode")
        _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
    }

    // ==================== NOTIFICATION ====================

    private fun dismissNotificationAndStop() {
        Log.d(TAG, "🗑️ Deslizando notificación")

        try {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player", e)
        }

        handler.removeCallbacks(updatePositionRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "✅ Notificación eliminada")
    }

    private suspend fun loadAlbumArtBitmap(albumArtUri: String?): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (albumArtUri.isNullOrEmpty()) return@withContext null
                if (albumArtUri.startsWith("http")) {
                    val connection = URL(albumArtUri).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    BitmapFactory.decodeStream(connection.getInputStream())
                } else {
                    contentResolver.openInputStream(Uri.parse(albumArtUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun createRoundedBitmapWithBorder(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)
        val cornerRadius = 24f

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = android.graphics.Color.parseColor("#4DFFFFFF")
        }
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
        return output
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this, 100, Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getService(
            this, 101, Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val previousPendingIntent = PendingIntent.getService(
            this, 102, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val shufflePendingIntent = PendingIntent.getService(
            this, 103, Intent(this, MusicService::class.java).setAction(ACTION_SHUFFLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val repeatPendingIntent = PendingIntent.getService(
            this, 104, Intent(this, MusicService::class.java).setAction(ACTION_REPEAT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)
        collapsedView.setTextViewText(R.id.tv_song_title, song.title)
        collapsedView.setImageViewResource(R.id.btn_play_pause, if (isPlaying) R.drawable.ic_pause_dark else R.drawable.ic_play_dark)
        collapsedView.setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)
        collapsedView.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
        collapsedView.setOnClickPendingIntent(R.id.btn_previous, previousPendingIntent)

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
        expandedView.setTextViewText(R.id.tv_song_title_big, song.title)
        expandedView.setTextViewText(R.id.tv_artist_big, song.artist)
        val progress = if (_duration.value > 0) (_currentPosition.value * 100 / _duration.value).toInt() else 0
        expandedView.setProgressBar(R.id.progress_bar, 100, progress, false)
        expandedView.setTextViewText(R.id.tv_current_time, formatTime(_currentPosition.value))
        expandedView.setTextViewText(R.id.tv_total_time, formatTime(_duration.value))
        expandedView.setImageViewResource(R.id.btn_play_pause_big, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        expandedView.setImageViewResource(R.id.btn_shuffle, if (_shuffleEnabled.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
        val repeatIcon = when (_repeatMode.value) {
            RepeatMode.OFF -> R.drawable.ic_repeat
            RepeatMode.ALL -> R.drawable.ic_repeat_all
            RepeatMode.ONE -> R.drawable.ic_repeat_one
        }
        expandedView.setImageViewResource(R.id.btn_repeat, repeatIcon)
        expandedView.setOnClickPendingIntent(R.id.btn_previous_big, previousPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_play_pause_big, playPausePendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_next_big, nextPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_shuffle, shufflePendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_repeat, repeatPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_collapse, openPendingIntent)

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = loadAlbumArtBitmap(song.albumArtUri)
            withContext(Dispatchers.Main) {
                val roundedBitmap = bitmap?.let { createRoundedBitmapWithBorder(it, 200, 200) }
                if (bitmap != null && roundedBitmap != null) {
                    expandedView.setImageViewBitmap(R.id.iv_background, bitmap)
                    expandedView.setImageViewBitmap(R.id.iv_album_art_big, roundedBitmap)
                    collapsedView.setImageViewBitmap(R.id.iv_album_art, roundedBitmap)
                } else {
                    expandedView.setImageViewResource(R.id.iv_background, R.drawable.ic_music_note)
                    expandedView.setImageViewResource(R.id.iv_album_art_big, R.drawable.ic_music_note)
                    collapsedView.setImageViewResource(R.id.iv_album_art, R.drawable.ic_music_note)
                }

                val notificationBuilder = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(song.title)
                    .setContentText(song.artist)
                    .setSubText("Music Player")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openPendingIntent)
                    .setSilent(true)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (isPlaying) {
                    notificationBuilder.setOngoing(true).setAutoCancel(false)
                    startForeground(NOTIFICATION_ID, notificationBuilder.build())
                } else {
                    notificationBuilder.setOngoing(false).setAutoCancel(true)
                    val dismissIntent = PendingIntent.getService(
                        this@MusicService, 105, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_DISMISS },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.setDeleteIntent(dismissIntent)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            }
        }
    }

    // ==================== LIFECYCLE ====================

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        handler.removeCallbacks(updatePositionRunnable)
        exoPlayer?.release()
        exoPlayer = null

        try {
            simpleCache.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing cache", e)
        }

        // Limpiar cache de StreamingSongs
        streamingSongCache.clear()

        super.onDestroy()
        Log.d(TAG, "🧹 MusicService destruido")
    }

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.example.music.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.music.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.music.ACTION_PREVIOUS"
        const val ACTION_SHUFFLE = "com.example.music.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.example.music.ACTION_REPEAT"
        const val ACTION_DISMISS = "com.example.music.ACTION_DISMISS"
    }
}*/


package com.example.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.DataSpec
import com.example.music.data.api.OkHttpDataSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong
import com.example.music.data.repository.StreamingMusicRepository
import com.example.music.data.api.MusicProviderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.net.URL

class MusicService : Service() {

    private val TAG = "MusicService"
    private var exoPlayer: ExoPlayer? = null
    private val binder = MusicBinder()

    // ✅ Cache de ExoPlayer (100MB)
    private lateinit var simpleCache: SimpleCache

    // ✅ Cache de StreamingSong para regenerar URLs
    private val streamingSongCache = mutableMapOf<String, StreamingSong>()

    // ✅ Repositorio para obtener URLs frescas
    private val streamingRepository by lazy { StreamingMusicRepository() }

    // Estados
    private val _currentSong = MutableStateFlow<Song?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    private val _shuffleEnabled = MutableStateFlow(false)
    private val _isBuffering = MutableStateFlow(false)

    // Cola de reproducción
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    private var currentIndex = 0
    private var originalQueue = listOf<Song>()

    private val handler = Handler(Looper.getMainLooper())
    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition
                        _duration.value = player.duration.coerceAtLeast(0)

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastNotificationUpdate > 1000) {
                            _currentSong.value?.let { updateNotification(it, true) }
                            lastNotificationUpdate = currentTime
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error actualizando posición: ${e.message}")
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    private var lastNotificationUpdate = 0L

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    fun getCurrentSong(): StateFlow<Song?> = _currentSong.asStateFlow()
    fun getIsPlaying(): StateFlow<Boolean> = _isPlaying.asStateFlow()
    fun getCurrentPosition(): StateFlow<Long> = _currentPosition.asStateFlow()
    fun getDuration(): StateFlow<Long> = _duration.asStateFlow()
    fun getRepeatMode(): StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    fun getShuffleEnabled(): StateFlow<Boolean> = _shuffleEnabled.asStateFlow()
    fun getIsBuffering(): StateFlow<Boolean> = _isBuffering.asStateFlow()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeExoPlayer()
        Log.d(TAG, "✅ MusicService creado con ExoPlayer + ResolvingDataSource (estilo InnerTune)")
    }

    @OptIn(UnstableApi::class)
    private fun initializeExoPlayer() {
        // ✅ Configurar cache (100MB)
        val cacheSize = 100L * 1024 * 1024 // 100MB
        val cacheDir = File(cacheDir, "exoplayer-cache")
        simpleCache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(cacheSize),
            androidx.media3.database.StandaloneDatabaseProvider(this)
        )

        // ✅ 1. OkHttpDataSource directo (bypasea sistema Android para evitar HTTP 403)
        val httpDataSourceFactory = OkHttpDataSourceFactory()

        // ✅ 2. ResolvingDataSource: regenera URLs automáticamente (como InnerTune)
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            httpDataSourceFactory
        ) { dataSpec ->
            val uri = dataSpec.uri
            val uriString = uri.toString()

            Log.d(TAG, "🔍 ResolvingDataSource interceptó: $uriString")

            // ✅ Si es URL de YouTube (streaming://PROVIDER/VIDEO_ID)
            if (uriString.startsWith("streaming://")) {
                Log.d(TAG, "🌐 Detectado URI streaming://, resolviendo...")

                val parts = uriString.removePrefix("streaming://").split("/")

                if (parts.size >= 2) {
                    val providerName = parts[0]
                    val songId = parts[1]

                    Log.d(TAG, "🎯 Provider: $providerName, ID: $songId")

                    // ✅ Buscar StreamingSong en cache
                    val streamingSong = streamingSongCache[songId]

                    if (streamingSong != null) {
                        Log.d(TAG, "🔄 Regenerando URL para: ${streamingSong.title}")

                        try {
                            // ✅ Obtener URL fresca bloqueando (necesario para ResolvingDataSource)
                            val freshUrl = runBlocking {
                                withContext(Dispatchers.IO) {
                                    streamingRepository.getStreamUrl(streamingSong)
                                }
                            }

                            if (freshUrl != null) {
                                Log.d(TAG, "✅ URL fresca obtenida: ${freshUrl.take(100)}...")
                                return@Factory dataSpec.withUri(Uri.parse(freshUrl))
                            } else {
                                Log.e(TAG, "❌ streamingRepository.getStreamUrl() retornó null")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error obteniendo URL fresca", e)
                        }
                    } else {
                        Log.e(TAG, "❌ StreamingSong no encontrado en cache para ID: $songId")
                    }
                }

                throw IOException("No se pudo resolver URL para: $uriString")
            }

            // ✅ Si es URL HTTP normal, pasar directo
            dataSpec
        }

        // ✅ 3. Cache DataSource
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // ✅ 4. ExoPlayer optimizado con ResolvingDataSource
        exoPlayer = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        500,    // minBufferMs
                        5000,   // maxBufferMs
                        200,    // bufferForPlaybackMs
                        500     // bufferForPlaybackAfterRebufferMs
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(cacheDataSourceFactory)
            )
            .build()

        // ✅ Listeners de ExoPlayer
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        Log.d(TAG, "⏳ Buffering...")
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        Log.d(TAG, "✅ Listo para reproducir")
                        _isBuffering.value = false
                        _duration.value = exoPlayer?.duration ?: 0L
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "🎵 Canción terminó")
                        handleAutoCompletion()
                    }
                    Player.STATE_IDLE -> {
                        Log.d(TAG, "💤 Idle")
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    handler.post(updatePositionRunnable)
                } else {
                    handler.removeCallbacks(updatePositionRunnable)
                }
                _currentSong.value?.let { updateNotification(it, isPlaying) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "❌ ExoPlayer error: ${error.message}", error)
                Log.e(TAG, "❌ Error code: ${error.errorCode}")
                _isPlaying.value = false
                _isBuffering.value = false
                _currentSong.value?.let { updateNotification(it, false) }
            }
        })

        Log.d(TAG, "✅ ExoPlayer inicializado con ResolvingDataSource (estilo InnerTune)")
    }

    /**
     * ✅ Registrar StreamingSong ANTES de reproducir
     * Esto permite que ResolvingDataSource pueda regenerar URLs cuando sea necesario
     */
    fun registerStreamingSong(songId: String, streamingSong: StreamingSong) {
        streamingSongCache[songId] = streamingSong
        Log.d(TAG, "📝 StreamingSong registrado: $songId -> ${streamingSong.title}")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📢 onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d(TAG, "⏯️ ACTION_PLAY_PAUSE desde notificación")
                playPause()
                handler.postDelayed({
                    _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
                }, 100)
            }
            ACTION_NEXT -> {
                Log.d(TAG, "⏭️ ACTION_NEXT desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_SKIP_NEXT"))
            }
            ACTION_PREVIOUS -> {
                Log.d(TAG, "⏮️ ACTION_PREVIOUS desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_SKIP_PREVIOUS"))
            }
            ACTION_SHUFFLE -> {
                Log.d(TAG, "🔀 ACTION_SHUFFLE desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_TOGGLE_SHUFFLE"))
            }
            ACTION_REPEAT -> {
                Log.d(TAG, "🔁 ACTION_REPEAT desde notificación")
                sendBroadcast(Intent("com.example.music.ACTION_CYCLE_REPEAT"))
            }
            ACTION_DISMISS -> {
                Log.d(TAG, "🗑️ ACTION_DISMISS")
                dismissNotificationAndStop()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for music playback"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    // ==================== PLAYBACK ====================

    fun playSongWithQueue(song: Song, queue: List<Song>) {
        Log.d(TAG, "🎵 playSongWithQueue: ${song.title}, queue: ${queue.size}")
        originalQueue = queue
        _queue.value = queue
        currentIndex = queue.indexOf(song)
        playSong(song)
    }

    fun play() {
        Log.d(TAG, "▶️ play()")
        try {
            exoPlayer?.play()
            _isPlaying.value = true
            handler.post(updatePositionRunnable)

            _currentSong.value?.let {
                updateNotification(it, true)
                Log.d(TAG, "🔔 Notificación actualizada")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al reproducir", e)
        }
    }

    private fun playSong(song: Song) {
        try {
            Log.d(TAG, "▶️▶️▶️ playSong INICIADO ▶️▶️▶️")
            Log.d(TAG, "📌 Título: ${song.title}")
            Log.d(TAG, "📌 isStreaming: ${song.isStreaming}")
            Log.d(TAG, "📌 Path completo: ${song.path}")

            _currentSong.value = song

            // ✅ Determinar tipo de URI
            val uri = when {
                // ✅ Para streaming, usar formato especial que ResolvingDataSource reconoce
                song.path.startsWith("streaming://") -> {
                    Log.d(TAG, "🌐 Tipo: URI streaming:// (será resuelto por ResolvingDataSource)")
                    Log.d(TAG, "🌐 URI: ${song.path}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("http://") || song.path.startsWith("https://") -> {
                    Log.d(TAG, "🌐 Tipo: URL HTTP/HTTPS directa")
                    Log.d(TAG, "🌐 URL (primeros 150 chars): ${song.path.take(150)}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("content://") -> {
                    Log.d(TAG, "📱 Tipo: Content URI")
                    Log.d(TAG, "📱 URI: ${song.path}")
                    Uri.parse(song.path)
                }
                song.path.startsWith("/") -> {
                    Log.d(TAG, "📁 Tipo: Archivo local")
                    Log.d(TAG, "📁 Path: file://${song.path}")
                    Uri.parse("file://${song.path}")
                }
                else -> {
                    Log.d(TAG, "❓ Tipo: Desconocido")
                    Log.d(TAG, "❓ Parseando directo: ${song.path}")
                    Uri.parse(song.path)
                }
            }

            Log.d(TAG, "🎵 URI final para ExoPlayer: $uri")

            // ✅ Crear MediaItem
            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .build()

            // ✅ Configurar y reproducir
            exoPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            Log.d(TAG, "✅ ExoPlayer.prepare() y play() llamados")
            Log.d(TAG, "⏳ Esperando que ExoPlayer inicie buffering...")

        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ EXCEPCIÓN EN playSong ❌❌❌", e)
            Log.e(TAG, "❌ Mensaje: ${e.message}")
            Log.e(TAG, "❌ Stack trace:", e)
            _isPlaying.value = false
            _isBuffering.value = false
        }
    }

    fun pause() {
        Log.d(TAG, "⏸️ pause()")
        try {
            exoPlayer?.pause()
            _isPlaying.value = false
            handler.removeCallbacks(updatePositionRunnable)
            stopForeground(STOP_FOREGROUND_DETACH)

            _currentSong.value?.let { updateNotification(it, false) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al pausar", e)
        }
    }

    fun playPause() {
        Log.d(TAG, "⏯️ playPause() - isPlaying: ${_isPlaying.value}")
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(position: Long) {
        Log.d(TAG, "🎯 Seeking to: ${formatTime(position)}")
        exoPlayer?.seekTo(position)
        _currentPosition.value = position
    }

    // ==================== AUTO COMPLETION ====================

    private fun handleAutoCompletion() {
        val queue = _queue.value
        if (queue.isEmpty()) return

        Log.d(TAG, "🎵 Auto-completion: repeat=${_repeatMode.value}, index=$currentIndex/${queue.size}")

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                playSong(queue[currentIndex])
                Log.d(TAG, "🔂 Repeat One (auto) → Repitiendo ${queue[currentIndex].title}")
            }
            RepeatMode.ALL -> {
                currentIndex = (currentIndex + 1) % queue.size
                playSong(queue[currentIndex])
                Log.d(TAG, "🔁 Repeat All (auto) → [$currentIndex/${queue.size}] ${queue[currentIndex].title}")
            }
            RepeatMode.OFF -> {
                if (currentIndex < queue.size - 1) {
                    currentIndex++
                    playSong(queue[currentIndex])
                    Log.d(TAG, "▶️ Normal (auto) → [$currentIndex/${queue.size}] ${queue[currentIndex].title}")
                } else {
                    Log.d(TAG, "🛑 Final de la cola - Deteniendo")
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    // ==================== SHUFFLE & REPEAT ====================

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
        Log.d(TAG, "🔀 setShuffle: $enabled")
        _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
    }

    fun updateQueue(newQueue: List<Song>, newIndex: Int) {
        _queue.value = newQueue
        currentIndex = newIndex
        Log.d(TAG, "📝 Cola actualizada: ${newQueue.size} canciones, índice: $newIndex")
    }

    fun setRepeat(mode: RepeatMode) {
        _repeatMode.value = mode
        Log.d(TAG, "🔁 setRepeat: $mode")
        _currentSong.value?.let { updateNotification(it, _isPlaying.value) }
    }

    // ==================== NOTIFICATION ====================
    // ✅ TODA LA LÓGICA DE NOTIFICACIÓN SE MANTIENE EXACTAMENTE IGUAL

    private fun dismissNotificationAndStop() {
        Log.d(TAG, "🗑️ Deslizando notificación")

        try {
            if (exoPlayer?.isPlaying == true) {
                exoPlayer?.pause()
            }
            _isPlaying.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing player", e)
        }

        handler.removeCallbacks(updatePositionRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "✅ Notificación eliminada")
    }

    private suspend fun loadAlbumArtBitmap(albumArtUri: String?): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                if (albumArtUri.isNullOrEmpty()) return@withContext null
                if (albumArtUri.startsWith("http")) {
                    val connection = URL(albumArtUri).openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.connect()
                    BitmapFactory.decodeStream(connection.getInputStream())
                } else {
                    contentResolver.openInputStream(Uri.parse(albumArtUri))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun createRoundedBitmapWithBorder(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }
        val rect = Rect(0, 0, width, height)
        val rectF = RectF(rect)
        val cornerRadius = 24f

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        canvas.drawBitmap(scaledBitmap, rect, rect, paint)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = android.graphics.Color.parseColor("#4DFFFFFF")
        }
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)
        return output
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateNotification(song: Song, isPlaying: Boolean) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this, 100, Intent(this, MusicService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextPendingIntent = PendingIntent.getService(
            this, 101, Intent(this, MusicService::class.java).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val previousPendingIntent = PendingIntent.getService(
            this, 102, Intent(this, MusicService::class.java).setAction(ACTION_PREVIOUS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val shufflePendingIntent = PendingIntent.getService(
            this, 103, Intent(this, MusicService::class.java).setAction(ACTION_SHUFFLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val repeatPendingIntent = PendingIntent.getService(
            this, 104, Intent(this, MusicService::class.java).setAction(ACTION_REPEAT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)
        collapsedView.setTextViewText(R.id.tv_song_title, song.title)
        collapsedView.setImageViewResource(R.id.btn_play_pause, if (isPlaying) R.drawable.ic_pause_dark else R.drawable.ic_play_dark)
        collapsedView.setOnClickPendingIntent(R.id.btn_play_pause, playPausePendingIntent)
        collapsedView.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
        collapsedView.setOnClickPendingIntent(R.id.btn_previous, previousPendingIntent)

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
        expandedView.setTextViewText(R.id.tv_song_title_big, song.title)
        expandedView.setTextViewText(R.id.tv_artist_big, song.artist)
        val progress = if (_duration.value > 0) (_currentPosition.value * 100 / _duration.value).toInt() else 0
        expandedView.setProgressBar(R.id.progress_bar, 100, progress, false)
        expandedView.setTextViewText(R.id.tv_current_time, formatTime(_currentPosition.value))
        expandedView.setTextViewText(R.id.tv_total_time, formatTime(_duration.value))
        expandedView.setImageViewResource(R.id.btn_play_pause_big, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        expandedView.setImageViewResource(R.id.btn_shuffle, if (_shuffleEnabled.value) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle)
        val repeatIcon = when (_repeatMode.value) {
            RepeatMode.OFF -> R.drawable.ic_repeat
            RepeatMode.ALL -> R.drawable.ic_repeat_all
            RepeatMode.ONE -> R.drawable.ic_repeat_one
        }
        expandedView.setImageViewResource(R.id.btn_repeat, repeatIcon)
        expandedView.setOnClickPendingIntent(R.id.btn_previous_big, previousPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_play_pause_big, playPausePendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_next_big, nextPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_shuffle, shufflePendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_repeat, repeatPendingIntent)
        expandedView.setOnClickPendingIntent(R.id.btn_collapse, openPendingIntent)

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = loadAlbumArtBitmap(song.albumArtUri)
            withContext(Dispatchers.Main) {
                val roundedBitmap = bitmap?.let { createRoundedBitmapWithBorder(it, 200, 200) }
                if (bitmap != null && roundedBitmap != null) {
                    expandedView.setImageViewBitmap(R.id.iv_background, bitmap)
                    expandedView.setImageViewBitmap(R.id.iv_album_art_big, roundedBitmap)
                    collapsedView.setImageViewBitmap(R.id.iv_album_art, roundedBitmap)
                } else {
                    expandedView.setImageViewResource(R.id.iv_background, R.drawable.ic_music_note)
                    expandedView.setImageViewResource(R.id.iv_album_art_big, R.drawable.ic_music_note)
                    collapsedView.setImageViewResource(R.id.iv_album_art, R.drawable.ic_music_note)
                }

                val notificationBuilder = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(song.title)
                    .setContentText(song.artist)
                    .setSubText("Music Player")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openPendingIntent)
                    .setSilent(true)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                if (isPlaying) {
                    notificationBuilder.setOngoing(true).setAutoCancel(false)
                    startForeground(NOTIFICATION_ID, notificationBuilder.build())
                } else {
                    notificationBuilder.setOngoing(false).setAutoCancel(true)
                    val dismissIntent = PendingIntent.getService(
                        this@MusicService, 105, Intent(this@MusicService, MusicService::class.java).apply { action = ACTION_DISMISS },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    notificationBuilder.setDeleteIntent(dismissIntent)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            }
        }
    }

    // ==================== LIFECYCLE ====================

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        handler.removeCallbacks(updatePositionRunnable)
        exoPlayer?.release()
        exoPlayer = null

        try {
            simpleCache.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing cache", e)
        }

        streamingSongCache.clear()

        super.onDestroy()
        Log.d(TAG, "🧹 MusicService destruido")
    }

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.example.music.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.music.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.music.ACTION_PREVIOUS"
        const val ACTION_SHUFFLE = "com.example.music.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "com.example.music.ACTION_REPEAT"
        const val ACTION_DISMISS = "com.example.music.ACTION_DISMISS"
    }
}