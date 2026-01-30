package com.example.music.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.music.data.model.AppMode
import com.example.music.data.model.Song
import com.example.music.data.model.RepeatMode
import androidx.compose.ui.tooling.preview.Preview
import com.example.music.ui.theme.MusicTheme
import com.example.music.R

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun PreviewMusicPlayerScreen() {
    MusicTheme {
        MusicPlayerScreen(
            playerState = PlayerState(
                currentSong = Song(1, "Blinding Lights", "The Weeknd", "After Hours", 200000, "", null),
                isPlaying = true,
                currentPosition = 90000,
                duration = 200000,
                isShuffleEnabled = false,
                repeatMode = RepeatMode.ALL,
                isLiked = true,
                isLoadingStream = false
            ),
            appMode = AppMode.OFFLINE,
            onToggleMode = {},
            onBackClick = {},
            onPlayPause = {},
            onSkipNext = {},
            onSkipPrevious = {},
            onSeek = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onToggleLike = {}
        )
    }
}

@Composable
fun MusicPlayerScreen(
    playerState: PlayerState,
    appMode: AppMode,
    onToggleMode: () -> Unit,
    onBackClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF00D9FF)
    val textColor = Color.White
    val secondaryText = Color.White.copy(alpha = 0.6f)

    val shuffleColor = if (playerState.isShuffleEnabled) accentColor else secondaryText
    val repeatColor = when (playerState.repeatMode) {
        RepeatMode.OFF -> secondaryText
        RepeatMode.ALL -> accentColor
        RepeatMode.ONE -> accentColor
    }

    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header con botones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Player",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isLiked)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (playerState.isLiked)
                            Color(0xFFFF6B6B)
                        else
                            textColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Album Art con título y artista superpuestos
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    // Contenedor del álbum
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(32.dp)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0B5A6B).copy(alpha = 0.5f),
                                        Color(0xFF051E26).copy(alpha = 0.5f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // ✅ Estado para saber si la imagen cargó
                        val artUri = playerState.currentSong?.albumArtUri
                        val currentSongId = playerState.currentSong?.id

                        // ✅ KEY CRÍTICO: Usar key() para recrear todo el estado cuando cambia la canción
                        key(currentSongId, artUri) {
                            var imageLoaded by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // ✅ CAPA 1: Icono de fondo (SOLO si no hay imagen O no cargó)
                                if (!imageLoaded) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Fondo musical",
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(140.dp)
                                    )
                                }

                                // ✅ CAPA 2: Imagen del álbum (si existe)
                                if (artUri != null && artUri.isNotEmpty()) {
                                    AsyncImage(
                                        model = artUri,
                                        contentDescription = "Album Art",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.3f,
                                        onSuccess = { imageLoaded = true },
                                        onError = { imageLoaded = false }
                                    )
                                }
                            }
                        }

                        // ✅ CAPA 3: Título y artista (encima del fondo)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(80.dp))
                            Text(
                                text = playerState.currentSong?.title ?: "No Song",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playerState.currentSong?.artist ?: "Unknown Artist",
                                fontSize = 16.sp,
                                color = secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }

                        // ✅ CAPA 4: Loading overlay
                        if (playerState.isLoadingStream) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 4.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Cargando...",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Botón de Play/Pause flotante
                    Button(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 38.dp)
                            .size(76.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 4.dp
                        ),
                        enabled = !playerState.isLoadingStream
                    ) {
                        if (playerState.isLoadingStream) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color(0xFF051E26),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying)
                                    Icons.Default.Pause
                                else
                                    Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = Color(0xFF051E26),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Controles de reproducción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = shuffleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(36.dp),
                    enabled = !playerState.isLoadingStream
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_previous),
                        contentDescription = "Previous",
                        tint = if (playerState.isLoadingStream)
                            textColor.copy(alpha = 0.3f)
                        else
                            textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(36.dp),
                    enabled = !playerState.isLoadingStream
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Next",
                        tint = if (playerState.isLoadingStream)
                            textColor.copy(alpha = 0.3f)
                        else
                            textColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = when (playerState.repeatMode) {
                                RepeatMode.ONE -> "Repeat One"
                                RepeatMode.ALL -> "Repeat All"
                                RepeatMode.OFF -> "Repeat Off"
                            },
                            tint = repeatColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (playerState.repeatMode == RepeatMode.ONE) {
                            Text(
                                text = "1",
                                fontSize = 7.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barra de progreso
            ProgressBar(
                currentPosition = playerState.currentPosition,
                duration = playerState.duration,
                onSeek = onSeek,
                accentColor = accentColor
            )
            // Info del álbum
            Text(
                text = "Album: ${playerState.currentSong?.album?.takeIf { it.isNotBlank() } ?: "Unknown Album"}",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
/*
            // Info del álbum
            val shouldShowAlbum = remember(playerState.currentSong?.album) {
                val album = playerState.currentSong?.album ?: ""
                album.isNotBlank() &&
                        !album.equals("Video", ignoreCase = true) &&
                        !album.equals("Episodio", ignoreCase = true) &&
                        !album.equals("Canción", ignoreCase = true) &&
                        !album.equals("Unknown Album", ignoreCase = true)
            }
            if (shouldShowAlbum) {
                Text(
                    text = "Album: ${playerState.currentSong?.album}",
                    fontSize = 13.sp,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }*/

            Spacer(modifier = Modifier.height(8.dp))

            // Footer con indicador de modo y menú
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    val modeText = when {
                        playerState.repeatMode == RepeatMode.ONE && playerState.isShuffleEnabled ->
                            "🔂 Repetir Uno (shuffle sin efecto)"
                        playerState.repeatMode == RepeatMode.ONE ->
                            "🔂 Repetir Uno"
                        playerState.repeatMode == RepeatMode.ALL && playerState.isShuffleEnabled ->
                            "🔀🔁 Aleatorio + Repetir"
                        playerState.repeatMode == RepeatMode.ALL ->
                            "🔁 Repetir Todo"
                        playerState.isShuffleEnabled ->
                            "🔀 Aleatorio"
                        else ->
                            "▶️ Normal"
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    // Menú con DropdownMenu
                    Box {
                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = Color(0xFF0A2F3D).copy(alpha = 0.95f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (appMode == AppMode.STREAMING)
                                                Icons.Default.CloudOff
                                            else
                                                Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (appMode == AppMode.STREAMING)
                                                "Cambiar a Offline"
                                            else
                                                "Cambiar a Online",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = {
                                    onToggleMode()
                                    showMenu = false
                                }
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Ver cola",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Compartir",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false }
                            )

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Album,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Ir al álbum",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Ir al artista",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    accentColor: Color
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((duration * newProgress).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((duration * newProgress).toLong())
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(4.dp)
            ) {
                val centerY = size.height / 2f

                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLine(
                    color = accentColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width * progress, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = accentColor,
                    radius = 8.dp.toPx(),
                    center = Offset(size.width * progress, centerY)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

data class PlayerState(
    val currentSong: Song?,
    val isPlaying: Boolean,
    val currentPosition: Long,
    val duration: Long,
    val isShuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
    val isLiked: Boolean,
    val isLoadingStream: Boolean = false
)