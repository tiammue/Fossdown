package com.fossdroid.update

import com.fossdroid.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val tagName: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val body: String?
)

sealed class UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class UpdateAvailable(val update: AppUpdate) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    private const val USER_AGENT = "Fossdroid/${BuildConfig.VERSION_NAME}"

    suspend fun checkForUpdate(
        currentVersion: String = BuildConfig.VERSION_NAME,
        owner: String = BuildConfig.GITHUB_OWNER,
        repo: String = BuildConfig.GITHUB_REPO
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val latest = fetchLatestRelease(owner, repo) ?: return@withContext UpdateCheckResult.Failed(
                "No releases found yet"
            )
            val remote = normalizeVersion(latest.versionName)
            val local = normalizeVersion(currentVersion)
            if (compareSemVer(remote, local) > 0) {
                UpdateCheckResult.UpdateAvailable(latest)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "Update check failed")
        }
    }

    private fun fetchLatestRelease(owner: String, repo: String): AppUpdate? {
        val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        try {
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code !in 200..299) {
                throw IllegalStateException("GitHub API returned HTTP $code")
            }
            val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            return parseRelease(JSONObject(body))
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseRelease(json: JSONObject): AppUpdate {
        val tag = json.optString("tag_name").orEmpty()
        val assets = json.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name").lowercase()
                val download = asset.optString("browser_download_url")
                if (name.endsWith(".apk") && download.isNotBlank()) {
                    apkUrl = download
                    break
                }
            }
        }
        return AppUpdate(
            versionName = normalizeVersion(tag),
            tagName = tag,
            htmlUrl = json.optString("html_url").ifBlank {
                BuildConfig.GITHUB_RELEASES_URL
            },
            apkDownloadUrl = apkUrl,
            body = json.optString("body").takeIf { it.isNotBlank() }
        )
    }

    /** Strip leading `v` and whitespace. */
    fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Compare dotted numeric versions. Returns >0 if [a] is newer than [b].
     * Non-numeric segments are treated as 0.
     */
    fun compareSemVer(a: String, b: String): Int {
        val left = normalizeVersion(a).split('.').map { it.toIntOrNull() ?: 0 }
        val right = normalizeVersion(b).split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(left.size, right.size)
        for (i in 0 until size) {
            val l = left.getOrElse(i) { 0 }
            val r = right.getOrElse(i) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }
}
