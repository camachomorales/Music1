package com.example.music.ui.theme.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Mini reproductor que se muestra en la parte inferior de la app
 * Inspirado en InnerTune
 */
@Composable
fun MiniPlayer(
    playbackState: com.tunombre.music1.data.model.PlaybackState,
    progress: Float,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong = playbackState.currentSong

    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                // Barra de progreso delgada
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thumbnail
                    MiniPlayerArtwork(
                        imageUrl = currentSong?.thumbnailUrl,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Información de la canción
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentSong?.title ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = currentSong?.artistName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Controles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause
                        IconButton(onClick = onPlayPause) {
                            if (playbackState.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (playbackState.isPlaying)
                                        Icons.Default.Pause
                                    else
                                        Icons.Default.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying)
                                        "Pausar"
                                    else
                                        "Reproducir"
                                )
                            }
                        }

                        // Next
                        IconButton(onClick = onNext) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Artwork para el mini player
 */
@Composable
private fun MiniPlayerArtwork(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
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
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Versión expandible del mini player que muestra en el home
 */
@Composable
fun ExpandableMiniPlayer(
    playbackState: com.tunombre.music1.data.model.PlaybackState,
    progress: Float,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = playbackState.currentSong != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column {
                // Barra de progreso
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                )

                // Contenido colapsado (siempre visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isExpanded) onOpenFullPlayer()
                            else onExpandChange(false)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniPlayerArtwork(
                        imageUrl = playbackState.currentSong?.thumbnailUrl,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playbackState.currentSong?.title ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = playbackState.currentSong?.artistName ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (playbackState.isPlaying)
                                    Icons.Default.Pause
                                else
                                    Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        }

                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipNext, null)
                        }

                        IconButton(onClick = { onExpandChange(!isExpanded) }) {
                            Icon(
                                imageVector = if (isExpanded)
                                    Icons.Default.KeyboardArrowDown
                                else
                                    Icons.Default.KeyboardArrowUp,
                                contentDescription = null
                            )
                        }
                    }
                }

                // Contenido expandido
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Cola de reproducción en miniatura
                        Text(
                            text = "Siguiente en la cola",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        playbackState.queue.drop(playbackState.currentIndex + 1).take(3).forEach { song ->
                            QueueItemMini(song = song)
                        }

                        if (playbackState.queue.size > playbackState.currentIndex + 4) {
                            Text(
                                text = "+ ${playbackState.queue.size - playbackState.currentIndex - 4} más",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // Botón para abrir reproductor completo
                        TextButton(
                            onClick = onOpenFullPlayer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ver reproductor completo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemMini(
    song: com.tunombre.music1.data.model.Song
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(song.durationSeconds * 1000L),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}