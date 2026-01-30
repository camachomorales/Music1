package com.example.music.data.api

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class CustomHttpDataSourceFactory : HttpDataSource.Factory {

    override fun createDataSource(): HttpDataSource {
        return CustomHttpDataSource()
    }

    override fun setDefaultRequestProperties(defaultRequestProperties: Map<String, String>): HttpDataSource.Factory {
        return this
    }
}

class CustomHttpDataSource : HttpDataSource {

    companion object {
        private const val TAG = "CustomHttpDataSource"
        // ✅ Cabeceras críticas para YouTube
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val REFERER = "https://www.youtube.com/"
        private const val ORIGIN = "https://www.youtube.com"
    }

    private var connection: HttpURLConnection? = null
    private var inputStream: InputStream? = null
    private var opened = false
    private var bytesToRead: Long = C.LENGTH_UNSET.toLong()
    private var bytesRead: Long = 0
    private var dataSpec: DataSpec? = null

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec

        try {
            val url = URL(dataSpec.uri.toString())
            connection = url.openConnection() as HttpURLConnection

            connection?.apply {
                requestMethod = "GET"

                // ✅ Cabeceras críticas ANTES de connect()
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Origin", ORIGIN)
                setRequestProperty("Referer", REFERER)
                setRequestProperty("Connection", "keep-alive")

                // ✅ Logs para debugging
                Log.d(TAG, "📤 Request a: ${dataSpec.uri.toString().take(80)}...")
                Log.d(TAG, "   User-Agent: $USER_AGENT")
                Log.d(TAG, "   Origin: $ORIGIN")
                Log.d(TAG, "   Referer: $REFERER")

                // ✅ Range request para streaming
                if (dataSpec.position > 0) {
                    val rangeEnd = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                        dataSpec.position + dataSpec.length - 1
                    } else {
                        ""
                    }
                    val rangeHeader = "bytes=${dataSpec.position}-$rangeEnd"
                    setRequestProperty("Range", rangeHeader)
                    Log.d(TAG, "   Range: $rangeHeader")
                }

                connectTimeout = 30000
                readTimeout = 30000
                doInput = true

                // ✅ Conectar DESPUÉS de configurar cabeceras
                connect()

                val responseCode = this.responseCode
                Log.d(TAG, "📥 Response Code: $responseCode")

                if (responseCode < 200 || responseCode > 299) {
                    val errorMessage = this.responseMessage ?: "Unknown error"
                    Log.e(TAG, "❌ HTTP $responseCode $errorMessage")

                    // Log cabeceras de respuesta para debugging
                    headerFields?.forEach { (key, value) ->
                        Log.e(TAG, "   $key: $value")
                    }

                    throw IOException("HTTP error: $responseCode - $errorMessage")
                }

                // ✅ FIX: Asignar inputStream desde la conexión
                this@CustomHttpDataSource.inputStream = this.inputStream

                val contentLength = getHeaderField("Content-Length")?.toLongOrNull() ?: C.LENGTH_UNSET.toLong()
                bytesToRead = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                    dataSpec.length
                } else {
                    contentLength
                }

                Log.d(TAG, "✅ Stream abierto - Content-Length: $contentLength")
            }

            opened = true
            bytesRead = 0
            return bytesToRead

        } catch (e: IOException) {
            Log.e(TAG, "❌ Error abriendo stream", e)
            close()
            throw e
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesToRead != C.LENGTH_UNSET.toLong() && bytesRead >= bytesToRead) {
            return C.RESULT_END_OF_INPUT
        }

        return try {
            val bytesToReadNow = if (bytesToRead != C.LENGTH_UNSET.toLong()) {
                minOf(length.toLong(), bytesToRead - bytesRead).toInt()
            } else {
                length
            }

            val read = inputStream?.read(buffer, offset, bytesToReadNow) ?: C.RESULT_END_OF_INPUT

            if (read > 0) {
                bytesRead += read
            }

            read
        } catch (e: IOException) {
            Log.e(TAG, "❌ Error leyendo stream", e)
            throw e
        }
    }

    override fun close() {
        try {
            inputStream?.close()
        } catch (e: IOException) {
            // Ignore
        } finally {
            inputStream = null
            connection?.disconnect()
            connection = null
            opened = false
            bytesRead = 0
            bytesToRead = C.LENGTH_UNSET.toLong()
            dataSpec = null
        }
    }

    override fun addTransferListener(transferListener: TransferListener) {
        // Not implemented
    }

    override fun getUri(): Uri? {
        return dataSpec?.uri
    }

    override fun getResponseHeaders(): Map<String, List<String>> {
        val headers = mutableMapOf<String, List<String>>()
        connection?.headerFields?.forEach { (key, value) ->
            if (key != null) {
                headers[key] = value
            }
        }
        return headers
    }

    override fun getResponseCode(): Int {
        return try {
            connection?.responseCode ?: 0
        } catch (e: IOException) {
            0
        }
    }

    override fun clearAllRequestProperties() {
        // Not implemented
    }

    override fun clearRequestProperty(name: String) {
        // Not implemented
    }

    override fun setRequestProperty(name: String, value: String) {
        // Not implemented
    }
}