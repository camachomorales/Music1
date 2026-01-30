// app/src/main/java/com/example/music/data/api/providers/JioSaavnProvider.kt
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

class JioSaavnProvider : MusicProvider {

    private val TAG = "JioSaavnProvider"
    private val baseUrl = "https://meloapi.vercel.app/api"

    override suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando: $query")

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "$baseUrl/search/songs?query=$encodedQuery&limit=$limit"

            Log.d(TAG, "🌐 URL: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MusicApp/1.0")
            }

            connection.connect()
            val responseCode = connection.responseCode
            Log.d(TAG, "📊 Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "📦 Respuesta recibida (${response.length} chars)")

                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val results = data.getJSONArray("results")

                Log.d(TAG, "✅ ${results.length()} resultados encontrados")

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until results.length()) {
                    try {
                        val item = results.getJSONObject(i)

                        val id = item.getString("id")
                        val name = item.getString("name")

                        // Extraer artista
                        val artist = if (item.has("artists")) {
                            val artistsObj = item.getJSONObject("artists")
                            if (artistsObj.has("primary")) {
                                val primaryArray = artistsObj.getJSONArray("primary")
                                if (primaryArray.length() > 0) {
                                    val firstArtist = primaryArray.getJSONObject(0)
                                    firstArtist.optString("name", "Unknown Artist")
                                } else {
                                    "Unknown Artist"
                                }
                            } else {
                                "Unknown Artist"
                            }
                        } else {
                            item.optString("primaryArtists", "Unknown Artist")
                        }

                        // Album
                        val album = if (item.has("album")) {
                            item.getJSONObject("album").optString("name", null)
                        } else {
                            null
                        }

                        // Duración en milisegundos
                        val duration = item.optLong("duration", 0) * 1000

                        // Thumbnail - buscar la imagen de mayor calidad
                        val thumbnailUrl = if (item.has("image")) {
                            val imageArray = item.getJSONArray("image")
                            if (imageArray.length() > 0) {
                                // Obtener la última (mayor calidad)
                                val lastImage = imageArray.getJSONObject(imageArray.length() - 1)
                                lastImage.getString("url")
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                        // URL externa
                        val externalUrl = item.optString("url", "")

                        songs.add(
                            StreamingSong(
                                id = "js:$id",
                                title = name,
                                artist = artist,
                                album = album,
                                duration = duration,
                                thumbnailUrl = thumbnailUrl,
                                provider = MusicProviderType.JIOSAAVN,
                                externalUrl = externalUrl
                            )
                        )

                        if (i < 3) {
                            Log.d(TAG, "🎵 [$i] $name - $artist")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando item $i: ${e.message}")
                    }
                }

                connection.disconnect()
                return@withContext songs

            } else {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Sin detalles"
                Log.e(TAG, "❌ Error HTTP $responseCode: $errorResponse")
                connection.disconnect()
                return@withContext emptyList()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en search: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📈 Obteniendo trending...")

            // MeloAPI no tiene endpoint de trending directo, usar búsqueda popular
            val popularQueries = listOf("hindi songs", "bollywood", "latest songs")
            val randomQuery = popularQueries.random()

            Log.d(TAG, "🎲 Usando query: $randomQuery")

            return@withContext search(randomQuery, limit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en getTrending: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getStreamUrl(songId: String): String = withContext(Dispatchers.IO) {
        try {
            val cleanId = songId.removePrefix("js:")
            Log.d(TAG, "🎵 Obteniendo URL de stream para: $cleanId")

            // ✅ CORRECCIÓN: La API requiere "ids" (plural) como array
            val urlString = "$baseUrl/songs?ids=$cleanId"
            Log.d(TAG, "🌐 URL: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }

            connection.connect()
            val responseCode = connection.responseCode
            Log.d(TAG, "📊 Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "📦 Respuesta recibida (${response.length} chars)")
                Log.d(TAG, "📄 JSON: ${response.take(500)}")

                val json = JSONObject(response)

                // Verificar si hay data
                if (!json.has("data")) {
                    Log.e(TAG, "❌ No existe campo 'data' en JSON")
                    Log.e(TAG, "📋 Keys disponibles: ${json.keys().asSequence().toList()}")
                    connection.disconnect()
                    throw Exception("JSON no contiene 'data'")
                }

                val data = json.getJSONArray("data")
                Log.d(TAG, "📊 Items en data: ${data.length()}")

                if (data.length() > 0) {
                    val song = data.getJSONObject(0)
                    Log.d(TAG, "🔑 Keys del song: ${song.keys().asSequence().toList()}")

                    // Verificar si tiene downloadUrl
                    if (!song.has("downloadUrl")) {
                        Log.e(TAG, "❌ Canción NO tiene 'downloadUrl'")
                        Log.e(TAG, "📋 Campos disponibles: ${song.keys().asSequence().toList()}")

                        // Intentar buscar otras opciones de URL
                        if (song.has("url")) {
                            val directUrl = song.getString("url")
                            Log.d(TAG, "⚠️ Usando 'url' directo: $directUrl")
                            connection.disconnect()
                            return@withContext directUrl
                        }

                        connection.disconnect()
                        throw Exception("Canción no tiene downloadUrl ni url alternativo")
                    }

                    val downloadUrlArray = song.getJSONArray("downloadUrl")
                    Log.d(TAG, "📊 Calidades disponibles: ${downloadUrlArray.length()}")

                    if (downloadUrlArray.length() > 0) {
                        // Mostrar todas las calidades disponibles
                        for (i in 0 until downloadUrlArray.length()) {
                            val quality = downloadUrlArray.getJSONObject(i)
                            Log.d(TAG, "  [$i] Quality: ${quality.optString("quality", "unknown")}")
                        }

                        // Obtener la URL de mayor calidad (última del array)
                        val bestQuality = downloadUrlArray.getJSONObject(downloadUrlArray.length() - 1)
                        val streamUrl = bestQuality.getString("url")

                        Log.d(TAG, "✅ Stream URL obtenida: ${streamUrl.take(100)}...")
                        connection.disconnect()
                        return@withContext streamUrl
                    } else {
                        Log.e(TAG, "❌ downloadUrl array está vacío")
                        connection.disconnect()
                        throw Exception("downloadUrl array vacío")
                    }
                } else {
                    Log.e(TAG, "❌ data array está vacío")
                    connection.disconnect()
                    throw Exception("No se encontró la canción")
                }
            } else {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Sin detalles"
                Log.e(TAG, "❌ Error HTTP $responseCode: ${errorResponse.take(200)}")
                connection.disconnect()
                throw Exception("HTTP error: $responseCode")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL: ${e.message}", e)
            throw Exception("Failed to get stream URL: ${e.message}")
        }
    }

    override suspend fun getRelated(songId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val cleanId = songId.removePrefix("js:")
            Log.d(TAG, "🔗 Obteniendo canciones relacionadas para: $cleanId")

            val urlString = "$baseUrl/songs/$cleanId/suggestions?limit=$limit"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.getJSONArray("data")

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until data.length()) {
                    try {
                        val item = data.getJSONObject(i)

                        songs.add(
                            StreamingSong(
                                id = "js:${item.getString("id")}",
                                title = item.getString("name"),
                                artist = getArtistName(item),
                                album = item.optJSONObject("album")?.optString("name"),
                                duration = item.optLong("duration", 0) * 1000,
                                thumbnailUrl = getBestImage(item),
                                provider = MusicProviderType.JIOSAAVN,
                                externalUrl = item.optString("url", "")
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando related song $i: ${e.message}")
                    }
                }

                connection.disconnect()
                return@withContext songs
            }

            connection.disconnect()
            return@withContext emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en getRelated: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override suspend fun getPlaylist(playlistId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val cleanId = playlistId.removePrefix("js:")
            Log.d(TAG, "📝 Obteniendo playlist: $cleanId")

            val urlString = "$baseUrl/playlists?id=$cleanId&limit=$limit"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }

            connection.connect()
            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val data = json.getJSONObject("data")
                val songsArray = data.getJSONArray("songs")

                val songs = mutableListOf<StreamingSong>()

                for (i in 0 until songsArray.length()) {
                    try {
                        val item = songsArray.getJSONObject(i)

                        songs.add(
                            StreamingSong(
                                id = "js:${item.getString("id")}",
                                title = item.getString("name"),
                                artist = getArtistName(item),
                                album = item.optJSONObject("album")?.optString("name"),
                                duration = item.optLong("duration", 0) * 1000,
                                thumbnailUrl = getBestImage(item),
                                provider = MusicProviderType.JIOSAAVN,
                                externalUrl = item.optString("url", "")
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando playlist song $i: ${e.message}")
                    }
                }

                connection.disconnect()
                return@withContext songs
            }

            connection.disconnect()
            return@withContext emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en getPlaylist: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    override fun getProviderType(): MusicProviderType {
        return MusicProviderType.JIOSAAVN
    }

    // Helpers
    private fun getArtistName(item: JSONObject): String {
        return try {
            if (item.has("artists")) {
                val artistsObj = item.getJSONObject("artists")
                if (artistsObj.has("primary")) {
                    val primaryArray = artistsObj.getJSONArray("primary")
                    if (primaryArray.length() > 0) {
                        primaryArray.getJSONObject(0).getString("name")
                    } else {
                        "Unknown Artist"
                    }
                } else {
                    "Unknown Artist"
                }
            } else {
                item.optString("primaryArtists", "Unknown Artist")
            }
        } catch (e: Exception) {
            "Unknown Artist"
        }
    }

    private fun getBestImage(item: JSONObject): String? {
        return try {
            if (item.has("image")) {
                val imageArray = item.getJSONArray("image")
                if (imageArray.length() > 0) {
                    val lastImage = imageArray.getJSONObject(imageArray.length() - 1)
                    lastImage.getString("url")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}