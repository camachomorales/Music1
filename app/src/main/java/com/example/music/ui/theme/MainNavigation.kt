/*package com.example.music.ui.theme

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.music.R
import com.example.music.audio.MusicPlayerScreen
import com.example.music.audio.PlayerState
import com.example.music.data.model.*
import com.example.music.ui.theme.components.BottomNavigationBar
import com.example.music.ui.theme.components.MiniPlayer
import com.example.music.ui.theme.screens.*
import com.example.music.viewmodel.MusicPlayerViewModel
import com.example.music.viewmodel.LibraryViewModel
import com.example.music.viewmodel.UserPreferencesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(viewModel: MusicPlayerViewModel, songs: List<Song>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ LibraryViewModel
    val libraryViewModel: LibraryViewModel = viewModel()

    // Estados del MusicPlayerViewModel
    val streamingSongs by viewModel.streamingSongs.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isSearchingStreaming by viewModel.isSearching.collectAsStateWithLifecycle()
    val localSearchResults by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // ✅ Estados del LibraryViewModel
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val favoriteStreamingSongIds by libraryViewModel.favoriteStreamingSongIds.collectAsStateWithLifecycle()
    val recentlyPlayedSongs by libraryViewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()

    // Determinar si estamos en modo online
    val isOnlineMode = appMode == AppMode.STREAMING

    // Ruta actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                // Mini Player (solo visible si hay canción y no estamos en PlayerScreen)
                if (currentSong != null && currentRoute != "player") {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = 0f,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onMiniPlayerClick = { navController.navigate("player") }
                    )
                }

                // Bottom Navigation
                if (currentRoute != "player") {
                    BottomNavigationBar(
                        selectedRoute = currentRoute ?: "home",
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo de la app
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Navegación
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                // ==================== HOME SCREEN ====================
                composable("home") {
                    HomeScreen(
                        songs = if (isOnlineMode) emptyList() else songs,
                        streamingSongs = if (isOnlineMode) streamingSongs else emptyList(),
                        appMode = appMode,
                        onSongClick = { song ->
                            // ✅ Agregar a Recently Played
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songs)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            // ✅ Cachear StreamingSong
                            viewModel.cacheStreamingSong(streamingSong)

                            // ✅ Crear Song desde StreamingSong
                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = streamingSong.album ?: streamingSong.artist,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id, // ✅ IMPORTANTE
                                streamingProvider = streamingSong.provider.name // ✅ IMPORTANTE
                            )

                            // ✅ Agregar a Recently Played
                            libraryViewModel.addToRecentlyPlayed(song)

                            viewModel.playStreamingSong(streamingSong)
                            navController.navigate("player")
                        },
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onSearch = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        searchQuery = searchQuery,
                        isSearching = isSearchingStreaming,
                        // ✅ CALLBACKS DE FAVORITOS LOCAL
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        // ✅ CALLBACKS DE FAVORITOS STREAMING
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== SEARCH SCREEN ====================
                composable("search") {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        localSearchResults = if (isOnlineMode) emptyList() else localSearchResults,
                        streamingSearchResults = if (isOnlineMode) streamingSongs else emptyList(),
                        isOnlineMode = isOnlineMode,
                        isSearching = isSearchingStreaming,
                        onSongClick = { song ->
                            // ✅ Agregar a Recently Played
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, localSearchResults)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            // ✅ Cachear StreamingSong
                            viewModel.cacheStreamingSong(streamingSong)

                            // ✅ Crear Song desde StreamingSong
                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = streamingSong.album ?: streamingSong.artist,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id, // ✅ IMPORTANTE
                                streamingProvider = streamingSong.provider.name // ✅ IMPORTANTE
                            )

                            // ✅ Agregar a Recently Played
                            libraryViewModel.addToRecentlyPlayed(song)

                            viewModel.playStreamingSong(streamingSong)
                            navController.navigate("player")
                        },
                        // ✅ CALLBACKS DE FAVORITOS LOCAL
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        // ✅ CALLBACKS DE FAVORITOS STREAMING
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== LIBRARY SCREEN ====================
                composable("library") {
                    LibraryScreen(
                        currentMode = appMode,
                        onToggleMode = {
                            viewModel.toggleAppMode()
                        },
                        onPlaylistsClick = {
                            navController.navigate("playlists")
                        },
                        onSongsClick = {
                            navController.navigate("songs")
                        },
                        onAlbumsClick = {
                            navController.navigate("albums")
                        },
                        onArtistsClick = {
                            navController.navigate("artists")
                        },
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        },
                        onRecentlyPlayedClick = {
                            navController.navigate("recently_played")
                        }
                    )
                }

                // ==================== FRAGMENTO PARA MainNavigation.kt ====================
// Reemplazar los composables de "favorites" y "recently_played"

                // ==================== FAVORITES SCREEN ====================
                composable("favorites") {
                    // ✅ Obtener canciones locales favoritas de playlists
                    val localFavoriteSongs = remember(playlists, favoriteSongIds) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { favoriteSongIds.contains(it.id) }
                    }

                    // ✅ Obtener canciones favoritas del historial (Recently Played)
                    val recentFavoriteSongs = remember(recentlyPlayedSongs, favoriteSongIds, favoriteStreamingSongIds) {
                        recentlyPlayedSongs.filter { song ->
                            if (song.isStreaming) {
                                // Obtener streamingId
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        }
                    }

                    // ✅ Combinar ambas listas sin duplicados
                    val allFavoriteSongs = remember(localFavoriteSongs, recentFavoriteSongs) {
                        (localFavoriteSongs + recentFavoriteSongs).distinctBy { it.id }
                    }

                    FavoritesScreen(
                        favoriteSongs = allFavoriteSongs,
                        appMode = appMode, // ✅ NUEVO: Pasar modo actual
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, allFavoriteSongs)
                            navController.navigate("player")
                        },
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                // ✅ Es streaming - extraer ID del path
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    // ✅ Obtener provider
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    // ✅ Crear StreamingSong para toggle
                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // ==================== RECENTLY PLAYED SCREEN ====================
                composable("recently_played") {
                    RecentlyPlayedScreen(
                        recentlyPlayedSongs = recentlyPlayedSongs,
                        appMode = appMode, // ✅ NUEVO: Pasar modo actual
                        favoriteSongIds = favoriteSongIds,
                        favoriteStreamingSongIds = favoriteStreamingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            // ✅ Agregar a Recently Played
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, recentlyPlayedSongs)
                            navController.navigate("player")
                        },
                        // ✅ TOGGLE CORREGIDO - Detecta tipo de canción
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                // ✅ Es streaming - extraer ID del path
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    // ✅ Obtener provider
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    // ✅ Crear StreamingSong para toggle
                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                // ✅ Es local
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onClearHistory = {
                            libraryViewModel.clearRecentlyPlayed()
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }
                // ==================== PLAYLIST DETAIL SCREEN ====================
                composable(
                    route = "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    val playlist = playlists.find { it.id == playlistId } ?: return@composable

                    PlaylistDetailScreen(
                        playlist = playlist,
                        allSongs = songs,
                        currentMode = appMode,
                        streamingSongs = streamingSongs,
                        isSearching = isSearchingStreaming,
                        onSearchStreaming = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        onBackClick = { navController.navigateUp() },
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playMixedPlaylist(
                                song = song,
                                playlist = playlist.songs,
                                streamingSongs = streamingSongs,
                                playlistName = playlist.name
                            )
                            navController.navigate("player")
                        },
                        onRemoveSong = { songId ->
                            libraryViewModel.removeSongFromPlaylist(playlistId, songId)
                        },
                        onAddSong = { song ->
                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onAddStreamingSong = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)

                            val song = Song(
                                id = streamingSong.id.hashCode().toLong(),
                                title = streamingSong.title,
                                artist = streamingSong.artist,
                                album = playlist.name,
                                duration = streamingSong.duration,
                                path = "streaming://${streamingSong.provider.name.lowercase()}/${streamingSong.id}",
                                albumArtUri = streamingSong.thumbnailUrl,
                                isStreaming = true,
                                streamingId = streamingSong.id,
                                streamingProvider = streamingSong.provider.name
                            )
                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onReorderSongs = { reorderedSongs ->
                            libraryViewModel.reorderPlaylistSongs(playlistId, reorderedSongs)
                        },
                        onPlayAll = {
                            if (playlist.songs.isNotEmpty()) {
                                libraryViewModel.addToRecentlyPlayed(playlist.songs.first())
                                viewModel.playMixedPlaylist(
                                    song = playlist.songs.first(),
                                    playlist = playlist.songs,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        },
                        onShufflePlay = {
                            if (playlist.songs.isNotEmpty()) {
                                val shuffled = playlist.songs.shuffled()
                                libraryViewModel.addToRecentlyPlayed(shuffled.first())
                                viewModel.playMixedPlaylist(
                                    song = shuffled.first(),
                                    playlist = shuffled,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        }
                    )
                }

                // ==================== SONGS (ALL SONGS) SCREEN ====================
                composable("songs") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    AllSongsScreen(
                        songs = filteredSongs,
                        onSongClick = { song, songList ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songList)
                            navController.navigate("player")
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ALBUMS SCREEN ====================
                composable("albums") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val albumsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                            Album(
                                id = albumName,
                                title = albumName,
                                artist = albumSongs.firstOrNull()?.artist ?: "Unknown",
                                coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                year = null,
                                songs = albumSongs
                            )
                        }
                    }

                    AlbumsScreen(
                        albums = albumsList,
                        onAlbumClick = { album -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ARTISTS SCREEN ====================
                composable("artists") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val artistsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.artist }.map { (artistName, artistSongs) ->
                            val albumsForArtist = artistSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                                Album(
                                    id = albumName,
                                    title = albumName,
                                    artist = artistName,
                                    coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                    songs = albumSongs
                                )
                            }

                            Artist(
                                id = artistName,
                                name = artistName,
                                imageUri = null,
                                albums = albumsForArtist,
                                songs = artistSongs
                            )
                        }
                    }

                    ArtistsScreen(
                        artists = artistsList,
                        onArtistClick = { artist -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== USER SCREEN ====================
                composable("user") {
                    // ✅ UserPreferencesViewModel
                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel()

                    // Obtener estados
                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()
                    val accountType by userPreferencesViewModel.accountType.collectAsStateWithLifecycle()

                    SettingsScreen(
                        appMode = appMode,
                        userPreferences = userPreferences,
                        accountType = accountType,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onAccountClick = {
                            navController.navigate("account")
                        },
                        onPlaybackClick = {
                            navController.navigate("playback_settings")
                        },
                        onStorageClick = {
                            Toast.makeText(context, "Storage screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onNotificationsClick = {
                            userPreferencesViewModel.toggleNotifications()
                            Toast.makeText(context, "Notifications setting updated", Toast.LENGTH_SHORT).show()
                        },
                        onAboutClick = {
                            Toast.makeText(context, "About screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDarkTheme = {
                            userPreferencesViewModel.toggleDarkTheme()
                            Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDownload = {
                            userPreferencesViewModel.toggleDownloadOnlyOnWifi()
                            Toast.makeText(context, "Download settings updated", Toast.LENGTH_SHORT).show()
                        },
                        onClearCache = {
                            viewModel.clearCache()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ACCOUNT SCREEN ====================
                composable("account") {
                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel()
                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()
                    val accountType by userPreferencesViewModel.accountType.collectAsStateWithLifecycle()

                    var isLoading by remember { mutableStateOf(false) }
                    var isSyncing by remember { mutableStateOf(false) }

                    AccountScreen(
                        userPreferences = userPreferences,
                        accountType = accountType,
                        isLoading = isLoading,
                        isSyncing = isSyncing,
                        onLogin = { email, password ->
                            isLoading = true
                            scope.launch {
                                delay(2000) // Simular login
                                isLoading = false
                                userPreferencesViewModel.setAccountType(AccountType.LOCAL)
                                Toast.makeText(context, "Logged in successfully", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        },
                        onRegister = { email, password, name ->
                            isLoading = true
                            scope.launch {
                                delay(2000) // Simular registro
                                isLoading = false
                                userPreferencesViewModel.setAccountType(AccountType.LOCAL)
                                Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        },
                        onLogout = {
                            userPreferencesViewModel.setAccountType(AccountType.GUEST)
                            Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        },
                        onSync = {
                            isSyncing = true
                            scope.launch {
                                delay(3000) // Simular sync
                                isSyncing = false
                                Toast.makeText(context, "Data synchronized", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYBACK SETTINGS SCREEN ====================
                composable("playback_settings") {
                    val userPreferencesViewModel: UserPreferencesViewModel = viewModel()
                    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()

                    PlaybackSettingsScreen(
                        userPreferences = userPreferences,
                        onAudioQualityChange = { newQuality ->
                            userPreferencesViewModel.setAudioQuality(newQuality)
                            Toast.makeText(context, "Audio quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onStreamingQualityChange = { newQuality ->
                            userPreferencesViewModel.setStreamingQuality(newQuality)
                            Toast.makeText(context, "Streaming quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onDownloadQualityChange = { newQuality ->
                            userPreferencesViewModel.setDownloadQuality(newQuality)
                            Toast.makeText(context, "Download quality set to ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onCrossfadeChange = { seconds ->
                            userPreferencesViewModel.setCrossfadeDuration(seconds)
                            Toast.makeText(context, "Crossfade set to ${seconds}s", Toast.LENGTH_SHORT).show()
                        },
                        onToggleGapless = {
                            userPreferencesViewModel.toggleGaplessPlayback()
                            Toast.makeText(context, "Gapless playback toggled", Toast.LENGTH_SHORT).show()
                        },
                        onToggleNormalize = {
                            userPreferencesViewModel.toggleNormalizeVolume()
                            Toast.makeText(context, "Volume normalization toggled", Toast.LENGTH_SHORT).show()
                        },
                        onEqualizerChange = { preset ->
                            userPreferencesViewModel.setEqualizerPreset(preset)
                            Toast.makeText(context, "Equalizer preset set to ${preset.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYER SCREEN ====================
                composable("player") {
                    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
                    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
                    val duration by viewModel.duration.collectAsStateWithLifecycle()
                    val isLoadingStream by viewModel.isLoadingStream.collectAsStateWithLifecycle()

                    // ✅ DETECTAR SI ES FAVORITO (LOCAL O STREAMING)
                    val isLiked = remember(currentSong, favoriteSongIds, favoriteStreamingSongIds) {
                        currentSong?.let { song ->
                            if (song.isStreaming) {
                                // Usar streamingId si está disponible
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        } ?: false
                    }

                    MusicPlayerScreen(
                        playerState = PlayerState(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            isShuffleEnabled = shuffleEnabled,
                            repeatMode = repeatMode,
                            isLiked = isLiked,
                            isLoadingStream = isLoadingStream
                        ),
                        appMode = appMode,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleLike = {
                            currentSong?.let { song ->
                                if (song.isStreaming) {
                                    // ✅ Es streaming song - usar streamingId si está disponible
                                    val streamingId = song.streamingId ?: run {
                                        if (song.path.startsWith("streaming://")) {
                                            val parts = song.path.removePrefix("streaming://").split("/")
                                            if (parts.size >= 2) parts[1] else null
                                        } else null
                                    }

                                    streamingId?.let { id ->
                                        // ✅ Obtener provider
                                        val provider = song.streamingProvider?.let { providerName ->
                                            when (providerName.uppercase()) {
                                                "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                                "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            }
                                        } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                        // ✅ Crear StreamingSong para toggle
                                        val streamingSong = com.example.music.data.model.StreamingSong(
                                            id = id,
                                            title = song.title,
                                            artist = song.artist,
                                            album = song.album,
                                            duration = song.duration,
                                            thumbnailUrl = song.albumArtUri,
                                            provider = provider
                                        )
                                        libraryViewModel.toggleStreamingFavorite(streamingSong)
                                    }
                                } else {
                                    // ✅ Es local song
                                    libraryViewModel.toggleFavorite(song)
                                }
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}*/


