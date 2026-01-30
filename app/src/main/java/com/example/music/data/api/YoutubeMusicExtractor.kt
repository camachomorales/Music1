package com.example.music.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * ✅✅✅ VERSIÓN CORREGIDA - Cliente WEB_REMIX ✅✅✅
 * Extractor robusto para YouTube Music basado en InnerTube API
 * Basado en InnerTune y OuterTune
 *
 * CAMBIO PRINCIPAL: Usa WEB_REMIX en lugar de ANDROID para evitar TLS fingerprinting
 */
class YoutubeMusicExtractor {

    companion object {
        private const val TAG = "YTMusicExtractor"

        // InnerTube API endpoints
        private const val INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private const val INNERTUBE_CLIENT_VERSION = "1.20231122.01.00"  // Versión actualizada
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"

        // User-Agent para navegador (WEB_REMIX)
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        // Cliente info
        private const val CLIENT_NAME = "WEB_REMIX"
        private const val CLIENT_NAME_ID = "67"  // ID numérico para WEB_REMIX
    }

    /**
     * Obtener URL de stream para un video
     * Retorna URL con tiempo de expiración extendido
     */
    suspend fun getStreamUrl(videoId: String): StreamResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Obteniendo stream URL para: $videoId")
            Log.d(TAG, "🌐 Usando cliente: $CLIENT_NAME")

            // 1. Obtener player response
            val playerResponse = getPlayerResponse(videoId) ?: run {
                Log.e(TAG, "❌ No se pudo obtener player response")
                return@withContext null
            }

            // 2. Verificar si hay error de playabilidad
            val playabilityStatus = playerResponse.optJSONObject("playabilityStatus")
            val status = playabilityStatus?.optString("status")
            if (status != "OK") {
                val reason = playabilityStatus?.optString("reason", "Unknown error")
                Log.e(TAG, "❌ Playability error: $status - $reason")
                return@withContext null
            }

            // 3. Extraer streaming data
            val streamingData = playerResponse.optJSONObject("streamingData")
            if (streamingData == null) {
                Log.e(TAG, "❌ No streaming data en response")
                return@withContext null
            }

