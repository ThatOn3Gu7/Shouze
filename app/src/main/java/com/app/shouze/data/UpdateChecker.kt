package com.app.shouze.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val GITHUB_REPO = "ThatOn3Gu7/Shouze"

data class GitHubRelease(
    val tag: String,
    val htmlUrl: String,
    val body: String,
    val apkUrl: String?
)

suspend fun fetchLatestRelease(repo: String = GITHUB_REPO): GitHubRelease? =
    withContext(Dispatchers.IO) {
        try {
            val conn = (URL("https://api.github.com/repos/$repo/releases/latest")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(text)
            val tag = json.optString("tag_name")
            val html = json.optString("html_url")
            val body = json.optString("body")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (tag.isBlank()) null else GitHubRelease(tag, html, body, apkUrl)
        } catch (_: Exception) {
            null
        }
    }

fun isNewerVersion(latest: String, current: String): Boolean {
    fun parse(v: String): List<Int> =
        v.trim().trimStart('v', 'V').split('.').map {
            it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
        }
    val a = parse(latest)
    val b = parse(current)
    val n = if (a.size >= b.size) a.size else b.size
    for (i in 0 until n) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

fun currentVersionName(context: Context): String =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0"