package com.mitv.player.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "M3uParser"

@Singleton
class M3uParser @Inject constructor() {

    /**
     * Downloads and parses an M3U playlist from a URL.
     * Returns grouped categories with channels.
     */
    suspend fun parseFromUrl(url: String): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val content = fetchContent(url)
            val channels = parseM3uContent(content)
            val categories = groupByCategory(channels)
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse M3U from $url", e)
            Result.failure(e)
        }
    }

    /**
     * Parses M3U content from a raw string.
     */
    suspend fun parseFromContent(content: String): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val channels = parseM3uContent(content)
            val categories = groupByCategory(channels)
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse M3U content", e)
            Result.failure(e)
        }
    }

    private fun fetchContent(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "MiTV-Player/1.0")
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream))
                    .use { it.readText() }
            } else {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseM3uContent(content: String): List<Channel> {
        val lines = content.lines()
        if (lines.isEmpty() || !lines.first().trim().startsWith("#EXTM3U")) {
            throw Exception("Invalid M3U format: missing #EXTM3U header")
        }

        val channels = mutableListOf<Channel>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()

            if (line.startsWith("#EXTINF:")) {
                val channel = parseExtInf(line, lines.getOrNull(i + 1)?.trim() ?: "")
                if (channel != null && channel.url.isNotBlank()) {
                    channels.add(channel)
                }
                i += 2 // skip URL line
            } else {
                i++
            }
        }

        Log.d(TAG, "Parsed ${channels.size} channels")
        return channels
    }

    private fun parseExtInf(extinf: String, urlLine: String): Channel? {
        if (urlLine.isBlank() || urlLine.startsWith("#")) return null

        // Extract display name (everything after last comma)
        val name = extinf.substringAfterLast(",").trim()
            .ifBlank { "Unknown Channel" }

        // Extract attributes
        val tvgId = extractAttribute(extinf, "tvg-id")
        val tvgName = extractAttribute(extinf, "tvg-name")
        val tvgLogo = extractAttribute(extinf, "tvg-logo")
        val groupTitle = extractAttribute(extinf, "group-title")
            .ifBlank { "Uncategorized" }

        // Generate a unique ID
        val id = "${name}_${urlLine.hashCode()}".replace(" ", "_")

        return Channel(
            id = id,
            name = name,
            url = urlLine,
            logoUrl = tvgLogo,
            group = groupTitle,
            tvgId = tvgId,
            tvgName = tvgName
        )
    }

    private fun extractAttribute(line: String, attribute: String): String {
        val pattern = Regex("""$attribute="([^"]*?)"""", RegexOption.IGNORE_CASE)
        return pattern.find(line)?.groupValues?.get(1) ?: ""
    }

    private fun groupByCategory(channels: List<Channel>): List<Category> {
        return channels
            .groupBy { it.group }
            .map { (group, channelList) ->
                Category(name = group, channels = channelList)
            }
            .sortedBy { it.name }
    }
}