            // 4. Obtener formatos adaptivos (mejor calidad de audio)
            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats == null || adaptiveFormats.length() == 0) {
                Log.e(TAG, "❌ No hay formatos adaptativos")
                return@withContext null
            }

            // 5. Buscar mejor formato de audio (preferir opus/webm)
            var bestFormat: JSONObject? = null
            var bestBitrate = 0

            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val mimeType = format.optString("mimeType", "")

                // Solo audio (no video)
                if (!mimeType.startsWith("audio/")) continue

                val bitrate = format.optInt("bitrate", 0)
                val url = format.optString("url", "")

                // Preferir formatos con URL directa y mayor bitrate
                if (url.isNotEmpty() && bitrate > bestBitrate) {
                    bestFormat = format
                    bestBitrate = bitrate
                    Log.d(TAG, "📊 Formato encontrado: $mimeType, bitrate: $bitrate")
                }
            }

            if (bestFormat == null) {
                Log.e(TAG, "❌ No se encontró formato de audio válido")
                return@withContext null
            }

            val streamUrl = bestFormat.optString("url", "")
            if (streamUrl.isEmpty()) {
                Log.e(TAG, "❌ URL de stream vacía")
                return@withContext null
            }

            // 6. Extraer información adicional
            val videoDetails = playerResponse.optJSONObject("videoDetails")
            val duration = videoDetails?.optString("lengthSeconds", "0")?.toLongOrNull()?.times(1000) ?: 0L
            val title = videoDetails?.optString("title", "Unknown")

            Log.d(TAG, "✅ Stream URL obtenida exitosamente")
            Log.d(TAG, "🎵 Título: $title")
            Log.d(TAG, "📊 Bitrate: $bestBitrate bps, Duration: ${duration}ms")
            Log.d(TAG, "🔗 URL: ${streamUrl.take(150)}...")
            Log.d(TAG, "⏰ URL contiene expire=${extractExpireTime(streamUrl)}")

            StreamResult(
                url = streamUrl,
                duration = duration,
                bitrate = bestBitrate,
                mimeType = bestFormat.optString("mimeType", "audio/webm"),
                title = title ?: "Unknown"
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo stream URL", e)
            null
        }
    }

    /**
     * Obtener player response desde InnerTube API
     * IMPORTANTE: Usa cliente WEB_REMIX para evitar TLS fingerprinting
     */
    private fun getPlayerResponse(videoId: String): JSONObject? {
        return try {
            val url = URL("$BASE_URL/player?key=$INNERTUBE_API_KEY&prettyPrint=false")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true

                // ⭐⭐⭐ HEADERS CRÍTICOS PARA WEB_REMIX ⭐⭐⭐
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept-Encoding", "gzip, deflate, br")
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("X-Goog-Api-Format-Version", "1")
                setRequestProperty("X-YouTube-Client-Name", CLIENT_NAME_ID)
                setRequestProperty("X-YouTube-Client-Version", INNERTUBE_CLIENT_VERSION)

                // Headers adicionales de navegador
                setRequestProperty("Sec-Fetch-Dest", "empty")
                setRequestProperty("Sec-Fetch-Mode", "cors")
                setRequestProperty("Sec-Fetch-Site", "same-origin")

                connectTimeout = 15000
                readTimeout = 15000
            }

            // Body del request con cliente WEB_REMIX
            val requestBody = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", CLIENT_NAME)
                        put("clientVersion", INNERTUBE_CLIENT_VERSION)
                        put("hl", "en")
                        put("gl", "US")
                        put("utcOffsetMinutes", 0)
                        put("userAgent", USER_AGENT)
                        put("browserName", "Chrome")
                        put("browserVersion", "131.0.0.0")
                        put("osName", "Windows")
                        put("osVersion", "10.0")
                        put("platform", "DESKTOP")
                        put("clientFormFactor", "UNKNOWN_FORM_FACTOR")
                    })
                    put("user", JSONObject().apply {
                        put("lockedSafetyMode", false)
                    })
                    put("request", JSONObject().apply {
                        put("useSsl", true)
                        put("internalExperimentFlags", JSONObject())
                    })
                })
                put("playbackContext", JSONObject().apply {
                    put("contentPlaybackContext", JSONObject().apply {
                        put("html5Preference", "HTML5_PREF_WANTS")
                        put("referer", "https://music.youtube.com/")
                        put("signatureTimestamp", (System.currentTimeMillis() / 1000) - 3600) // -1 hora
                    })
                })
                put("racyCheckOk", false)
                put("contentCheckOk", false)
            }

            Log.d(TAG, "📤 Enviando request a InnerTube API")
            Log.d(TAG, "🔑 Cliente: $CLIENT_NAME (ID: $CLIENT_NAME_ID)")

            // Enviar request
            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            // Leer response
            val responseCode = connection.responseCode
            Log.d(TAG, "📥 Response code: $responseCode")

            if (responseCode != 200) {
                val errorStream = connection.errorStream
                val errorText = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                Log.e(TAG, "❌ HTTP Error $responseCode: $errorText")
                connection.disconnect()
                return null
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            Log.d(TAG, "✅ Player response obtenida (${response.length} bytes)")

            JSONObject(response)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en getPlayerResponse", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Buscar canciones
     */
    suspend fun search(query: String, limit: Int = 20): List<YTMusicTrack> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando: $query (límite: $limit)")

            val url = URL("$BASE_URL/search?key=$INNERTUBE_API_KEY&prettyPrint=false")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true

                // Headers para búsqueda
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Origin", "https://music.youtube.com")
                setRequestProperty("Referer", "https://music.youtube.com/")
                setRequestProperty("X-YouTube-Client-Name", CLIENT_NAME_ID)
                setRequestProperty("X-YouTube-Client-Version", INNERTUBE_CLIENT_VERSION)

                connectTimeout = 15000
                readTimeout = 15000
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", CLIENT_NAME)
                        put("clientVersion", INNERTUBE_CLIENT_VERSION)
                        put("hl", "en")
                        put("gl", "US")
                        put("userAgent", USER_AGENT)
                    })
                })
                put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV") // Filtro para canciones
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "❌ Search HTTP Error: $responseCode")
                connection.disconnect()
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val results = parseSearchResults(JSONObject(response), limit)
            Log.d(TAG, "✅ ${results.size} resultados encontrados")

            results

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en search", e)
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseSearchResults(response: JSONObject, limit: Int): List<YTMusicTrack> {
        val tracks = mutableListOf<YTMusicTrack>()

        try {
            val contents = response
                .optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.getJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
                ?: return tracks

            for (i in 0 until contents.length()) {
                val section = contents.getJSONObject(i)
                val musicShelfRenderer = section.optJSONObject("musicShelfRenderer") ?: continue
                val contentsList = musicShelfRenderer.optJSONArray("contents") ?: continue

                for (j in 0 until contentsList.length()) {
                    if (tracks.size >= limit) break

                    val item = contentsList.getJSONObject(j)
                    val musicResponsiveListItemRenderer =
                        item.optJSONObject("musicResponsiveListItemRenderer") ?: continue

                    // Extraer datos
                    val flexColumns = musicResponsiveListItemRenderer.optJSONArray("flexColumns") ?: continue

                    // Video ID
                    val videoId = musicResponsiveListItemRenderer
                        .optJSONObject("playlistItemData")
                        ?.optString("videoId") ?: continue

                    // Título
                    val title = flexColumns.getJSONObject(0)
                        .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.optJSONObject("text")
                        ?.optJSONArray("runs")
                        ?.getJSONObject(0)
                        ?.optString("text") ?: "Unknown"

                    // Artista
                    val artist = if (flexColumns.length() > 1) {
                        flexColumns.getJSONObject(1)
                            .optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.optJSONObject("text")
                            ?.optJSONArray("runs")
                            ?.getJSONObject(0)
                            ?.optString("text") ?: "Unknown Artist"
                    } else "Unknown Artist"

                    // Thumbnail
                    val thumbnail = musicResponsiveListItemRenderer
                        .optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        ?.getJSONObject(0)
                        ?.optString("url")

                    tracks.add(YTMusicTrack(
                        videoId = videoId,
                        title = title,
                        artist = artist,
                        thumbnailUrl = thumbnail
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parseando resultados", e)
        }

        return tracks
    }

    /**
     * Extraer tiempo de expiración de la URL para debugging
     */
    private fun extractExpireTime(url: String): String {
        return try {
            val expireMatch = Regex("expire=(\\d+)").find(url)
            if (expireMatch != null) {
                val expireTimestamp = expireMatch.groupValues[1].toLong()
                val currentTimestamp = System.currentTimeMillis() / 1000
                val hoursUntilExpire = (expireTimestamp - currentTimestamp) / 3600
                "$expireTimestamp (~${hoursUntilExpire}h restantes)"
            } else {
                "No encontrado"
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    data class StreamResult(
        val url: String,
        val duration: Long,
        val bitrate: Int,
        val mimeType: String,
        val title: String
    )

    data class YTMusicTrack(
        val videoId: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?
    )
}