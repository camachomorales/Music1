package com.example.music.data.database.dao

import androidx.room.*
import androidx.room.Dao
import com.example.music.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("SELECT * FROM songs ORDER BY lastPlayedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE likeStatus = 'LIKE' ORDER BY addedAt DESC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE songs SET likeStatus = :status WHERE id = :id")
    suspend fun updateLikeStatus(id: String, status: String)

    @Delete
    suspend fun deleteSong(song: Song)
}