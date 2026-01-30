package com.example.music.data.database.dao

import androidx.room.OnConflictStrategy
import com.example.music.data.model.QueueItem
import retrofit2.http.Query
import androidx.room.*


@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position")
    suspend fun getQueue(): List<QueueItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<QueueItem>)

    @Query("DELETE FROM queue")
    suspend fun clearQueue()

    @Query("DELETE FROM queue WHERE songId = :songId")
    suspend fun removeFromQueue(songId: String)
}