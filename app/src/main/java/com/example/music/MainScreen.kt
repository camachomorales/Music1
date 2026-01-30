package com.example.music

import androidx.compose.runtime.Composable
import com.example.music.ui.theme.player.MiniPlayer
import com.example.music.ui.theme.player.PlayerScreen

package com.example.music.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.music.ui.player.MiniPlayer
import com.example.music.ui.player.PlayerScreen
import com.example.music.ui.player.PlayerViewModel

@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playbackState by playerViewModel.playbackState.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    var showFullPlayer by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            MiniPlayer(
                playbackState = playbackState,
                progress = progress,
                onExpand = { showFullPlayer = true },
                onPlayPause = { playerViewModel.playPause() },
                onNext = { playerViewModel.skipToNext() }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // ✅ AQUÍ VA TU UI EXISTENTE
            // Por ahora un placeholder:
            Text("Tu contenido aquí")
        }
    }

    // Pantalla completa del player
    if (showFullPlayer) {
        PlayerScreen(
            viewModel = playerViewModel,
            onDismiss = { showFullPlayer = false }
        )
    }
}