// app/src/main/java/com/example/music/data/api/providers/SpotifyProvider.kt
package com.example.music.data.api.providers

import android.util.Base64
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

/**
 * Spotify Provider - Requiere credenciales de API
 *
 * Para obtener tus credenciales:
 * 1. Ve a https://developer.spotify.com/dashboard
 * 2. Crea una app
 * 3. Obtén tu Client ID y Client Secret
 * 4. Reemplaza los valores abajo
 */
class SpotifyProvider : MusicProvider {

    private val TAG = "Spotify"

    // ⚠️ REEMPLAZA ESTOS VALORES CON TUS CREDENCIALES
    private val CLIENT_ID = "TU_CLIENT_ID_AQUI"
    private val CLIENT_SECRET = "TU_CLIENT_SECRET_AQUI"

    private var accessToken: String? = null
    private var tokenExpiration: Long = 0

    override fun getProviderType(): MusicProviderType = MusicProviderType.JIOSAAVN

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiration) {
            return@withContext accessToken
        }

        try {
            val credentials = "$CLIENT_ID:$CLIENT_SECRET"
            val encodedCredentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val url = URL("https://accounts.spotify.com/api/token")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic $encodedCredentials")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
            }

            connection.outputStream.use {
                it.write("grant_type=client_credentials".toByteArray())
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            accessToken = json.getString("access_token")
            val expiresIn = json.getInt("expires_in")
            tokenExpiration = System.currentTimeMillis() + (expiresIn * 1000)

            Log.d(TAG, "✅ Token obtenido")

            accessToken

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo token: ${e.message}")
            null
        }
    }

    override suspend fun search(query: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        if (CLIENT_ID == "TU_CLIENT_ID_AQUI") {
            Log.w(TAG, "⚠️ Spotify no configurado")
            return@withContext emptyList()
        }

        try {
            val token = getAccessToken() ?: return@withContext emptyList()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=$limit"

            val response = makeAuthenticatedRequest(url, token)
            val json = JSONObject(response)

            val tracks = json.getJSONObject("tracks").getJSONArray("items")
            val songs = mutableListOf<StreamingSong>()

            for (i in 0 until tracks.length()) {
                try {
                    val track = tracks.getJSONObject(i)

                    val id = track.getString("id")
                    val name = track.getString("name")
                    val durationMs = track.getLong("duration_ms")

                    val artists = track.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) {
                        artists.getJSONObject(0).getString("name")
                    } else "Unknown Artist"

                    val album = track.getJSONObject("album")
                    val albumName = album.getString("name")

                    val images = album.getJSONArray("images")
                    val thumbnail = if (images.length() > 0) {
                        images.getJSONObject(0).getString("url")
                    } else null

                    songs.add(
                        StreamingSong(
                            id = id,
                            title = name,
                            artist = artistName,
                            album = albumName,
                            duration = durationMs,
                            thumbnailUrl = thumbnail,
                            provider = MusicProviderType.JIOSAAVN
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando track: ${e.message}")
                }
            }

            Log.d(TAG, "✅ ${songs.size} canciones encontradas")
            songs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en búsqueda: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTrending(limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        if (CLIENT_ID == "TU_CLIENT_ID_AQUI") {
            Log.w(TAG, "⚠️ Spotify no configurado")
            return@withContext emptyList()
        }

        try {
            val token = getAccessToken() ?: return@withContext emptyList()

            val playlistId = "37i9dQZEVXbMDoHDwVN2tF"
            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit"

            val response = makeAuthenticatedRequest(url, token)
            val json = JSONObject(response)

            val items = json.getJSONArray("items")
            val songs = mutableListOf<StreamingSong>()

            for (i in 0 until items.length()) {
                try {
                    val item = items.getJSONObject(i)
                    val track = item.getJSONObject("track")

                    val id = track.getString("id")
                    val name = track.getString("name")
                    val durationMs = track.getLong("duration_ms")

                    val artists = track.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) {
                        artists.getJSONObject(0).getString("name")
                    } else "Unknown Artist"

                    val album = track.getJSONObject("album")
                    val albumName = album.getString("name")

                    val images = album.getJSONArray("images")
                    val thumbnail = if (images.length() > 0) {
                        images.getJSONObject(0).getString("url")
                    } else null

                    songs.add(
                        StreamingSong(
                            id = id,
                            title = name,
                            artist = artistName,
                            album = albumName,
                            duration = durationMs,
                            thumbnailUrl = thumbnail,
                            provider = MusicProviderType.JIOSAAVN
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando item: ${e.message}")
                }
            }

            Log.d(TAG, "✅ ${songs.size} trending songs")
            songs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo trending: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext null

            val url = "https://api.spotify.com/v1/tracks/$songId"
            val response = makeAuthenticatedRequest(url, token)
            val json = JSONObject(response)

            val previewUrl = json.optString("preview_url", null)

            if (previewUrl != null && previewUrl.isNotEmpty()) {
                Log.d(TAG, "✅ Preview URL obtenida (30s)")
                previewUrl
            } else {
                Log.w(TAG, "⚠️ No hay preview disponible")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL: ${e.message}")
            null
        }
    }

    override suspend fun getRelated(songId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext emptyList()

            val url = "https://api.spotify.com/v1/recommendations?seed_tracks=$songId&limit=$limit"
            val response = makeAuthenticatedRequest(url, token)
            val json = JSONObject(response)

            val tracks = json.getJSONArray("tracks")
            val songs = mutableListOf<StreamingSong>()

            for (i in 0 until tracks.length()) {
                try {
                    val track = tracks.getJSONObject(i)

                    val id = track.getString("id")
                    val name = track.getString("name")
                    val durationMs = track.getLong("duration_ms")

                    val artists = track.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) {
                        artists.getJSONObject(0).getString("name")
                    } else "Unknown Artist"

                    val album = track.getJSONObject("album")
                    val albumName = album.getString("name")

                    val images = album.getJSONArray("images")
                    val thumbnail = if (images.length() > 0) {
                        images.getJSONObject(0).getString("url")
                    } else null

                    songs.add(
                        StreamingSong(
                            id = id,
                            title = name,
                            artist = artistName,
                            album = albumName,
                            duration = durationMs,
                            thumbnailUrl = thumbnail,
                            provider = MusicProviderType.JIOSAAVN
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando relacionada: ${e.message}")
                }
            }

            Log.d(TAG, "✅ ${songs.size} relacionadas")
            songs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo relacionadas: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getPlaylist(playlistId: String, limit: Int): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken() ?: return@withContext emptyList()

            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit"
            val response = makeAuthenticatedRequest(url, token)
            val json = JSONObject(response)

            val items = json.getJSONArray("items")
            val songs = mutableListOf<StreamingSong>()

            for (i in 0 until items.length()) {
                try {
                    val item = items.getJSONObject(i)
                    val track = item.getJSONObject("track")

                    val id = track.getString("id")
                    val name = track.getString("name")
                    val durationMs = track.getLong("duration_ms")

                    val artists = track.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) {
                        artists.getJSONObject(0).getString("name")
                    } else "Unknown Artist"

                    val album = track.getJSONObject("album")
                    val albumName = album.getString("name")

                    val images = album.getJSONArray("images")
                    val thumbnail = if (images.length() > 0) {
                        images.getJSONObject(0).getString("url")
                    } else null

                    songs.add(
                        StreamingSong(
                            id = id,
                            title = name,
                            artist = artistName,
                            album = albumName,
                            duration = durationMs,
                            thumbnailUrl = thumbnail,
                            provider = MusicProviderType.JIOSAAVN
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando canción: ${e.message}")
                }
            }

            Log.d(TAG, "✅ ${songs.size} canciones de playlist")
            songs

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo playlist: ${e.message}")
            emptyList()
        }
    }

    private fun makeAuthenticatedRequest(urlString: String, token: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
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