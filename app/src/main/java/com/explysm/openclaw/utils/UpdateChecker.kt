package com.explysm.openclaw.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.explysm.openclaw.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val html_url: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

object UpdateChecker {
    private const val GITHUB_REPO = "explysm/openclaw-termux-android"
    private const val RELEASES_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdates(context: Context, onUpdateAvailable: (version: String, url: String) -> Unit) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.e("UpdateChecker", "Failed to check for updates", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Logger.e("UpdateChecker", "GitHub API error: ${response.code}")
                        return
                    }

                    val body = response.body?.string() ?: return
                    try {
                        val release = json.decodeFromString<GitHubRelease>(body)
                        val latestVersion = release.tag_name.removePrefix("v")
                        val currentVersion = BuildConfig.VERSION_NAME

                        if (isNewerVersion(latestVersion, currentVersion)) {
                            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                            val downloadUrl = apkAsset?.browser_download_url ?: release.html_url
                            Logger.i("UpdateChecker", "New version available: $latestVersion")
                            onUpdateAvailable(latestVersion, downloadUrl)
                        } else {
                            Logger.i("UpdateChecker", "App is up to date: $currentVersion")
                        }
                    } catch (e: Exception) {
                        Logger.e("UpdateChecker", "Failed to parse release info", e)
                    }
                }
            }
        })
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(latestParts.size, currentParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }

    fun openDownloadUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
