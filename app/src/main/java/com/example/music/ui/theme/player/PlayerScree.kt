package com.example.music.ui.theme.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tunombre.music1.data.model.RepeatMode
import kotlin.math.roundToInt

/**
 * Pantalla principal del reproductor
 * UI inspirada en InnerTune/Material 3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val currentSong = playbackState.currentSong

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, "Cerrar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleQueueSheet() }) {
                        Icon(Icons.Default.QueueMusic, "Cola de reproducción")
                    }
                    IconButton(onClick = { /* Más opciones */ }) {
                        Icon(Icons.Default.MoreVert, "Más opciones")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contenido principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Artwork
                AlbumArtwork(
                    imageUrl = currentSong?.thumbnailUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Información de la canción
                SongInfo(
                    title = currentSong?.title ?: "No hay canción",
                    artist = currentSong?.artistName ?: "Desconocido",
                    isFavorite = uiState.isFavorite,
                    onFavoriteClick = { viewModel.toggleFavorite() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress bar
                ProgressSlider(
                    progress = progress,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Controles principales
                PlayerControls(
                    isPlaying = playbackState.isPlaying,
                    isBuffering = playbackState.isBuffering,
                    repeatMode = playbackState.repeatMode,
                    shuffleEnabled = playbackState.shuffleEnabled,
                    canSkipPrevious = uiState.canSkipPrevious,
                    canSkipNext = uiState.canSkipNext,
                    onPlayPause = { viewModel.playPause() },
                    onNext = { viewModel.skipToNext() },
                    onPrevious = { viewModel.skipToPrevious() },
                    onRepeat = { viewModel.toggleRepeatMode() },
                    onShuffle = { viewModel.toggleShuffle() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Controles adicionales
                AdditionalControls(
                    playbackSpeed = playbackState.playbackSpeed,
                    onSpeedClick = { viewModel.toggleSpeedSheet() },
                    onLyricsClick = { viewModel.toggleLyrics() }
                )
            }

            // Error snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Bottom sheets
    if (uiState.showQueueSheet) {
        QueueBottomSheet(
            queue = playbackState.queue,
            currentIndex = playbackState.currentIndex,
            onDismiss = { viewModel.toggleQueueSheet() },
            onItemClick = { /* Ir a canción */ },
            onRemove = { viewModel.removeFromQueue(it) }
        )
    }

    if (uiState.showSpeedSheet) {
        SpeedBottomSheet(
            currentSpeed = playbackState.playbackSpeed,
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { viewModel.toggleSpeedSheet() }
        )
    }
}

/**
 * Artwork del álbum con animación
 */
@Composable
private fun AlbumArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Album art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Información de la canción
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Barra de progreso con tiempo
 */
@Composable
private fun ProgressSlider(
    progress: Float,
    currentPosition: Long,
    duration: Long,
    onSeek: (Float) -> Unit
) {
    var tempProgress by remember { mutableStateOf<Float?>(null) }

    Column {
        Slider(
            value = tempProgress ?: progress,
            onValueChange = { tempProgress = it },
            onValueChangeFinished = {
                tempProgress?.let { onSeek(it) }
                tempProgress = null
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Controles principales de reproducción
 */
@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    repeatMode: RepeatMode,
    shuffleEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onShuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Previous
        FilledIconButton(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                modifier = Modifier.size(32.dp)
            )
        }

        // Play/Pause
        FilledTonalButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Next
        FilledIconButton(
            onClick = onNext,
            enabled = canSkipNext,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                modifier = Modifier.size(32.dp)
            )
        }

        // Repeat
        IconButton(onClick = onRepeat) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.OFF -> Icons.Default.Repeat
                    RepeatMode.ALL -> Icons.Default.Repeat
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                },
                contentDescription = "Repetir",
                tint = if (repeatMode != RepeatMode.OFF)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Controles adicionales (velocidad, letra)
 */
@Composable
private fun AdditionalControls(
    playbackSpeed: Float,
    onSpeedClick: () -> Unit,
    onLyricsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Velocidad
        OutlinedButton(
            onClick = onSpeedClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Velocidad",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("${playbackSpeed}x")
        }

        // Letra
        OutlinedButton(
            onClick = onLyricsClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Letra",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Letra")
        }
    }
}

/**
 * Formatear duración en mm:ss
 */
private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

/**
 * Bottom sheet de cola de reproducción
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    queue: List<com.tunombre.music1.data.model.Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Cola de reproducción (${queue.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            queue.forEachIndexed { index, song ->
                QueueItem(
                    song = song,
                    isPlaying = index == currentIndex,
                    onClick = { onItemClick(index) },
                    onRemove = { onRemove(index) }
                )
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: com.tunombre.music1.data.model.Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail pequeña
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName ?: "Desconocido",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, "Remover")
        }
    }
}

/**
 * Bottom sheet de velocidad de reproducción
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedBottomSheet(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Velocidad de reproducción",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            speeds.forEach { speed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSpeedChange(speed)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (speed == currentSpeed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionado",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}