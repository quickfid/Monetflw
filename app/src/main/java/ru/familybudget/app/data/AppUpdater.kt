package ru.familybudget.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String
)

object AppUpdater {
    private const val RELEASES_API = "https://api.github.com/repos/quickfid/Monetflw/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun currentVersion(context: Context): String {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.4.0"
        } catch (_: Exception) {
            "1.4.0"
        }
    }

    fun checkLatest(context: Context): UpdateInfo? {
        val req = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "FamilyBudget-Android")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val tag = json.optString("tag_name").trim().removePrefix("v").removePrefix("V")
            if (tag.isBlank()) return null

            val installed = currentVersion(context).trim().removePrefix("v").removePrefix("V")
            if (!isNewer(tag, installed)) return null

            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl = ""
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
            if (apkUrl.isBlank()) return null

            val title = json.optString("name").ifBlank { "Версия $tag" }
            val notes = json.optString("body").trim().take(400)
            return UpdateInfo(version = tag, title = title, notes = notes, apkUrl = apkUrl)
        }
    }

    fun downloadAndInstall(context: Context, apkUrl: String, onProgress: (Int) -> Unit) {
        val req = Request.Builder()
            .url(apkUrl)
            .header("User-Agent", "FamilyBudget-Android")
            .build()

        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val partFile = File(dir, "update.apk.part")
        val outFile = File(dir, "update.apk")

        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Ошибка загрузки: HTTP ${resp.code}")
                val body = resp.body ?: error("Пустой ответ сервера")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buf = ByteArray(16 * 1024)
                        var readTotal = 0L
                        var lastPct = -1
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            readTotal += n
                            if (total > 0) {
                                val pct = ((readTotal * 100) / total).toInt().coerceIn(0, 100)
                                if (pct != lastPct) {
                                    lastPct = pct
                                    onProgress(pct)
                                }
                            }
                        }
                        onProgress(100)
                    }
                }
            }
            if (outFile.exists()) outFile.delete()
            if (!partFile.renameTo(outFile)) error("Не удалось сохранить файл обновления")
        } catch (e: Exception) {
            partFile.delete()
            throw e
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    internal fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val l = local.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val max = maxOf(r.size, l.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
