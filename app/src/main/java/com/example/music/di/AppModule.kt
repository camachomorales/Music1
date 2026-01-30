package com.example.music.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.music.data.database.MusicDatabase
import com.example.music.data.database.dao.QueueDao
import com.example.music.data.database.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusicDatabase {
        return Room.databaseBuilder(
            context,
            MusicDatabase::class.java,
            "music_database"
        ).build()
    }

    @Provides
    fun provideSongDao(database: MusicDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun provideQueueDao(database: MusicDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
    }
}