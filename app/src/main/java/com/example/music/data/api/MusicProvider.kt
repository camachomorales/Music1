// app/src/main/java/com/example/music/data/api/MusicProvider.kt
package com.example.music.data.api

import com.example.music.data.model.StreamingSong

interface MusicProvider {
    suspend fun search(query: String, limit: Int = 20): List<StreamingSong>
    suspend fun getStreamUrl(songId: String): String?
    suspend fun getTrending(limit: Int = 20): List<StreamingSong>
    suspend fun getRelated(songId: String, limit: Int = 20): List<StreamingSong>
    suspend fun getPlaylist(playlistId: String, limit: Int = 20): List<StreamingSong>
    fun getProviderType(): MusicProviderType
}
