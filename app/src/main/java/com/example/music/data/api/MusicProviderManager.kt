// app/src/main/java/com/example/music/data/api/MusicProviderManager.kt
package com.example.music.data.api

import android.util.Log
import com.example.music.data.api.providers.InnerTubeProvider
import com.example.music.data.api.providers.JioSaavnProvider
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.*

class MusicProviderManager {

    private val TAG = "ProviderManager"

    // Mapa de todos los providers disponibles
    private val allProviders: Map<MusicProviderType, MusicProvider> = mapOf(
        MusicProviderType.INNERTUBE to InnerTubeProvider(),
        MusicProviderType.JIOSAAVN to JioSaavnProvider()
    )

    // Providers activos
    private var enabledProviders: Set<MusicProviderType> = setOf(
        MusicProviderType.INNERTUBE,
        MusicProviderType.JIOSAAVN
    )

    fun setEnabledProviders(providers: Set<MusicProviderType>) {
        enabledProviders = providers
        Log.d(TAG, "✅ Providers activos: ${providers.map { it.displayName }}")
    }

    suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        val activeProviders = enabledProviders.mapNotNull { allProviders[it] }

        if (activeProviders.isEmpty()) {
            Log.w(TAG, "⚠️ No hay providers activos")
            return@withContext emptyList<StreamingSong>()
        }

        Log.d(TAG, "🔍 Buscando '$query' en ${activeProviders.size} providers...")

        // Buscar en todos los providers en paralelo
        val results = activeProviders.map { provider ->
            async {
                try {
                    provider.search(query, limit)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en ${provider.getProviderType().displayName}: ${e.message}")
                    emptyList<StreamingSong>()
                }
            }
        }.awaitAll().flatten()

        Log.d(TAG, "✅ Total: ${results.size} canciones encontradas")
        results
    }

    suspend fun getStreamUrl(songId: String, providerType: MusicProviderType): String? = withContext(Dispatchers.IO) {
        try {
            val provider = allProviders[providerType]
            if (provider == null) {
                Log.e(TAG, "❌ Provider no encontrado: ${providerType.displayName}")
                return@withContext null
            }

            provider.getStreamUrl(songId)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL: ${e.message}", e)
            null
        }
    }

    suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        val activeProviders = enabledProviders.mapNotNull { allProviders[it] }

        if (activeProviders.isEmpty()) {
            return@withContext emptyList<StreamingSong>()
        }

        val results = activeProviders.map { provider ->
            async {
                try {
                    provider.getTrending(limit)
                } catch (e: Exception) {
                    Log.e(TAG, "Error en ${provider.getProviderType().displayName}: ${e.message}")
                    emptyList<StreamingSong>()
                }
            }
        }.awaitAll().flatten()

        results
    }
}