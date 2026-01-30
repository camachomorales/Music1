package com.example.music.data.api

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * ✅✅✅ VERSIÓN FINAL CON DETECCIÓN AUTOMÁTICA DE CLIENTE ✅✅✅
 * Detecta automáticamente si la URL es de cliente WEB o ANDROID
 * y aplica los headers correspondientes
 */
@UnstableApi
class OkHttpDataSource(
    private val client: OkHttpClient
) : BaseDataSource(true), HttpDataSource {

    private var response: Response? = null
    private var inputStream: InputStream? = null
    private var opened = false
    private var bytesToRead = 0L
    private var bytesRead = 0L

    companion object {
        private const val TAG = "OkHttpDataSource"

        // Headers para cliente ANDROID
        private const val ANDROID_USER_AGENT = "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip"
        private const val ANDROID_CLIENT_NAME = "3"
        private const val ANDROID_CLIENT_VERSION = "19.09.37"

        // Headers para cliente WEB
        private const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        // Headers comunes
        private const val ORIGIN = "https://music.youtube.com"
        private const val REFERER = "https://music.youtube.com/"
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun open(dataSpec: DataSpec): Long {
        val url = dataSpec.uri.toString()

        try {
            val requestBuilder = Request.Builder()
                .url(url)

            // ⭐⭐⭐ DETECCIÓN AUTOMÁTICA DE CLIENTE ⭐⭐⭐
            val isWebClient = detectWebClient(url)

            if (isWebClient) {
                Log.d(TAG, "🌐 Detectado cliente WEB - usando headers de navegador")
                applyWebHeaders(requestBuilder)
            } else {
                Log.d(TAG, "📱 Detectado cliente ANDROID - usando headers de app")
                applyAndroidHeaders(requestBuilder)
            }

            // ⭐⭐⭐ HEADERS COMUNES ⭐⭐⭐
            requestBuilder.header("Accept", "*/*")
            requestBuilder.header("Accept-Language", "en-US,en;q=0.9")
            requestBuilder.header("Origin", ORIGIN)
            requestBuilder.header("Referer", REFERER)

            // ⭐⭐⭐ RANGE HEADER - SIEMPRE OBLIGATORIO ⭐⭐⭐
            val rangeStart = dataSpec.position
            val rangeEnd = if (dataSpec.length != C.LENGTH_UNSET.toLong() && dataSpec.length != Long.MAX_VALUE) {
                rangeStart + dataSpec.length - 1
            } else {
                "" // Sin límite superior = descarga completa desde rangeStart
            }
            requestBuilder.header("Range", "bytes=$rangeStart-$rangeEnd")

            val request = requestBuilder.build()

            // Logging detallado
            Log.d(TAG, "========== REQUEST ==========")
            Log.d(TAG, "URL: ${url.take(150)}...")
            Log.d(TAG, "Cliente: ${if (isWebClient) "WEB" else "ANDROID"}")
            request.headers.names().forEach { name ->
                Log.d(TAG, "$name: ${request.header(name)}")
            }
            Log.d(TAG, "============================")

            // Ejecutar request
            response = client.newCall(request).execute()

            val responseCode = response!!.code

            Log.d(TAG, "✅ Response Code: $responseCode")

            // Verificar código de respuesta
            // 206 = Partial Content (esperado con Range)
            // 200 = OK (también válido)
            // 416 = Range Not Satisfiable (puede pasar al final del archivo)
            if (responseCode !in 200..299 && responseCode != 416) {
                val errorMessage = response!!.message
                Log.e(TAG, "❌ HTTP ERROR $responseCode: $errorMessage")

                // Log de respuesta completa para debug
                try {
                    val bodyText = response!!.body?.string()?.take(500)
                    Log.e(TAG, "Response body: $bodyText")
                } catch (e: Exception) {
                    Log.e(TAG, "No se pudo leer response body")
                }

                response!!.close()
                response = null

                throw HttpDataSource.HttpDataSourceException(
                    "HTTP $responseCode: $errorMessage",
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN,
                    responseCode
                )
            }

            // Obtener InputStream
            inputStream = response!!.body?.byteStream()

            if (inputStream == null) {
                response!!.close()
                throw HttpDataSource.HttpDataSourceException(
                    "Response body is null",
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN,
                    0
                )
            }

            opened = true
            bytesRead = 0L

            // Determinar tamaño del contenido
            bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                val contentLength = response!!.body?.contentLength() ?: C.LENGTH_UNSET.toLong()
                if (contentLength == -1L) C.LENGTH_UNSET.toLong() else contentLength
            }

            transferStarted(dataSpec)

            Log.d(TAG, "✅ Transfer started, bytes to read: $bytesToRead")

            return bytesToRead

        } catch (e: IOException) {
            Log.e(TAG, "❌ IOException: ${e.message}", e)
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
    }

    /**
     * Detecta si la URL proviene de un cliente WEB
     */
    private fun detectWebClient(url: String): Boolean {
        return url.contains("c=WEB", ignoreCase = true) ||
                url.contains("c=MWEB", ignoreCase = true) ||
                url.contains("c=WEB_REMIX", ignoreCase = true) ||
                url.contains("clientName=WEB", ignoreCase = true)
    }

    /**
     * Aplica headers específicos para cliente WEB
     */
    private fun applyWebHeaders(builder: Request.Builder) {
        builder.header("User-Agent", WEB_USER_AGENT)
        builder.header("Sec-Fetch-Dest", "empty")
        builder.header("Sec-Fetch-Mode", "cors")
        builder.header("Sec-Fetch-Site", "cross-site")
        builder.header("Accept-Encoding", "gzip, deflate, br")
    }

    /**
     * Aplica headers específicos para cliente ANDROID
     */
    private fun applyAndroidHeaders(builder: Request.Builder) {
        builder.header("User-Agent", ANDROID_USER_AGENT)
        builder.header("X-YouTube-Client-Name", ANDROID_CLIENT_NAME)
        builder.header("X-YouTube-Client-Version", ANDROID_CLIENT_VERSION)
    }

    @Throws(HttpDataSource.HttpDataSourceException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) {
            return 0
        }

        if (bytesToRead != C.LENGTH_UNSET.toLong()) {
            if (bytesRead >= bytesToRead) {
                return C.RESULT_END_OF_INPUT
            }
        }

        try {
            val readLength = if (bytesToRead != C.LENGTH_UNSET.toLong()) {
                minOf(length.toLong(), bytesToRead - bytesRead).toInt()
            } else {
                length
            }

            val read = inputStream!!.read(buffer, offset, readLength)

            if (read == -1) {
                return C.RESULT_END_OF_INPUT
            }

            bytesRead += read
            bytesTransferred(read)

            return read

        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                DataSpec(Uri.parse("unknown")),
                HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            // Ignorar
        } finally {
            inputStream = null

            try {
                response?.close()
            } catch (e: IOException) {
                // Ignorar
            } finally {
                response = null

                if (opened) {
                    opened = false
                    transferEnded()
                }
            }
        }
    }

    override fun getUri(): Uri? {
        return response?.request?.url?.let { Uri.parse(it.toString()) }
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        val headers = mutableMapOf<String, List<String>>()
        response?.headers?.let { responseHeaders ->
            for (name in responseHeaders.names()) {
                headers[name] = responseHeaders.values(name)
            }
        }
        return headers
    }

    override fun getResponseCode(): Int {
        return response?.code ?: -1
    }

    override fun setRequestProperty(name: String, value: String) {
        // No usado
    }

    override fun clearRequestProperty(name: String) {
        // No usado
    }

    override fun clearAllRequestProperties() {
        // No usado
    }
}

@UnstableApi
class OkHttpDataSourceFactory : DataSource.Factory {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val url = original.url.toString()

                // Detectar tipo de cliente
                val isWebClient = url.contains("c=WEB", ignoreCase = true) ||
                        url.contains("c=MWEB", ignoreCase = true) ||
                        url.contains("c=WEB_REMIX", ignoreCase = true)

                val request = original.newBuilder().apply {
                    // Solo agregar headers si no están presentes
                    if (original.header("User-Agent") == null) {
                        if (isWebClient) {
                            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        } else {
                            header("User-Agent", "com.google.android.youtube/19.09.37 (Linux; U; Android 11) gzip")
                        }
                    }
                    if (original.header("Accept") == null) {
                        header("Accept", "*/*")
                    }
                    if (original.header("Accept-Language") == null) {
                        header("Accept-Language", "en-US,en;q=0.9")
                    }
                }.build()

                chain.proceed(request)
            }
            .build()
    }

    override fun createDataSource(): DataSource {
        return OkHttpDataSource(client)
    }
}