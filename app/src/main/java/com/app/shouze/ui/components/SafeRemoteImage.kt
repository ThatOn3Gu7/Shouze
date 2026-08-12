package com.app.shouze.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

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
    val context = LocalContext.current
    CoverImageStore.init(context.applicationContext)
    val initialState = when {
        CoverImageStore.isFailed(url) -> RemoteImageState.Failed
        else -> CoverImageStore.peek(url)?.let { RemoteImageState.Loaded(it) }
            ?: RemoteImageState.Loading
    }
    val state by produceState<RemoteImageState>(initialValue = initialState, url) {
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
