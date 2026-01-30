package com.example.music.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music.data.model.AppMode

@Composable
fun LibraryScreen(
    currentMode: AppMode,
    onToggleMode: () -> Unit,
    onPlaylistsClick: () -> Unit = {},
    onSongsClick: () -> Unit = {},
    onAlbumsClick: () -> Unit = {},
    onArtistsClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onRecentlyPlayedClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Library",
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Library",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LibraryOptionCard(
                    icon = Icons.Default.QueueMusic,
                    title = "Playlists",
                    onClick = onPlaylistsClick
                )

                LibraryOptionCard(
                    icon = Icons.Default.MusicNote,
                    title = "Songs",
                    onClick = onSongsClick
                )

                LibraryOptionCard(
                    icon = Icons.Default.Album,
                    title = "Albums",
                    onClick = onAlbumsClick
                )

                LibraryOptionCard(
                    icon = Icons.Default.Person,
                    title = "Artists",
                    onClick = onArtistsClick
                )

                LibraryOptionCard(
                    icon = Icons.Default.Favorite,
                    title = "Favorites",
                    iconColor = Color(0xFFFF006E),
                    onClick = onFavoritesClick
                )

                LibraryOptionCard(
                    icon = Icons.Default.History,
                    title = "Recently Played",
                    onClick = onRecentlyPlayedClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Menú flotante en la esquina superior derecha
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, end = 8.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = { showMenu = !showMenu }
            ) {
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.width(220.dp)
            ) {
                // Toggle Online/Offline
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (currentMode == AppMode.STREAMING)
                                    Icons.Default.CloudOff
                                else
                                    Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Color(0xFF00D9FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (currentMode == AppMode.STREAMING)
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
            }
        }
    }
}

@Composable
fun LibraryOptionCard(
    icon: ImageVector,
    title: String,
    iconColor: Color = Color(0xFF00D9FF),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp)
        )
    }
}