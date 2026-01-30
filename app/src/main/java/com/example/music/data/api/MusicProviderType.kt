// app/src/main/java/com/example/music/data/api/MusicProviderType.kt
package com.example.music.data.api

enum class MusicProviderType(val displayName: String) {
    YOUTUBE_MUSIC("YouTube Music (Piped)"),
    INNERTUBE("YouTube Music"),  // ✅ AGREGADO
    JIOSAAVN("JioSaavn"),
    SPOTIFY("Spotify")
}