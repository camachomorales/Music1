package com.example.music.utils

import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.Song
import com.example.music.data.model.StreamingSong

/**
 * Convierte un StreamingSong a Song con path normalizado para streaming
 *
 * IMPORTANTE: Normaliza el provider scheme a valores que el MusicService puede resolver:
 * - YOUTUBE_MUSIC, INNERTUBE → "youtube"
 * - JIOSAAVN → "jiosaavn"
 * - SPOTIFY → "spotify"
 *
 * También limpia el ID removiendo prefijos como "yt:"
 */
fun StreamingSong.toSong(albumOverride: String? = null): Song {
    // ✅ Normalizar provider scheme para compatibilidad con MusicService
    val providerScheme = when (this.provider) {
        MusicProviderType.YOUTUBE_MUSIC -> "youtube"
        MusicProviderType.INNERTUBE -> "youtube"
        MusicProviderType.JIOSAAVN -> "jiosaavn"
        MusicProviderType.SPOTIFY -> "spotify"
        else -> "youtube" // Default fallback
    }

    // ✅ Limpiar ID: remover prefijos como "yt:"
    val cleanId = this.id.removePrefix("yt:")

    // ✅ Construir path normalizado
    val streamingPath = "streaming://${providerScheme}/${cleanId}"

    return Song(
        id = this.id.hashCode().toLong(),
        title = this.title,
        artist = this.artist,
        album = albumOverride ?: this.album ?: this.artist,
        duration = this.duration,
        path = streamingPath,
        albumArtUri = this.thumbnailUrl,
        isStreaming = true,
        streamingId = cleanId,
        streamingProvider = this.provider.name
    )
}

/**
 * Extrae el ID de video limpio desde un path de streaming
 * Soporta múltiples formatos y limpia prefijos
 *
 * Ejemplos:
 * - "streaming://youtube/ABC123" -> "ABC123"
 * - "streaming://youtube_music/yt:ABC123" -> "ABC123"
 * - "streaming://innertube/ABC123" -> "ABC123"
 */
fun getVideoIdFromPath(path: String): String? {
    val id = when {
        path.startsWith("streaming://youtube/") ->
            path.removePrefix("streaming://youtube/")
        path.startsWith("streaming://youtube_music/") ->
            path.removePrefix("streaming://youtube_music/")
        path.startsWith("streaming://innertube/") ->
            path.removePrefix("streaming://innertube/")
        path.startsWith("streaming://jiosaavn/") ->
            path.removePrefix("streaming://jiosaavn/")
        path.startsWith("streaming://spotify/") ->
            path.removePrefix("streaming://spotify/")
        else -> null
    }

    // Limpiar el ID de prefijos adicionales
    return id?.removePrefix("yt:")
}

/**
 * Verifica si una canción es de streaming basándose en su path
 */
fun isStreamingSong(path: String): Boolean {
    return path.startsWith("streaming://")
}