package com.example.music.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.music.data.database.dao.QueueDao
import com.example.music.data.database.dao.SongDao
import com.example.music.data.model.PlaybackState
import com.example.music.data.model.QueueItem
import com.example.music.data.model.RepeatMode
import com.example.music.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

package com.example.music.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.music.data.database.dao.QueueDao
import com.example.music.data.database.dao.SongDao
import com.example.music.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepository @Inject constructor(
    private val queueDao: QueueDao,
    private val songDao: SongDao,
    private val preferences: SharedPreferences
) {
    suspend fun saveQueue(songs: List<Song>) {
        val queueItems = songs.mapIndexed { index, song ->
            QueueItem(songId = song.id, position = index)
        }
        queueDao.clearQueue()
        queueDao.insertQueueItems(queueItems)
    }

    suspend fun getQueue(): List<Song> {
        val queueItems = queueDao.getQueue()
        return queueItems.mapNotNull { item ->
            songDao.getSongById(item.songId)
        }
    }

    suspend fun savePlaybackState(state: PlaybackState) {
        preferences.edit {
            putString("current_song_id", state.currentSong?.id)
            putLong("current_position", state.currentPosition)
            putInt("repeat_mode", state.repeatMode.ordinal)
            putBoolean("shuffle_enabled", state.shuffleEnabled)
        }
    }

    suspend fun getLastPlaybackState(): PlaybackState? {
        val songId = preferences.getString("current_song_id", null) ?: return null
        val song = songDao.getSongById(songId) ?: return null

        return PlaybackState(
            currentSong = song,
            currentPosition = preferences.getLong("current_position", 0),
            repeatMode = RepeatMode.values()[preferences.getInt("repeat_mode", 0)],
            shuffleEnabled = preferences.getBoolean("shuffle_enabled", false)
        )
    }

    suspend fun updateCurrentSong(song: Song) {
        songDao.insertSong(song)
        preferences.edit {
            putString("current_song_id", song.id)
        }
    }

    suspend fun refreshStreamUrl(song: Song): Song {
        // TODO: Implementar llamada a InnerTube API
        // Por ahora retornamos la misma canción
        return song
    }
}