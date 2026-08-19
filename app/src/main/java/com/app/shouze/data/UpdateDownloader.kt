package com.app.shouze.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object UpdateDownloader {
    private const val PREFS = "update_download"
    private const val KEY_ID = "download_id"
    private const val APK_NAME = "shouze-update.apk"
    private const val MIN_APK_BYTES = 1024L * 1024L

    fun enqueue(context: Context, url: String): Boolean = try {
        File(context.getExternalFilesDir(null), APK_NAME)?.delete()
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Shouze update")
            setDescription("Downloading new version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, null, APK_NAME)
            setMimeType("application/vnd.android.package-archive")
        }
        val id = dm.enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_ID, id).apply()
        true
    } catch (_: Exception) {
        false
    }

    fun install(context: Context): Boolean {
        val file = File(context.getExternalFilesDir(null), APK_NAME)
        if (!file.exists() || file.length() < MIN_APK_BYTES) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
fun findApkFiles(context: Context): List<File> {
        val dir = context.getExternalFilesDir(null) ?: return emptyList()
        return dir.listFiles { f -> f.isFile && f.extension.equals("apk", ignoreCase = true) }
            ?.toList() ?: emptyList()
    }

    fun deleteApkFiles(files: List<File>): Int = files.count { it.delete() }
}

class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val prefs = context.getSharedPreferences("update_download", Context.MODE_PRIVATE)
        if (id != prefs.getLong("download_id", -1L)) return
        val appContext = context.applicationContext
        val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var successful = false
        dm.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                successful = status == DownloadManager.STATUS_SUCCESSFUL
            }
        }
        if (successful) {
            if (UpdateDownloader.install(appContext)) {
                Toast.makeText(appContext, "Installing update...", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(appContext, "Download incomplete, please try again", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(appContext, "Update download failed", Toast.LENGTH_LONG).show()
        }
    }
}