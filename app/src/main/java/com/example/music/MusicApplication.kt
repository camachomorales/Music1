package com.example.music

import android.app.Application
import android.util.Log

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("MusicApp", "🚀 Application onCreate()")

        // Capturar crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MusicApp", "💥 CRASH NO CAPTURADO 💥", throwable)
            Log.e("MusicApp", "Thread: ${thread.name}")
            Log.e("MusicApp", "Message: ${throwable.message}")
            throwable.printStackTrace()
        }
    }
}