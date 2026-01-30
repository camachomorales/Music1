package com.example.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.music.ui.theme.MusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint  // ✅ AGREGAR ESTA ANOTACIÓN
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicTheme {
                MainScreen()
            }
        }
    }
}