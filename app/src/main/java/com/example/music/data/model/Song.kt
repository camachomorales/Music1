package com.example.music.data.model

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