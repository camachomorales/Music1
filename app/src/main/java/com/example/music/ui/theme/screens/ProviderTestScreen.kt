// app/src/main/java/com/example/music/ui/theme/screens/ProviderTestScreen.kt
package com.example.music.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.music.data.model.StreamingSong
import com.example.music.viewmodel.MusicPlayerViewModel

@Composable
fun ProviderTestScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val streamingSongs by viewModel.streamingSongs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Prueba de Proveedores", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar canción") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                viewModel.searchStreamingSongs(searchQuery)
            }) {
                Text("Buscar")
            }

            Button(onClick = {
                viewModel.getTrending() // ✅ CORREGIDO
            }) {
                Text("Trending")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(streamingSongs) { song ->
                SongTestCard(
                    song = song,
                    onClick = { viewModel.playStreamingSong(song) }
                )
            }
        }
    }
}

@Composable
fun SongTestCard(song: StreamingSong, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyLarge)
                Text(song.artist, style = MaterialTheme.typography.bodyMedium)
                Text("[${song.provider.displayName}]", style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = onClick) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
        }
    }
}