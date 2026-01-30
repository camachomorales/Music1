// app/src/main/java/com/example/music/data/api/providers/PipedProvider.kt
package com.example.music.data.api.providers

import android.util.Log
import com.example.music.data.api.MusicProvider
import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PipedProvider : MusicProvider {

    private val TAG = "PipedProvider"

    // Lista de instancias de Piped (ordenadas por confiabilidad)
    private val instances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.moomoo.me",
        "https://piped-api.garudalinux.org",
        "https://pipedapi.rivo.lol",
        "https://pipedapi.aeong.one",
        "https://piped-api.lunar.icu"
    )

    private var currentInstanceIndex = 0

    override fun getProviderType(): MusicProviderType = MusicProviderType.YOUTUBE_MUSIC

    private fun getNextInstance(): String {
        val instance = instances[currentInstanceIndex]
        currentInstanceIndex = (currentInstanceIndex + 1) % instances.size
        return instance
    }

    override suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<StreamingSong>()
        var lastException: Exception? = null

        // Intentar con todas las instancias hasta que una funcione
        repeat(instances.size) {
            try {
                val instance = getNextInstance()
                Log.d(TAG, "🔍 Buscando en: $instance")

                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = URL("$instance/search?q=$encodedQuery&filter=music_songs")

                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val jsonResponse = JSONObject(response)
                    val items = jsonResponse.optJSONArray("items") ?: JSONArray()

                    for (i in 0 until minOf(items.length(), limit)) {
                        try {
                            val item = items.getJSONObject(i)

                            val videoId = item.optString("url")?.removePrefix("/watch?v=") ?: continue
                            val title = item.optString("title") ?: continue
                            val artistName = item.optString("uploaderName") ?: "Unknown Artist"
                            val thumbnailUrl = item.optString("thumbnail")
                            val duration = item.optLong("duration", 0)

                            songs.add(
                                StreamingSong(
                                    id = "yt:$videoId",
                                    title = title,
                                    artist = artistName,
                                    album = null,
                                    thumbnailUrl = thumbnailUrl,
                                    duration = duration,
                                    provider = MusicProviderType.YOUTUBE_MUSIC
                                )
                            )
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parseando item: ${e.message}")
                        }
                    }

                    if (songs.isNotEmpty()) {
                        Log.d(TAG, "✅ ${songs.size} canciones encontradas desde $instance")
                        return@withContext songs
                    }
                } else {
                    Log.w(TAG, "⚠️ $instance respondió con código: ${connection.responseCode}")
                    connection.disconnect()
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "❌ Error con instancia: ${e.message}")
            }
        }

        // Si ninguna instancia funcionó
        Log.e(TAG, "❌ Todas las instancias fallaron. Último error: ${lastException?.message}")
        emptyList()
    }

    override suspend fun getStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val videoId = songId.removePrefix("yt:")
            Log.d(TAG, "🎵 Obteniendo stream URL para: $videoId")

            // Intentar con múltiples instancias
            repeat(instances.size) {
                try {
                    val instance = getNextInstance()
                    val url = URL("$instance/streams/$videoId")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()

                        val jsonResponse = JSONObject(response)
                        val audioStreams = jsonResponse.optJSONArray("audioStreams")

                        if (audioStreams != null && audioStreams.length() > 0) {
                            // Buscar el mejor formato (preferir Opus)
                            var bestStream: JSONObject? = null
                            var bestBitrate = 0

                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                val mimeType = stream.optString("mimeType", "")
                                val bitrate = stream.optInt("bitrate", 0)

                                // Preferir Opus > M4A
                                if (mimeType.contains("opus", ignoreCase = true) || mimeType.contains("webm", ignoreCase = true)) {
                                    if (bitrate > bestBitrate) {
                                        bestStream = stream
                                        bestBitrate = bitrate
                                    }
                                } else if (bestStream == null && mimeType.contains("mp4", ignoreCase = true)) {
                                    bestStream = stream
                                    bestBitrate = bitrate
                                }
                            }

                            val streamUrl = bestStream?.optString("url")
                            if (!streamUrl.isNullOrBlank()) {
                                Log.d(TAG, "✅ Stream URL obtenida desde $instance (${bestBitrate / 1000}kbps)")
                                return@withContext streamUrl
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Error con instancia: ${e.message}")
                }
            }

            Log.e(TAG, "❌ No se pudo obtener stream URL de ninguna instancia")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL: ${e.message}", e)
            null
        }
    }

    override suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        // Piped no tiene trending directo, usar búsqueda popular
        search("trending music", limit)
    }

    override suspend fun getRelated(songId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val videoId = songId.removePrefix("yt:")
            Log.d(TAG, "🔗 Obteniendo relacionadas para: $videoId")

            repeat(instances.size) {
                try {
                    val instance = getNextInstance()
                    val url = URL("$instance/streams/$videoId")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()

                        val jsonResponse = JSONObject(response)
                        val relatedStreams = jsonResponse.optJSONArray("relatedStreams")

                        if (relatedStreams != null) {
                            val songs = mutableListOf<StreamingSong>()

                            for (i in 0 until minOf(relatedStreams.length(), limit)) {
                                try {
                                    val item = relatedStreams.getJSONObject(i)
                                    val relatedVideoId = item.optString("url")?.removePrefix("/watch?v=") ?: continue
                                    val title = item.optString("title") ?: continue
                                    val uploaderName = item.optString("uploaderName") ?: "Unknown Artist"
                                    val thumbnailUrl = item.optString("thumbnail")
                                    val duration = item.optLong("duration", 0)

                                    songs.add(
                                        StreamingSong(
                                            id = "yt:$relatedVideoId",
                                            title = title,
                                            artist = uploaderName,
                                            album = null,
                                            thumbnailUrl = thumbnailUrl,
                                            duration = duration,
                                            provider = MusicProviderType.YOUTUBE_MUSIC
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error parseando item relacionado: ${e.message}")
                                }
                            }

                            if (songs.isNotEmpty()) {
                                Log.d(TAG, "✅ ${songs.size} canciones relacionadas encontradas")
                                return@withContext songs
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Error obteniendo relacionadas: ${e.message}")
                }
            }

            Log.e(TAG, "❌ No se pudieron obtener canciones relacionadas")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo relacionadas: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getPlaylist(playlistId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val cleanPlaylistId = playlistId.removePrefix("yt:").removePrefix("playlist:")
            Log.d(TAG, "📋 Obteniendo playlist: $cleanPlaylistId")

            repeat(instances.size) {
                try {
                    val instance = getNextInstance()
                    val url = URL("$instance/playlists/$cleanPlaylistId")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }

                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()

                        val jsonResponse = JSONObject(response)
                        val relatedStreams = jsonResponse.optJSONArray("relatedStreams")

                        if (relatedStreams != null) {
                            val songs = mutableListOf<StreamingSong>()

                            for (i in 0 until minOf(relatedStreams.length(), limit)) {
                                try {
                                    val item = relatedStreams.getJSONObject(i)
                                    val videoId = item.optString("url")?.removePrefix("/watch?v=") ?: continue
                                    val title = item.optString("title") ?: continue
                                    val uploaderName = item.optString("uploaderName") ?: "Unknown Artist"
                                    val thumbnailUrl = item.optString("thumbnail")
                                    val duration = item.optLong("duration", 0)

                                    songs.add(
                                        StreamingSong(
                                            id = "yt:$videoId",
                                            title = title,
                                            artist = uploaderName,
                                            album = null,
                                            thumbnailUrl = thumbnailUrl,
                                            duration = duration,
                                            provider = MusicProviderType.YOUTUBE_MUSIC
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error parseando item de playlist: ${e.message}")
                                }
                            }

                            if (songs.isNotEmpty()) {
                                Log.d(TAG, "✅ ${songs.size} canciones de playlist obtenidas")
                                return@withContext songs
                            }
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.w(TAG, "Error obteniendo playlist: ${e.message}")
                }
            }

            Log.e(TAG, "❌ No se pudo obtener playlist")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo playlist: ${e.message}", e)
            emptyList()
        }
    }
}