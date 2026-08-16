package com.app.shouze.ui.components

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object CoverImageStore {

    private const val TAG = "Shouze"
    private const val MEM_CACHE_BYTES = 20 * 1024 * 1024
    private const val DISK_CACHE_BYTES = 50L * 1024 * 1024
    private const val MAX_DOWNLOAD_BYTES = 24L * 1024 * 1024
    private const val MAX_DECODE_DIMENSION = 1024
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_FAILED_URLS = 200
    private const val PREFS_NAME = "cover_image_status"
    private const val KEY_FAILED_URLS = "failed_urls"

    private val memoryCache = object : LruCache<String, Bitmap>(MEM_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private val failedUrls = LinkedHashMap<String, Unit>()

    @Volatile
    private var initialized = false
    private var diskDir: File? = null
    private var statusPrefs: SharedPreferences? = null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val dir = File(context.filesDir, "cover_cache")
            dir.mkdirs()
            diskDir = dir
            statusPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            statusPrefs?.getStringSet(KEY_FAILED_URLS, emptySet())?.forEach { failedUrls[it] = Unit }
            initialized = true
        }
    }

    fun peek(url: String): Bitmap? = memoryCache.get(url)

    fun isFailed(url: String): Boolean = synchronized(failedUrls) { failedUrls.containsKey(url) }

    fun forgetFailure(url: String) {
        synchronized(failedUrls) {
            if (failedUrls.remove(url) != null) persistFailed()
        }
    }

    suspend fun getOrLoad(url: String): Bitmap? {
        memoryCache.get(url)?.let { return it }
        if (isFailed(url)) return null

        val diskFile = diskFileFor(url)
        val fromDisk = withContext(Dispatchers.IO) {
            if (!diskFile.exists()) {
                null
            } else {
                runCatching { diskFile.readBytes() }.getOrNull()?.let { decodeSampled(it) }
            }
        }
        if (fromDisk != null) {
            memoryCache.put(url, fromDisk)
            return fromDisk
        }

        val bytes = fetchBytes(url)
        if (bytes == null) {
            rememberFailure(url)
            return null
        }
        val bitmap = withContext(Dispatchers.IO) { decodeSampled(bytes) }
        if (bitmap == null) {
            rememberFailure(url)
            return null
        }
        memoryCache.put(url, bitmap)
        withContext(Dispatchers.IO) {
            runCatching {
                diskFile.parentFile?.mkdirs()
                diskFile.writeBytes(bytes)
                evictDiskIfNeeded()
            }
        }
        return bitmap
    }

    private fun rememberFailure(url: String) {
        Log.w(TAG, "Cover image not loadable: $url")
        synchronized(failedUrls) {
            failedUrls[url] = Unit
            if (failedUrls.size > MAX_FAILED_URLS) {
                failedUrls.remove(failedUrls.keys.first())
            }
            persistFailed()
        }
    }

    private fun persistFailed() {
        runCatching {
            statusPrefs?.edit()?.putStringSet(KEY_FAILED_URLS, failedUrls.keys.toSet())?.apply()
        }
    }

    private fun diskFileFor(url: String): File {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return File(diskDir, hash)
    }

    private fun evictDiskIfNeeded() {
        val dir = diskDir ?: return
        val files = dir.listFiles() ?: return
        var size = files.sumOf { it.length() }
        if (size <= DISK_CACHE_BYTES) return
        for (file in files.sortedBy { it.lastModified() }) {
            if (size <= DISK_CACHE_BYTES) break
            size -= file.length()
            file.delete()
        }
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
}