package com.example.music.data.api.providers

import android.util.Log
import com.example.music.data.api.MusicProvider
import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.StreamingSong
import com.example.music.innertube.InnerTube
import com.example.music.innertube.models.YouTubeClient
import com.example.music.innertube.models.response.PlayerResponse
import com.example.music.innertube.models.response.SearchResponse
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider que usa la carpeta innertube/ completa de OuterTune
 * Requiere Ktor + Kotlin Serialization
 */
class InnerTubeProvider : MusicProvider {

    private val TAG = "InnerTubeNative"
    private val innerTube = InnerTube()

    override suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando en InnerTube: $query")

            val response = innerTube.search(
                client = YouTubeClient.WEB_REMIX,
                query = query,
                params = null,
                continuation = null
            )

            val searchResponse: SearchResponse = response.body()
            val songs = mutableListOf<StreamingSong>()

            searchResponse.contents?.tabbedSearchResultsRenderer?.tabs
                ?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.forEach { content ->
                    content.musicShelfRenderer?.contents?.forEach { item ->
                        item.musicResponsiveListItemRenderer?.let { renderer ->
                            val videoId = renderer.playlistItemData?.videoId ?: return@let

                            // Título (flexColumn 0)
                            val title = renderer.flexColumns.firstOrNull()
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                                ?.firstOrNull()?.text ?: "Unknown"

                            // flexColumn 1 contiene información separada por "•"
                            // Estructura: [Tipo, "•", Artista, "•", Info extra]
                            // Ejemplo: ["Video", " • ", "Guns N' Roses", " • ", "2390 M de vistas"]
                            // O para canciones: ["Canción", " • ", "Guns N' Roses", " • ", "Álbum"]
                            val secondColumn = renderer.flexColumns.getOrNull(1)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs

                            var artist = "Unknown Artist"
                            var album: String? = null

                            if (secondColumn != null && secondColumn.isNotEmpty()) {
                                // Filtrar solo elementos pares (los impares son separadores "•")
                                val dataElements = secondColumn.filterIndexed { index, _ -> index % 2 == 0 }

                                when {
                                    // ✅ CASO 1: Tipo conocido en primera posición
                                    // Estructura: [Tipo, Artista, Álbum/Info]
                                    // Ejemplo: ["Video", "Guns N' Roses", "..."]
                                    //          ["Canción", "Guns N' Roses", "Use Your Illusion I"]
                                    //          ["Episodio", "Artista", "..."]
                                    dataElements.size >= 2 &&
                                            (dataElements[0].text.equals("Video", ignoreCase = true) ||
                                                    dataElements[0].text.equals("Canción", ignoreCase = true) ||
                                                    dataElements[0].text.equals("Episodio", ignoreCase = true)) -> {
                                        // El artista está en la posición 1
                                        artist = dataElements.getOrNull(1)?.text ?: "Unknown Artist"

                                        // ✅ Filtrar información de vistas del tercer elemento
                                        val thirdElement = dataElements.getOrNull(2)?.text
                                        album = if (thirdElement != null) {
                                            val isViewsInfo = thirdElement.contains(" M ", ignoreCase = true) ||
                                                    thirdElement.contains(" K ", ignoreCase = true) ||
                                                    thirdElement.contains("vistas", ignoreCase = true) ||
                                                    thirdElement.contains("views", ignoreCase = true) ||
                                                    thirdElement.matches(Regex(".*\\d+.*[MKmk].*"))

                                            if (isViewsInfo) {
                                                dataElements[0].text // Usar el tipo como álbum ("Video", "Canción")
                                            } else {
                                                thirdElement // Es un álbum real
                                            }
                                        } else {
                                            dataElements[0].text // Solo hay 2 elementos, usar el tipo
                                        }
                                    }

                                    // ✅ CASO 2: Formato normal de canción
                                    // Estructura: [Artista, Álbum, ...]
                                    // Ejemplo: ["Guns N' Roses", "Use Your Illusion I", "1991"]
                                    dataElements.size >= 2 -> {
                                        artist = dataElements[0].text
                                        album = dataElements.getOrNull(1)?.text
                                    }

                                    // ✅ CASO 3: Solo un elemento
                                    dataElements.size == 1 -> {
                                        artist = dataElements[0].text
                                        album = null
                                    }
                                }
                            }

                            val thumbnail = renderer.thumbnail?.musicThumbnailRenderer
                                ?.thumbnail?.thumbnails?.lastOrNull()?.url ?: ""

                            val durationText = renderer.flexColumns.getOrNull(2)
                                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                                ?.firstOrNull()?.text ?: "0:00"

                            // Convertir duración "3:45" a milisegundos
                            val duration = parseDuration(durationText)

                            songs.add(
                                StreamingSong(
                                    id = videoId,
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    thumbnailUrl = thumbnail,
                                    duration = duration,
                                    provider = MusicProviderType.INNERTUBE,
                                )
                            )

                            if (songs.size >= limit) return@forEach
                        }
                    }
                    if (songs.size >= limit) return@forEach
                }

