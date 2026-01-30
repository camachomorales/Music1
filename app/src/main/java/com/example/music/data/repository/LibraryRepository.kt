package com.example.music.data.repository

import android.content.Context
import com.example.music.data.model.Playlist
import com.example.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class LibraryRepository(private val context: Context) {

    // En memoria por ahora - después puedes usar Room Database
    private val playlists = mutableListOf<Playlist>()

    // ==================== FAVORITES (MIXTO) ====================
    private val favoriteSongs = mutableSetOf<Long>() // IDs de canciones locales
    private val favoriteStreamingSongs = mutableSetOf<String>() // IDs de streaming songs

    // ==================== RECENTLY PLAYED ====================
    private val recentlyPlayed = mutableListOf<Pair<Song, Long>>() // (Song, timestamp)
    private val MAX_RECENTLY_PLAYED = 50

    // ==================== PLAYLISTS ====================

    suspend fun getAllPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        playlists.toList()
    }

    suspend fun getPlaylistById(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        playlists.find { it.id == playlistId }
    }

    suspend fun createPlaylist(name: String, description: String): Playlist = withContext(Dispatchers.IO) {
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            songs = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        playlists.add(newPlaylist)
        newPlaylist
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlists.removeIf { it.id == playlistId }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) = withContext(Dispatchers.IO) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            playlists[index] = playlist.copy(name = newName)
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song) = withContext(Dispatchers.IO) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            // Solo agregar si la canción no está ya en la playlist
            if (!playlist.songs.any { it.id == song.id }) {
                val updatedSongs = playlist.songs + song
                playlists[index] = playlist.copy(songs = updatedSongs)
            }
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long) = withContext(Dispatchers.IO) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            val updatedSongs = playlist.songs.filter { it.id != songId }
            playlists[index] = playlist.copy(songs = updatedSongs)
        }
    }

    suspend fun reorderPlaylistSongs(playlistId: String, newOrder: List<Song>) = withContext(Dispatchers.IO) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            playlists[index] = playlist.copy(songs = newOrder)
        }
    }

    suspend fun addSongsToPlaylist(playlistId: String, songs: List<Song>) = withContext(Dispatchers.IO) {
        val index = playlists.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val playlist = playlists[index]
            val existingIds = playlist.songs.map { it.id }.toSet()
            val newSongs = songs.filter { it.id !in existingIds }
            val updatedSongs = playlist.songs + newSongs
            playlists[index] = playlist.copy(songs = updatedSongs)
        }
    }

    // ==================== FAVORITES (LOCAL SONGS) ====================

    suspend fun addToFavorites(songId: Long) = withContext(Dispatchers.IO) {
        favoriteSongs.add(songId)
    }

    suspend fun removeFromFavorites(songId: Long) = withContext(Dispatchers.IO) {
        favoriteSongs.remove(songId)
    }

    suspend fun isFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        favoriteSongs.contains(songId)
    }

    suspend fun getFavoriteSongIds(): Set<Long> = withContext(Dispatchers.IO) {
        favoriteSongs.toSet()
    }

    suspend fun toggleFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        if (favoriteSongs.contains(songId)) {
            favoriteSongs.remove(songId)
            false
        } else {
            favoriteSongs.add(songId)
            true
        }
    }

    // ==================== FAVORITES (STREAMING SONGS) ====================

    suspend fun addStreamingToFavorites(streamingSongId: String) = withContext(Dispatchers.IO) {
        favoriteStreamingSongs.add(streamingSongId)
    }

    suspend fun removeStreamingFromFavorites(streamingSongId: String) = withContext(Dispatchers.IO) {
        favoriteStreamingSongs.remove(streamingSongId)
    }

    suspend fun isStreamingFavorite(streamingSongId: String): Boolean = withContext(Dispatchers.IO) {
        favoriteStreamingSongs.contains(streamingSongId)
    }

    suspend fun getFavoriteStreamingSongIds(): Set<String> = withContext(Dispatchers.IO) {
        favoriteStreamingSongs.toSet()
    }

    suspend fun toggleStreamingFavorite(streamingSongId: String): Boolean = withContext(Dispatchers.IO) {
        if (favoriteStreamingSongs.contains(streamingSongId)) {
            favoriteStreamingSongs.remove(streamingSongId)
            false
        } else {
            favoriteStreamingSongs.add(streamingSongId)
            true
        }
    }

    // ==================== RECENTLY PLAYED ====================

    suspend fun addToRecentlyPlayed(song: Song) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()

        // Remover la canción si ya existe
        recentlyPlayed.removeAll { it.first.id == song.id }

        // Agregar al inicio
        recentlyPlayed.add(0, Pair(song, timestamp))

        // Mantener solo las últimas MAX_RECENTLY_PLAYED canciones
        if (recentlyPlayed.size > MAX_RECENTLY_PLAYED) {
            recentlyPlayed.removeAt(recentlyPlayed.size - 1)
        }
    }

    suspend fun getRecentlyPlayedSongs(): List<Song> = withContext(Dispatchers.IO) {
        recentlyPlayed.map { it.first }
    }

    suspend fun clearRecentlyPlayed() = withContext(Dispatchers.IO) {
        recentlyPlayed.clear()
    }
}