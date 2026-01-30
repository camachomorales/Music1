// app/src/main/java/com/example/music/data/api/providers/YouTubeMusicProvider.kt
package com.example.music.data.api.providers

import android.util.Log
import com.example.music.data.api.MusicProvider
import com.example.music.data.api.MusicProviderType
import com.example.music.data.model.StreamingSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YouTubeMusicProvider : MusicProvider {

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.privacydev.net",
        "https://pipedapi.palveluntarjoaja.eu",
        "https://pipedapi.adminforge.de"
    )

    private var currentInstanceIndex = 0
    private val TAG = "YouTubeMusic"

    override fun getProviderType(): MusicProviderType = MusicProviderType.YOUTUBE_MUSIC

    private fun getCurrentInstance(): String = pipedInstances[currentInstanceIndex]

    private fun switchToNextInstance() {
        currentInstanceIndex = (currentInstanceIndex + 1) % pipedInstances.size
        Log.d(TAG, "🔄 Cambiando a instancia: ${getCurrentInstance()}")
    }

    override suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(pipedInstances.size) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "${getCurrentInstance()}/search?q=$encodedQuery&filter=music_songs"

                Log.d(TAG, "🔍 Buscando en: ${getCurrentInstance()}")

                val response = makeRequest(url)
                val json = JSONObject(response)
                val items = json.optJSONArray("items") ?: return@withContext emptyList()

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until minOf(items.length(), limit)) {
                    try {
                        val item = items.getJSONObject(i)

                        val videoId = item.optString("url", "").removePrefix("/watch?v=")
                        if (videoId.isEmpty()) continue

                        val title = item.optString("title", "Unknown")
                        val artist = item.optString("uploaderName", "Unknown Artist")
                        val duration = item.optLong("duration", 0) * 1000L
                        val thumbnail = item.optString("thumbnail", "")

                        songs.add(
                            StreamingSong(
                                id = videoId,
                                title = title,
                                artist = artist,
                                album = null,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                provider = MusicProviderType.YOUTUBE_MUSIC
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parseando item $i: ${e.message}")
                    }
                }

                Log.d(TAG, "✅ ${songs.size} canciones encontradas")
                return@withContext songs

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en ${getCurrentInstance()}: ${e.message}")
                lastException = e
                switchToNextInstance()
            }
        }

        Log.e(TAG, "❌ Todas las instancias fallaron")
        emptyList()
    }

    override suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(pipedInstances.size) {
            try {
                val url = "${getCurrentInstance()}/trending?region=US"

                Log.d(TAG, "🔍 Obteniendo trending de: ${getCurrentInstance()}")

                val response = makeRequest(url)
                val json = org.json.JSONArray(response)

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until minOf(json.length(), limit)) {
                    try {
                        val item = json.getJSONObject(i)

                        val type = item.optString("type", "")
                        if (type != "stream") continue

                        val videoId = item.optString("url", "").removePrefix("/watch?v=")
                        if (videoId.isEmpty()) continue

                        val title = item.optString("title", "Unknown")
                        val artist = item.optString("uploaderName", "Unknown Artist")
                        val duration = item.optLong("duration", 0) * 1000L
                        val thumbnail = item.optString("thumbnail", "")

                        songs.add(
                            StreamingSong(
                                id = videoId,
                                title = title,
                                artist = artist,
                                album = null,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                provider = MusicProviderType.YOUTUBE_MUSIC
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parseando item $i: ${e.message}")
                    }
                }

                Log.d(TAG, "✅ ${songs.size} trending songs")
                return@withContext songs

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en ${getCurrentInstance()}: ${e.message}")
                lastException = e
                switchToNextInstance()
            }
        }

        Log.e(TAG, "❌ Todas las instancias fallaron")
        emptyList()
    }

    override suspend fun getStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(pipedInstances.size) {
            try {
                val url = "${getCurrentInstance()}/streams/$songId"

                Log.d(TAG, "🎵 Obteniendo stream de: ${getCurrentInstance()}")

                val response = makeRequest(url)
                val json = JSONObject(response)

                val audioStreams = json.optJSONArray("audioStreams")

                if (audioStreams != null && audioStreams.length() > 0) {
                    var bestStream: String? = null
                    var bestBitrate = 0

                    for (i in 0 until audioStreams.length()) {
                        val stream = audioStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url", "")
                        val bitrate = stream.optInt("bitrate", 0)

                        if (streamUrl.isNotEmpty() && bitrate > bestBitrate) {
                            bestStream = streamUrl
                            bestBitrate = bitrate
                        }
                    }

                    if (bestStream != null) {
                        Log.d(TAG, "✅ Stream URL obtenida (${bestBitrate}kbps)")
                        return@withContext bestStream
                    }
                }

                Log.w(TAG, "⚠️ No se encontraron streams de audio")
                return@withContext null

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en ${getCurrentInstance()}: ${e.message}")
                lastException = e
                switchToNextInstance()
            }
        }

        Log.e(TAG, "❌ No se pudo obtener stream URL")
        null
    }

    override suspend fun getRelated(songId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(pipedInstances.size) {
            try {
                val url = "${getCurrentInstance()}/streams/$songId"

                Log.d(TAG, "🔍 Obteniendo relacionadas para: $songId")

                val response = makeRequest(url)
                val json = JSONObject(response)
                val relatedStreams = json.optJSONArray("relatedStreams") ?: return@withContext emptyList()

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until minOf(relatedStreams.length(), limit)) {
                    try {
                        val item = relatedStreams.getJSONObject(i)

                        val videoId = item.optString("url", "").removePrefix("/watch?v=")
                        if (videoId.isEmpty()) continue

                        val title = item.optString("title", "Unknown")
                        val artist = item.optString("uploaderName", "Unknown Artist")
                        val duration = item.optLong("duration", 0) * 1000L
                        val thumbnail = item.optString("thumbnail", "")

                        songs.add(
                            StreamingSong(
                                id = videoId,
                                title = title,
                                artist = artist,
                                album = null,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                provider = MusicProviderType.YOUTUBE_MUSIC
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parseando relacionada $i: ${e.message}")
                    }
                }

                Log.d(TAG, "✅ ${songs.size} relacionadas encontradas")
                return@withContext songs

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo relacionadas: ${e.message}")
                lastException = e
                switchToNextInstance()
            }
        }

        Log.e(TAG, "❌ No se pudieron obtener relacionadas")
        emptyList()
    }

    override suspend fun getPlaylist(playlistId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        repeat(pipedInstances.size) {
            try {
                val url = "${getCurrentInstance()}/playlists/$playlistId"

                Log.d(TAG, "🔍 Obteniendo playlist: $playlistId")

                val response = makeRequest(url)
                val json = JSONObject(response)
                val relatedStreams = json.optJSONArray("relatedStreams") ?: return@withContext emptyList()

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until minOf(relatedStreams.length(), limit)) {
                    try {
                        val item = relatedStreams.getJSONObject(i)

                        val videoId = item.optString("url", "").removePrefix("/watch?v=")
                        if (videoId.isEmpty()) continue

                        val title = item.optString("title", "Unknown")
                        val artist = item.optString("uploaderName", "Unknown Artist")
                        val duration = item.optLong("duration", 0) * 1000L
                        val thumbnail = item.optString("thumbnail", "")

                        songs.add(
                            StreamingSong(
                                id = videoId,
                                title = title,
                                artist = artist,
                                album = null,
                                duration = duration,
                                thumbnailUrl = thumbnail,
                                provider = MusicProviderType.YOUTUBE_MUSIC
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parseando canción de playlist $i: ${e.message}")
                    }
                }

                Log.d(TAG, "✅ ${songs.size} canciones de playlist")
                return@withContext songs

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error obteniendo playlist: ${e.message}")
                lastException = e
                switchToNextInstance()
            }
        }

        Log.e(TAG, "❌ No se pudo obtener playlist")
        emptyList()
    }

    private fun makeRequest(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("HTTP error: $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }
}