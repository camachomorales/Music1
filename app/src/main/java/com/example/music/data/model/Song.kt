package com.example.music.data.model

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.music.ui.theme.player.PlayerViewModel

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String? = null,
    val isStreaming: Boolean = false,
    val streamingId: String? = null,      // ✅ NUEVO - debe ser nullable
    val streamingProvider: String? = null  // ✅ NUEVO - debe ser nullable
)

// En tu SongListItem o donde sea que tengas las canciones
@Composable
fun SongItem(
    song: Song,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    Row(
        modifier = Modifier
            .clickable {
                viewModel.playSong(song)
            }
    ) {
        // ... tu UI de SongItem existente
    }
}