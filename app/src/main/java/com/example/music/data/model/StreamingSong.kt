// app/src/main/java/com/example/music/data/model/StreamingSong.kt
package com.example.music.data.model

import com.example.music.data.api.MusicProviderType

enum class AppMode {
    OFFLINE,    // Solo música local
    STREAMING   // YouTube Music, Spotify, JioSaavn
}

data class StreamingSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val thumbnailUrl: String? = null,
    val provider: MusicProviderType,
    val externalUrl: String? = null
)

