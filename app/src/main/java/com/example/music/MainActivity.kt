package com.example.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.music.data.repository.MusicRepository
import com.example.music.ui.theme.MainNavigation
import com.example.music.ui.theme.MusicTheme
import com.example.music.ui.theme.screens.ProviderTestScreen
import com.example.music.viewmodel.MusicPlayerViewModel
import com.example.music.viewmodel.MusicPlayerViewModelFactory

class MainActivity : ComponentActivity() {

    private val musicRepository by lazy { MusicRepository(this) }

    // ✅ Usar Factory para crear el ViewModel
    private val viewModel: MusicPlayerViewModel by viewModels {
        MusicPlayerViewModelFactory(application, musicRepository)
    }

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Audio permission granted")
            viewModel.loadLocalSongs()
            Toast.makeText(this, "Loading music...", Toast.LENGTH_SHORT).show()
        } else {
            Log.w("MainActivity", "❌ Audio permission denied")
            Toast.makeText(
                this,
                "Permission denied. Local music won't be available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Notification permission granted")
        } else {
            Log.w("MainActivity", "❌ Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("MainActivity", "🚀 App iniciada")
        checkAndRequestPermissions()

        setContent {
            MusicTheme {
                val songs by viewModel.songs.collectAsStateWithLifecycle()
                MainNavigation(viewModel = viewModel, songs = songs)

                // ✅ AGREGAR ESTO PARA PROBAR
               // ProviderTestScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // Permiso para leer audio
        val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, audioPermission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "✅ Audio permission already granted")
                viewModel.loadLocalSongs()
            }
            else -> {
                Log.d("MainActivity", "📋 Requesting audio permission")
                requestAudioPermissionLauncher.launch(audioPermission)
            }
        }

        // Permiso para notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "📋 Requesting notification permission")
                requestNotificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
}