package com.example.music.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.music.R
import com.example.music.audio.MusicPlayerScreen
import com.example.music.audio.PlayerState
import com.example.music.data.model.Album
import com.example.music.data.model.AppMode
import com.example.music.data.model.Artist
import com.example.music.data.model.Song
import com.example.music.ui.theme.components.BottomNavigationBar
import com.example.music.ui.theme.components.MiniPlayer
import com.example.music.ui.theme.screens.*
import com.example.music.viewmodel.MusicPlayerViewModel
import com.example.music.viewmodel.LibraryViewModel
import android.widget.Toast
import com.example.music.viewmodel.UserPreferencesViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.music.utils.toSong
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(viewModel: MusicPlayerViewModel, songs: List<Song>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ ViewModels
    val libraryViewModel: LibraryViewModel = viewModel()
    val userPreferencesViewModel: UserPreferencesViewModel = viewModel()

    // Estados del MusicPlayerViewModel
    val streamingSongs by viewModel.streamingSongs.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isSearchingStreaming by viewModel.isSearching.collectAsStateWithLifecycle()
    val localSearchResults by viewModel.localSearchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // ✅ Estados del LibraryViewModel
    val playlists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongIds by libraryViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val favoriteStreamingSongIds by libraryViewModel.favoriteStreamingSongIds.collectAsStateWithLifecycle()
    val recentlyPlayedSongs by libraryViewModel.recentlyPlayedSongs.collectAsStateWithLifecycle()

    // ✅ Estados de UserPreferences
    val userPreferences by userPreferencesViewModel.userPreferences.collectAsStateWithLifecycle()
    val accountType by userPreferencesViewModel.accountType.collectAsStateWithLifecycle()

    // Determinar si estamos en modo online
    val isOnlineMode = appMode == AppMode.STREAMING

    // Ruta actual
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                // Mini Player (solo visible si hay canción y no estamos en PlayerScreen)
                if (currentSong != null && currentRoute != "player") {
                    MiniPlayer(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        progress = 0f,
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onNextClick = { viewModel.skipToNext() },
                        onMiniPlayerClick = { navController.navigate("player") }
                    )
                }

                // Bottom Navigation
                if (currentRoute != "player") {
                    BottomNavigationBar(
                        selectedRoute = currentRoute ?: "home",
                        onItemSelected = { route ->
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo de la app
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Navegación
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                // ==================== HOME SCREEN ====================
                composable("home") {
                    HomeScreen(
                        songs = if (isOnlineMode) emptyList() else songs,
                        streamingSongs = if (isOnlineMode) streamingSongs else emptyList(),
                        appMode = appMode,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songs)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)
                            viewModel.playStreamingSong(streamingSong)  // ✅ Solo esto
                            navController.navigate("player")
                        },
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onSearch = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        searchQuery = searchQuery,
                        isSearching = isSearchingStreaming,
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== SEARCH SCREEN ====================
                composable("search") {
                    SearchScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        localSearchResults = if (isOnlineMode) emptyList() else localSearchResults,
                        streamingSearchResults = if (isOnlineMode) streamingSongs else emptyList(),
                        isOnlineMode = isOnlineMode,
                        isSearching = isSearchingStreaming,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, localSearchResults)
                            navController.navigate("player")
                        },
                        onStreamingSongClick = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)
                            viewModel.playStreamingSong(streamingSong)  // ✅ Solo esto
                            navController.navigate("player")
                        },
                        isFavorite = { songId ->
                            favoriteSongIds.contains(songId)
                        },
                        onToggleFavorite = { song ->
                            libraryViewModel.toggleFavorite(song)
                        },
                        isStreamingFavorite = { streamingId ->
                            favoriteStreamingSongIds.contains(streamingId)
                        },
                        onToggleStreamingFavorite = { streamingSong ->
                            libraryViewModel.toggleStreamingFavorite(streamingSong)
                        }
                    )
                }

                // ==================== LIBRARY SCREEN ====================
                composable("library") {
                    LibraryScreen(
                        currentMode = appMode,
                        onToggleMode = {
                            viewModel.toggleAppMode()
                        },
                        onPlaylistsClick = {
                            navController.navigate("playlists")
                        },
                        onSongsClick = {
                            navController.navigate("songs")
                        },
                        onAlbumsClick = {
                            navController.navigate("albums")
                        },
                        onArtistsClick = {
                            navController.navigate("artists")
                        },
                        onFavoritesClick = {
                            navController.navigate("favorites")
                        },
                        onRecentlyPlayedClick = {
                            navController.navigate("recently_played")
                        }
                    )
                }

                // ==================== FAVORITES SCREEN ====================
                composable("favorites") {
                    val localFavoriteSongs = remember(playlists, favoriteSongIds) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { favoriteSongIds.contains(it.id) }
                    }

                    val recentFavoriteSongs = remember(recentlyPlayedSongs, favoriteSongIds, favoriteStreamingSongIds) {
                        recentlyPlayedSongs.filter { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        }
                    }

                    val allFavoriteSongs = remember(localFavoriteSongs, recentFavoriteSongs) {
                        (localFavoriteSongs + recentFavoriteSongs).distinctBy { it.id }
                    }

                    FavoritesScreen(
                        favoriteSongs = allFavoriteSongs,
                        appMode = appMode,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, allFavoriteSongs)
                            navController.navigate("player")
                        },
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // ==================== RECENTLY PLAYED SCREEN ====================
                composable("recently_played") {
                    RecentlyPlayedScreen(
                        recentlyPlayedSongs = recentlyPlayedSongs,
                        appMode = appMode,
                        favoriteSongIds = favoriteSongIds,
                        favoriteStreamingSongIds = favoriteStreamingSongIds,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, recentlyPlayedSongs)
                            navController.navigate("player")
                        },
                        onToggleFavorite = { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }

                                streamingId?.let { id ->
                                    val provider = song.streamingProvider?.let { providerName ->
                                        when (providerName.uppercase()) {
                                            "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                            "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                        }
                                    } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                    val streamingSong = com.example.music.data.model.StreamingSong(
                                        id = id,
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        duration = song.duration,
                                        thumbnailUrl = song.albumArtUri,
                                        provider = provider
                                    )

                                    libraryViewModel.toggleStreamingFavorite(streamingSong)
                                }
                            } else {
                                libraryViewModel.toggleFavorite(song)
                            }
                        },
                        onClearHistory = {
                            libraryViewModel.clearRecentlyPlayed()
                        },
                        onNavigateBack = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYLISTS SCREEN ====================
                composable("playlists") {
                    PlaylistsScreen(
                        playlists = playlists,
                        onPlaylistClick = { playlist ->
                            navController.navigate("playlist_detail/${playlist.id}")
                        },
                        onCreatePlaylist = { name, description ->
                            libraryViewModel.createPlaylist(name, description)
                        },
                        onDeletePlaylist = { playlistId ->
                            libraryViewModel.deletePlaylist(playlistId)
                        },
                        onRenamePlaylist = { playlistId, newName ->
                            libraryViewModel.renamePlaylist(playlistId, newName)
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYLIST DETAIL SCREEN - CÓDIGO CORREGIDO ====================
                composable(
                    route = "playlist_detail/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    val playlist = playlists.find { it.id == playlistId } ?: return@composable

                    PlaylistDetailScreen(
                        playlist = playlist,
                        allSongs = songs,
                        currentMode = appMode,
                        streamingSongs = streamingSongs,
                        isSearching = isSearchingStreaming,
                        onSearchStreaming = { query ->
                            viewModel.updateSearchQuery(query)
                        },
                        onBackClick = { navController.navigateUp() },
                        onSongClick = { song ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playMixedPlaylist(
                                song = song,
                                playlist = playlist.songs,
                                streamingSongs = streamingSongs,
                                playlistName = playlist.name
                            )
                            navController.navigate("player")
                        },
                        onRemoveSong = { songId ->
                            libraryViewModel.removeSongFromPlaylist(playlistId, songId)
                        },
                        onAddSong = { song ->
                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onAddStreamingSong = { streamingSong ->
                            viewModel.cacheStreamingSong(streamingSong)

                            // ✅ Conversión con nombre de playlist
                            val song = streamingSong.toSong(albumOverride = playlist.name)

                            libraryViewModel.addSongToPlaylist(playlistId, song)
                        },
                        onReorderSongs = { reorderedSongs ->
                            libraryViewModel.reorderPlaylistSongs(playlistId, reorderedSongs)
                        },
                        onPlayAll = {
                            if (playlist.songs.isNotEmpty()) {
                                libraryViewModel.addToRecentlyPlayed(playlist.songs.first())
                                viewModel.playMixedPlaylist(
                                    song = playlist.songs.first(),
                                    playlist = playlist.songs,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        },
                        onShufflePlay = {
                            if (playlist.songs.isNotEmpty()) {
                                val shuffled = playlist.songs.shuffled()
                                libraryViewModel.addToRecentlyPlayed(shuffled.first())
                                viewModel.playMixedPlaylist(
                                    song = shuffled.first(),
                                    playlist = shuffled,
                                    streamingSongs = streamingSongs,
                                    playlistName = playlist.name
                                )
                                navController.navigate("player")
                            }
                        }
                    )
                }

                // ==================== SONGS (ALL SONGS) SCREEN ====================
                composable("songs") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    AllSongsScreen(
                        songs = filteredSongs,
                        onSongClick = { song, songList ->
                            libraryViewModel.addToRecentlyPlayed(song)
                            viewModel.playSong(song, songList)
                            navController.navigate("player")
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ALBUMS SCREEN ====================
                composable("albums") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val albumsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                            Album(
                                id = albumName,
                                title = albumName,
                                artist = albumSongs.firstOrNull()?.artist ?: "Unknown",
                                coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                year = null,
                                songs = albumSongs
                            )
                        }
                    }

                    AlbumsScreen(
                        albums = albumsList,
                        onAlbumClick = { album -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ARTISTS SCREEN ====================
                composable("artists") {
                    val filteredSongs = remember(playlists, appMode) {
                        playlists
                            .flatMap { it.songs }
                            .distinctBy { it.id }
                            .filter { song ->
                                when (appMode) {
                                    AppMode.OFFLINE -> !song.isStreaming
                                    AppMode.STREAMING -> song.isStreaming
                                }
                            }
                    }

                    val artistsList = remember(filteredSongs) {
                        filteredSongs.groupBy { it.artist }.map { (artistName, artistSongs) ->
                            val albumsForArtist = artistSongs.groupBy { it.album }.map { (albumName, albumSongs) ->
                                Album(
                                    id = albumName,
                                    title = albumName,
                                    artist = artistName,
                                    coverUri = albumSongs.firstOrNull()?.albumArtUri,
                                    songs = albumSongs
                                )
                            }

                            Artist(
                                id = artistName,
                                name = artistName,
                                imageUri = null,
                                albums = albumsForArtist,
                                songs = artistSongs
                            )
                        }
                    }

                    ArtistsScreen(
                        artists = artistsList,
                        onArtistClick = { artist -> },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== USER/SETTINGS SCREEN ====================
                composable("user") {
                    SettingsScreen(
                        appMode = appMode,
                        userPreferences = userPreferences,
                        accountType = accountType,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onAccountClick = {
                            navController.navigate("account")
                        },
                        onPlaybackClick = {
                            navController.navigate("playback_settings")
                        },
                        onStorageClick = {
                            Toast.makeText(context, "Storage screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onNotificationsClick = {
                            userPreferencesViewModel.toggleNotifications()
                            Toast.makeText(context, "Notifications ${if (userPreferences.showNotifications) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                        },
                        onAboutClick = {
                            Toast.makeText(context, "About screen coming soon", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDarkTheme = {
                            userPreferencesViewModel.toggleDarkTheme()
                            Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
                        },
                        onToggleDownload = {
                            userPreferencesViewModel.toggleDownloadOnlyOnWifi()
                            Toast.makeText(context, "Download settings updated", Toast.LENGTH_SHORT).show()
                        },
                        onClearCache = {
                            viewModel.clearCache()
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== ACCOUNT SCREEN ====================
                composable("account") {
                    var isLoading by remember { mutableStateOf(false) }
                    var isSyncing by remember { mutableStateOf(false) }

                    AccountScreen(
                        userPreferences = userPreferences,
                        accountType = accountType,
                        isLoading = isLoading,
                        isSyncing = isSyncing,
                        onLogin = { email, password ->
                            isLoading = true
                            scope.launch {
                                delay(1500)
                                isLoading = false

                                // Si es admin
                                if (email == "admin@musicapp.dev" && password == "MusicApp2024!") {
                                    userPreferencesViewModel.loginAsAdmin()
                                    Toast.makeText(context, "👑 Welcome back, Developer!", Toast.LENGTH_SHORT).show()
                                } else {
                                    userPreferencesViewModel.setAccountType(com.example.music.data.model.AccountType.LOCAL)
                                    Toast.makeText(context, "Logged in successfully", Toast.LENGTH_SHORT).show()
                                }
                                navController.navigateUp()
                            }
                        },
                        onRegister = { email, password, name ->
                            isLoading = true
                            scope.launch {
                                delay(1500)
                                isLoading = false
                                userPreferencesViewModel.setAccountType(com.example.music.data.model.AccountType.LOCAL)
                                Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                                navController.navigateUp()
                            }
                        },
                        onLogout = {
                            userPreferencesViewModel.setAccountType(com.example.music.data.model.AccountType.GUEST)
                            Toast.makeText(context, "Logged out - Guest mode", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        },
                        onSync = {
                            isSyncing = true
                            scope.launch {
                                delay(2000)
                                isSyncing = false
                                Toast.makeText(context, "Data synchronized", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYBACK SETTINGS SCREEN ====================
                composable("playback_settings") {
                    PlaybackSettingsScreen(
                        userPreferences = userPreferences,
                        onAudioQualityChange = { newQuality ->
                            userPreferencesViewModel.setAudioQuality(newQuality)
                            Toast.makeText(context, "Audio quality: ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onStreamingQualityChange = { newQuality ->
                            userPreferencesViewModel.setStreamingQuality(newQuality)
                            Toast.makeText(context, "Streaming quality: ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onDownloadQualityChange = { newQuality ->
                            userPreferencesViewModel.setDownloadQuality(newQuality)
                            Toast.makeText(context, "Download quality: ${newQuality.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onCrossfadeChange = { seconds ->
                            userPreferencesViewModel.setCrossfadeDuration(seconds)
                        },
                        onToggleGapless = {
                            userPreferencesViewModel.toggleGaplessPlayback()
                        },
                        onToggleNormalize = {
                            userPreferencesViewModel.toggleNormalizeVolume()
                        },
                        onEqualizerChange = { preset ->
                            userPreferencesViewModel.setEqualizerPreset(preset)
                            Toast.makeText(context, "Equalizer: ${preset.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }

                // ==================== PLAYER SCREEN ====================
                composable("player") {
                    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                    val shuffleEnabled by viewModel.shuffleEnabled.collectAsStateWithLifecycle()
                    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
                    val duration by viewModel.duration.collectAsStateWithLifecycle()
                    val isLoadingStream by viewModel.isLoadingStream.collectAsStateWithLifecycle()

                    val isLiked = remember(currentSong, favoriteSongIds, favoriteStreamingSongIds) {
                        currentSong?.let { song ->
                            if (song.isStreaming) {
                                val streamingId = song.streamingId ?: run {
                                    if (song.path.startsWith("streaming://")) {
                                        val parts = song.path.removePrefix("streaming://").split("/")
                                        if (parts.size >= 2) parts[1] else null
                                    } else null
                                }
                                streamingId?.let { favoriteStreamingSongIds.contains(it) } ?: false
                            } else {
                                favoriteSongIds.contains(song.id)
                            }
                        } ?: false
                    }

                    MusicPlayerScreen(
                        playerState = PlayerState(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            isShuffleEnabled = shuffleEnabled,
                            repeatMode = repeatMode,
                            isLiked = isLiked,
                            isLoadingStream = isLoadingStream
                        ),
                        appMode = appMode,
                        onToggleMode = { viewModel.toggleOnlineMode() },
                        onPlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onSkipPrevious = { viewModel.skipToPrevious() },
                        onSeek = { position -> viewModel.seekTo(position) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleLike = {
                            currentSong?.let { song ->
                                if (song.isStreaming) {
                                    val streamingId = song.streamingId ?: run {
                                        if (song.path.startsWith("streaming://")) {
                                            val parts = song.path.removePrefix("streaming://").split("/")
                                            if (parts.size >= 2) parts[1] else null
                                        } else null
                                    }

                                    streamingId?.let { id ->
                                        val provider = song.streamingProvider?.let { providerName ->
                                            when (providerName.uppercase()) {
                                                "INNERTUBE" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                "JIOSAAVN" -> com.example.music.data.api.MusicProviderType.JIOSAAVN
                                                "YOUTUBE_MUSIC" -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                                else -> com.example.music.data.api.MusicProviderType.INNERTUBE
                                            }
                                        } ?: com.example.music.data.api.MusicProviderType.INNERTUBE

                                        val streamingSong = com.example.music.data.model.StreamingSong(
                                            id = id,
                                            title = song.title,
                                            artist = song.artist,
                                            album = song.album,
                                            duration = song.duration,
                                            thumbnailUrl = song.albumArtUri,
                                            provider = provider
                                        )
                                        libraryViewModel.toggleStreamingFavorite(streamingSong)
                                    }
                                } else {
                                    libraryViewModel.toggleFavorite(song)
                                }
                            }
                        },
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}