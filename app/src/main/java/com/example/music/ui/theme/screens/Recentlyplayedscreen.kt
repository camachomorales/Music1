package com.example.music.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.music.data.model.AppMode
import com.example.music.data.model.Song

@Composable
fun RecentlyPlayedScreen(
    recentlyPlayedSongs: List<Song>,
    appMode: AppMode, // ✅ NUEVO: Para filtrar según modo
    favoriteSongIds: Set<Long>,
    favoriteStreamingSongIds: Set<String>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // ✅ Filtrar canciones según el modo actual
    val filteredSongs = remember(recentlyPlayedSongs, appMode) {
        when (appMode) {
            AppMode.OFFLINE -> recentlyPlayedSongs.filter { !it.isStreaming }
            AppMode.STREAMING -> recentlyPlayedSongs.filter { it.isStreaming }
        }
    }

    // ✅ Texto dinámico según el modo
    val modeText = when (appMode) {
        AppMode.OFFLINE -> "Local"
        AppMode.STREAMING -> "Streaming"
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Recently Played",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            // Menu button
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = Color(0xFF0A2F3D).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFF006E),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Clear $modeText history",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            showClearDialog = true
                        }
                    )
                }
            }
        }

        if (filteredSongs.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF00D9FF),
                        modifier = Modifier.size(60.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No $modeText tracks",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$modeText songs you play will show up here",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        } else {
            // Header Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00D9FF),
                                        Color(0xFF0099CC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Recently Played",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // ✅ Mostrar modo y cantidad
                        Text(
                            text = "$modeText • ${filteredSongs.size} ${if (filteredSongs.size == 1) "song" else "songs"}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Songs List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSongs, key = { it.id }) { song ->
                    // ✅ Detectar si es favorito (local o streaming)
                    val isFavorite = remember(song, favoriteSongIds, favoriteStreamingSongIds) {
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
                    }

                    RecentSongItem(
                        song = song,
                        isFavorite = isFavorite,
                        isCurrentSong = currentSong?.id == song.id,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        onSongClick = { onSongClick(song) },
                        onToggleFavorite = { onToggleFavorite(song) }
                    )
                }
            }
        }
    }

    // Clear History Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color(0xFF0A2F3D),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Clear $modeText history?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will remove all $modeText songs from your recently played list.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFFF006E),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        )
    }
}

@Composable
fun RecentSongItem(
    song: Song,
    isFavorite: Boolean,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentSong)
                    Color.White.copy(alpha = 0.15f)
                else
                    Color.White.copy(alpha = 0.05f)
            )
            .clickable(onClick = onSongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ✅ Artwork con imagen si está disponible
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            // ✅ Icono de fondo
            if (isCurrentSong && isPlaying) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(24.dp)
                )
            }

            // ✅ Imagen encima si existe
            if (song.albumArtUri != null && song.albumArtUri.isNotEmpty()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isCurrentSong) Color(0xFF00D9FF) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ✅ Mostrar artista sin "• Streaming" (ya sabemos el modo por el filtro)
            Text(
                text = song.artist,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (isFavorite) Color(0xFFFF006E) else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}