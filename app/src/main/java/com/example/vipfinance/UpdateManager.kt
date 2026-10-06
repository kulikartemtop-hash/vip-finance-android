package com.example.vipfinance

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val message: String,
    val isNewer: Boolean
)

object UpdateManager {
    private const val RELEASES_URL =
        "https://api.github.com/repos/kulikartemtop-hash/vip-finance-android/releases/latest"
    private const val APK_NAME = "VIP-Finance.apk"

    suspend fun checkLatest(): Result<UpdateInfo> = runCatching {
        val json = httpGet(RELEASES_URL)
        val root = JSONObject(json)
        val tag = root.optString("tag_name").removePrefix("v")
        require(tag.isNotBlank()) { "GitHub не вернул номер версии" }

        val assets = root.optJSONArray("assets")
            ?: throw IllegalStateException("В релизе нет APK")
        var downloadUrl: String? = null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name") == APK_NAME) {
                downloadUrl = asset.optString("browser_download_url")
                break
            }
        }
        require(!downloadUrl.isNullOrBlank()) { "APK новой версии не найден" }

        val newer = compareVersions(tag, BuildConfig.VERSION_NAME) > 0
        UpdateInfo(
            versionName = tag,
            downloadUrl = downloadUrl!!,
            message = if (newer) "Доступна новая версия $tag" else "Установлена последняя версия ${BuildConfig.VERSION_NAME}",
            isNewer = newer
        )
    }

    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apk = File(dir, APK_NAME)
                downloadFile(info.downloadUrl, apk)
                val uri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".fileprovider",
                    apk
                )
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(install)
            }
        }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "VIP-Finance/${BuildConfig.VERSION_NAME}")
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun downloadFile(url: String, target: File) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "VIP-Finance/${BuildConfig.VERSION_NAME}")
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output, 64 * 1024)
            }
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val av = a.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        val bv = b.split(".", "-", "_").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(av.size, bv.size)) {
            val x = av.getOrElse(i) { 0 }
            val y = bv.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
