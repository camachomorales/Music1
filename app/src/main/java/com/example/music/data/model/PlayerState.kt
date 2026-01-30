package com.example.music.data.model



enum class RepeatMode {
    OFF,        // No repetir
    ALL,        // Repetir toda la lista
    ONE         // Repetir una canción
}

enum class PlayerState {
    IDLE,
    PLAYING,
    PAUSED,
    LOADING,
    ERROR
}

data class PlayerStatedata(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLiked: Boolean = false,
    val playlist: List<Song> = emptyList()
)



