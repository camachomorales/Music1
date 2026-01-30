package com.example.music.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.music.data.database.dao.QueueDao
import com.example.music.data.database.dao.SongDao
import com.example.music.data.model.QueueItem
import com.example.music.data.model.Song

@Database(
    entities = [Song::class, QueueItem::class],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun queueDao(): QueueDao
}