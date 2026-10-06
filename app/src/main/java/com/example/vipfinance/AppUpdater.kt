package com.example.vipfinance

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val API_URL = "https://api.github.com/repos/kulikartemtop-hash/vip-finance-android/releases/latest"
    private const val APK_NAME = "VIP-Finance.apk"
    private const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L

    fun checkAndOffer(activity: Activity) {
        val prefs = activity.getSharedPreferences("vip_updater", Activity.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong("last_check", 0L)
        if (now - last < CHECK_INTERVAL_MS) return
        prefs.edit().putLong("last_check", now).apply()
        Thread {
            try {
                val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000; readTimeout = 12000; requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "VIP-Finance-Android")
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@Thread
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONObject(json)
                val tag = release.optString("tag_name")
                val versionCode = release.optInt("version_code", -1)
                if (versionCode <= BuildConfig.VERSION_CODE) return@Thread
                val assets = release.optJSONArray("assets") ?: return@Thread
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name") == APK_NAME) { downloadUrl = asset.optString("browser_download_url"); break }
                }
                if (downloadUrl.isNullOrBlank()) return@Thread
                val finalUrl = downloadUrl!!
                Handler(Looper.getMainLooper()).post {
                    android.app.AlertDialog.Builder(activity)
                        .setTitle("Доступно обновление")
                        .setMessage("Новая версия ${tag.ifBlank { "VIP Finance" }} доступна. Обновить приложение сейчас?")
                        .setNegativeButton("Позже", null)
                        .setPositiveButton("Обновить") { _, _ -> downloadAndInstall(activity, finalUrl) }
                        .show()
                }
            } catch (_: Exception) { }
        }.start()
    }

    private fun downloadAndInstall(activity: Activity, url: String) {
        Thread {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000; readTimeout = 30000; requestMethod = "GET"; instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "VIP-Finance-Android")
                }
                if (connection.responseCode !in 200..299) return@Thread
                val apk = File(activity.cacheDir, APK_NAME)
                connection.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
                Handler(Looper.getMainLooper()).post { installApk(activity, apk) }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post { android.widget.Toast.makeText(activity, "Не удалось скачать обновление", android.widget.Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun installApk(activity: Activity, apk: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
            android.widget.Toast.makeText(activity, "Разрешите установку из этого источника и повторите обновление", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}