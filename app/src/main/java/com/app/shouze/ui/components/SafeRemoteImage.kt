package com.app.shouze.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface RemoteImageState {
    data object Loading : RemoteImageState
    data class Loaded(val bitmap: Bitmap) : RemoteImageState
    data object Failed : RemoteImageState
}

private const val MAX_CONTENT_DECODE_DIMENSION = 1024
private const val CONTENT_CACHE_BYTES = 12 * 1024 * 1024

private val contentUriCache = object : LruCache<String, Bitmap>(CONTENT_CACHE_BYTES) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

private fun decodeSampled(bytes: ByteArray): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= MAX_CONTENT_DECODE_DIMENSION ||
        bounds.outHeight / (sampleSize * 2) >= MAX_CONTENT_DECODE_DIMENSION
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

    // Gallery picker returns content:// URIs — load them via ContentResolver
    if (url.startsWith("content://")) {
        ContentUriImage(
            uriString = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = placeholder,
            errorContent = errorContent
        )
        return
    }

    val context = LocalContext.current
    CoverImageStore.init(context.applicationContext)
    val initialState = when {
        CoverImageStore.isFailed(url) -> RemoteImageState.Failed
        else -> CoverImageStore.peek(url)?.let { RemoteImageState.Loaded(it) }
            ?: RemoteImageState.Loading
    }
    val state by produceState(initialValue = initialState, url) {
        value = CoverImageStore.getOrLoad(url)?.let { RemoteImageState.Loaded(it) }
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

@Composable
private fun ContentUriImage(
    uriString: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    placeholder: @Composable () -> Unit = {},
    errorContent: @Composable () -> Unit = placeholder
) {
    val context = LocalContext.current
    val initialState = contentUriCache.get(uriString)?.let { RemoteImageState.Loaded(it) }
        ?: RemoteImageState.Loading
    val state by produceState<RemoteImageState>(initialValue = initialState, uriString) {
        value = withContext(Dispatchers.IO) {
            contentUriCache.get(uriString)?.let { return@withContext RemoteImageState.Loaded(it) }
            try {
                val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
                val bitmap = bytes?.let { decodeSampled(it) }
                if (bitmap != null) {
                    contentUriCache.put(uriString, bitmap)
                    RemoteImageState.Loaded(bitmap)
                } else {
                    RemoteImageState.Failed
                }
            } catch (e: Exception) {
                RemoteImageState.Failed
            }
        }
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
