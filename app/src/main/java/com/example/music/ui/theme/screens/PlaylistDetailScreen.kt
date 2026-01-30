package com.example.music.ui.theme.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.music.data.model.AppMode
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    allSongs: List<Song> = emptyList(),
    currentMode: AppMode = AppMode.OFFLINE,
    streamingSongs: List<StreamingSong> = emptyList(),
    isSearching: Boolean = false,
    onSearchStreaming: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onAddSong: (Song) -> Unit = {},
    onAddStreamingSong: (StreamingSong) -> Unit = {},
    onReorderSongs: (List<Song>) -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var songs by remember { mutableStateOf(playlist.songs) }

    LaunchedEffect(playlist.songs) {
        songs = playlist.songs
    }

    val streamingCount = songs.count { it.path.startsWith("streaming://") }
    val localCount = songs.count { !it.path.startsWith("streaming://") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = playlist.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when {
                            streamingCount > 0 && localCount > 0 ->
                                "${songs.size} songs ($streamingCount online • $localCount local)"
                            streamingCount > 0 ->
                                "${songs.size} songs ($streamingCount online)"
                            localCount > 0 ->
                                "${songs.size} songs ($localCount local)"
                            else ->
                                "${songs.size} songs"
                        },
                        fontSize = 13.sp,
                        color = if (streamingCount > 0) Color(0xFF00D9FF) else Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00D9FF).copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Playlist",
                    tint = Color(0xFF00D9FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Text(
                        text = "Empty Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Text(
                        text = "Tap Edit to add songs",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )

                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D9FF)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Songs")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D9FF)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All")
                }

                Button(
                    onClick = onShufflePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = songs,
                    key = { song -> song.id }
                ) { song ->
                    val index = songs.indexOf(song) + 1
                    PlaylistSongItem(
                        song = song,
                        index = index,
                        onClick = { onSongClick(song) },
                        onRemove = { onRemoveSong(song.id) }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            playlist = playlist,
            allSongs = allSongs,
            currentMode = currentMode,
            streamingSongs = streamingSongs,
            isSearching = isSearching,
            onSearchStreaming = onSearchStreaming,
            onDismiss = { showEditDialog = false },
            onAddSong = onAddSong,
            onAddStreamingSong = onAddStreamingSong,
            onRemoveSong = onRemoveSong,
            onReorderSongs = onReorderSongs
        )
    }
}

@Composable
fun PlaylistSongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            if (song.albumArtUri != null) {
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Remove from playlist") },
                    onClick = {
                        onRemove()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaylistDialog(
    playlist: Playlist,
    allSongs: List<Song>,
    currentMode: AppMode = AppMode.OFFLINE,
    streamingSongs: List<StreamingSong> = emptyList(),
    isSearching: Boolean = false,
    onSearchStreaming: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onAddSong: (Song) -> Unit,
    onAddStreamingSong: (StreamingSong) -> Unit = {},
    onRemoveSong: (Long) -> Unit,
    onReorderSongs: (List<Song>) -> Unit
) {
    var currentSongs by remember { mutableStateOf(playlist.songs.toMutableList()) }
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(playlist.songs) {
        currentSongs = playlist.songs.toMutableList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A2F3D)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Playlist",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = {
                        onReorderSongs(currentSongs)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { selectedTab = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) Color(0xFF00D9FF) else Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit (${currentSongs.size})")
                    }

                    Button(
                        onClick = { selectedTab = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) Color(0xFF00D9FF) else Color.White.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Songs")
                    }
                }

                if (selectedTab == 0) {
                    if (currentSongs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(60.dp)
                                )
                                Text(
                                    text = "No songs yet",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Go to 'Add Songs' tab",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = currentSongs,
                                key = { _, song -> song.id }
                            ) { index, song ->
                                EditableSongItem(
                                    song = song,
                                    index = index,
                                    totalSongs = currentSongs.size,
                                    onRemove = {
                                        onRemoveSong(song.id)
                                        currentSongs = currentSongs.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    onMoveUp = {
                                        if (index > 0) {
                                            currentSongs = currentSongs.toMutableList().apply {
                                                val temp = this[index]
                                                this[index] = this[index - 1]
                                                this[index - 1] = temp
                                            }
                                            onReorderSongs(currentSongs)
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < currentSongs.size - 1) {
                                            currentSongs = currentSongs.toMutableList().apply {
                                                val temp = this[index]
                                                this[index] = this[index + 1]
                                                this[index + 1] = temp
                                            }
                                            onReorderSongs(currentSongs)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (currentMode == AppMode.STREAMING && it.length >= 2) {
                                    onSearchStreaming(it)
                                }
                            },
                            placeholder = {
                                Text(
                                    if (currentMode == AppMode.STREAMING)
                                        "Search online..."
                                    else
                                        "Search local songs...",
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00D9FF))
                            },
                            trailingIcon = {
                                Row {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color(0xFF00D9FF),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00D9FF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF00D9FF)
                            ),
                            singleLine = true
                        )

                        when (currentMode) {
                            AppMode.OFFLINE -> {
                                val availableSongs = remember(allSongs, currentSongs, searchQuery) {
                                    allSongs.filter { song ->
                                        !currentSongs.any { it.id == song.id } &&
                                                (searchQuery.isEmpty() ||
                                                        song.title.contains(searchQuery, ignoreCase = true) ||
                                                        song.artist.contains(searchQuery, ignoreCase = true))
                                    }
                                }

                                if (availableSongs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isEmpty()) "All songs added" else "No songs found",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 16.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            count = availableSongs.size,
                                            key = { index -> availableSongs[index].id }
                                        ) { index ->
                                            AddSongItem(
                                                song = availableSongs[index],
                                                onAddClick = {
                                                    onAddSong(availableSongs[index])
                                                    currentSongs = currentSongs.toMutableList().apply {
                                                        add(availableSongs[index])
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            AppMode.STREAMING -> {
                                if (searchQuery.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(60.dp)
                                            )
                                            Text(
                                                text = "Search for songs online",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                } else if (isSearching) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF00D9FF))
                                    }
                                } else if (streamingSongs.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No songs found",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 16.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            count = streamingSongs.size,
                                            key = { index -> streamingSongs[index].id }
                                        ) { index ->
                                            AddStreamingSongItem(
                                                streamingSong = streamingSongs[index],
                                                onAddClick = {
                                                    onAddStreamingSong(streamingSongs[index])
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddStreamingSongItem(
    streamingSong: StreamingSong,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onAddClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )

            if (streamingSong.thumbnailUrl != null) {
                AsyncImage(
                    model = streamingSong.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = streamingSong.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = streamingSong.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = streamingSong.provider.displayName,
                color = Color(0xFF00D9FF),
                fontSize = 11.sp,
                modifier = Modifier
                    .background(
                        Color(0xFF00D9FF).copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add song",
                tint = Color(0xFF00D9FF)
            )
        }
    }
}

@Composable
fun EditableSongItem(
    song: Song,
    index: Int,
    totalSongs: Int,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (index > 0) Color(0xFF00D9FF) else Color.White.copy(0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalSongs - 1,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (index < totalSongs - 1) Color(0xFF00D9FF) else Color.White.copy(0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            if (song.albumArtUri != null) {
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Remove",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AddSongItem(
    song: Song,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onAddClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF00D9FF).copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )

            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D9FF).copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add song",
                tint = Color(0xFF00D9FF)
            )
        }
    }
}