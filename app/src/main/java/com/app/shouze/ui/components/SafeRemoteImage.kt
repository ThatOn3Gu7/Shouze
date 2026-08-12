package com.app.shouze.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_DOWNLOAD_BYTES = 24L * 1024 * 1024
private const val MAX_DECODE_DIMENSION = 1024
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

private val remoteImageCache = object : LruCache<String, Bitmap>(20 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private sealed interface RemoteImageState {
    data object Loading : RemoteImageState
    data class Loaded(val bitmap: Bitmap) : RemoteImageState
    data object Failed : RemoteImageState
}

@Composable
fun SafeRemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable () -> Unit = {},
    errorContent: @Composable () -> Unit = placeholder
) {
    if (url.isNullOrBlank()) {
        placeholder()
        return
    }
    val state by produceState<RemoteImageState>(initialValue = RemoteImageState.Loading, url) {
        value = loadBitmapCached(url)?.let { RemoteImageState.Loaded(it) }
            ?: RemoteImageState.Failed
    }
    when (val current = state) {
        is RemoteImageState.Loaded -> Image(
            bitmap = current.bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        RemoteImageState.Failed -> errorContent()
        RemoteImageState.Loading -> placeholder()
    }
}

private suspend fun loadBitmapCached(url: String): Bitmap? {
    remoteImageCache.get(url)?.let { return it }
    val bytes = fetchBytes(url) ?: return null
    val bitmap = decodeSampled(bytes) ?: return null
    remoteImageCache.put(url, bitmap)
    return bitmap
}

private suspend fun fetchBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(url).openConnection() as? HttpURLConnection ?: return@withContext null
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "Shouze/1.2")
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                null
            } else {
                connection.inputStream.use { input ->
                    ByteArrayOutputStream().use { output ->
                        input.copyTo(output)
                        if (output.size() > MAX_DOWNLOAD_BYTES) null else output.toByteArray()
                    }
                }
            }
        } finally {
            runCatching { connection.disconnect() }
        }
    } catch (t: Throwable) {
        null
    }
}

private fun decodeSampled(bytes: ByteArray): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= MAX_DECODE_DIMENSION ||
        bounds.outHeight / (sampleSize * 2) >= MAX_DECODE_DIMENSION
    ) {
        sampleSize *= 2
    }
    BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
} catch (t: Throwable) {
    null
}