            Log.d(TAG, "✅ ${songs.size} canciones encontradas")
            songs.take(limit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream para: $songId")

            // Probar diferentes clientes en orden de preferencia
            val clients = listOf(
                YouTubeClient.ANDROID,
                YouTubeClient.WEB,
                YouTubeClient.WEB_REMIX,
                YouTubeClient.IOS
            )

            for (client in clients) {
                try {
                    Log.d(TAG, "Probando cliente: ${client.clientName}")

                    val response = innerTube.player(
                        client = client,
                        videoId = songId,
                        playlistId = null,
                        signatureTimestamp = null,
                        webPlayerPot = null
                    )

                    val playerResponse: PlayerResponse = response.body()

                    // Buscar el mejor formato de audio
                    val audioUrl = playerResponse.streamingData?.adaptiveFormats
                        ?.filter { it.mimeType?.startsWith("audio/") == true }
                        ?.maxByOrNull { it.bitrate ?: 0 }
                        ?.url

                    if (audioUrl != null) {
                        Log.d(TAG, "✅ URL obtenida con cliente: ${client.clientName}")
                        return@withContext audioUrl
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cliente ${client.clientName} falló: ${e.message}")
                }
            }

            Log.e(TAG, "❌ Ningún cliente funcionó para $songId")
            null

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream: ${e.message}", e)
            null
        }
    }

    override suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📈 Obteniendo trending...")

            // Buscar "trending music" para obtener canciones populares
            search("trending music", limit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en trending: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getRelated(songId: String, limit: Int): List<StreamingSong> {
        Log.w(TAG, "getRelated no implementado aún")
        return emptyList()
    }

    override suspend fun getPlaylist(playlistId: String, limit: Int): List<StreamingSong> {
        Log.w(TAG, "getPlaylist no implementado aún")
        return emptyList()
    }

    override fun getProviderType(): MusicProviderType {
        return MusicProviderType.INNERTUBE
    }

    /**
     * Convierte duración "3:45" a milisegundos
     */
    private fun parseDuration(durationText: String): Long {
        return try {
            val parts = durationText.split(":")
            when (parts.size) {
                2 -> { // mm:ss
                    val minutes = parts[0].toLongOrNull() ?: 0
                    val seconds = parts[1].toLongOrNull() ?: 0
                    (minutes * 60 + seconds) * 1000
                }
                3 -> { // hh:mm:ss
                    val hours = parts[0].toLongOrNull() ?: 0
                    val minutes = parts[1].toLongOrNull() ?: 0
                    val seconds = parts[2].toLongOrNull() ?: 0
                    (hours * 3600 + minutes * 60 + seconds) * 1000
                }
                else -> 0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parseando duración: $durationText")
            0L
        }
    }
}