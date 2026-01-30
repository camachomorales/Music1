package com.example.music.data.repository

import androidx.room.util.copy
import com.example.music.data.database.dao.SongDao
import com.example.music.data.model.LikeStatus
import com.example.music.data.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

package com.example.music.data.repository

import com.example.music.data.database.dao.SongDao
import com.example.music.data.model.LikeStatus
import com.example.music.data.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao
) {
    fun getRecentlyPlayed(): Flow<List<Song>> = songDao.getRecentlyPlayed()

    fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs()

    suspend fun addToHistory(song: Song) {
        songDao.insertSong(
            song.copy(lastPlayedAt = System.currentTimeMillis())
        )
    }

    suspend fun incrementPlayCount(songId: String) {
        songDao.incrementPlayCount(songId)
    }

    suspend fun updateLikeStatus(songId: String, status: LikeStatus) {
        songDao.updateLikeStatus(songId, status.name)
    }

    suspend fun getLyrics(songId: String): String? {
        // TODO: Implementar llamada a API de letras
        return null
    }

    suspend fun getSongById(id: String): Song? {
        return songDao.getSongById(id)
    }
}