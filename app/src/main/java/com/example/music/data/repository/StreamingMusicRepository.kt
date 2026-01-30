package com.example.music.data.repository

import android.util.Log
import com.example.music.data.api.*
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamingMusicRepository {

    private val TAG = "StreamingMusicRepo"

    // ✅ USAR EL MANAGER EXISTENTE
    private val providerManager = MusicProviderManager()

    init {
        // Configurar providers habilitados (solo los que existen)
        providerManager.setEnabledProviders(
            setOf(
                MusicProviderType.INNERTUBE,
                MusicProviderType.YOUTUBE_MUSIC
                // ❌ Quitamos JIOSAAVN porque no está implementado
            )
        )
    }

    suspend fun getStreamUrl(song: StreamingSong): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream URL: ${song.title} [${song.provider.displayName}]")

            val streamUrl = providerManager.getStreamUrl(song.id, song.provider)

            if (streamUrl != null) {
                Log.d(TAG, "✅ Stream URL obtenida: ${streamUrl.take(100)}...")
                Log.d(TAG, "⏱️ URL válida por ~6 horas")
            } else {
                Log.e(TAG, "❌ No se pudo obtener stream URL")
            }

            streamUrl
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL", e)
            null
        }
    }

    suspend fun search(query: String, limit: Int = 20): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                Log.w(TAG, "⚠️ Query vacía")
                return@withContext emptyList()
            }

            Log.d(TAG, "🔍 Buscando: '$query'")

            // Usar el manager para buscar
            val results = providerManager.search(query, limit)

            Log.d(TAG, "✅ Encontradas ${results.size} canciones")
            results

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda", e)
            emptyList()
        }
    }

    suspend fun getTrending(limit: Int = 30): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔥 Obteniendo trending...")
            providerManager.getTrending(limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo trending", e)
            emptyList()
        }
    }
}