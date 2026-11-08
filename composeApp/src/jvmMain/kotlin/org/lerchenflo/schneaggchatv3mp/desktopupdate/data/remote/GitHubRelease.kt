package org.lerchenflo.schneaggchatv3mp.desktopupdate.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String,
)

private val jsonParser = Json { ignoreUnknownKeys = true }

fun parseGitHubReleases(jsonString: String): List<GitHubRelease> {
    return try {
        // You can safely use the default uppercase Json here now
        jsonParser.decodeFromString<List<GitHubRelease>>(jsonString)
    } catch (e: Exception) {
        println("Parsing error: ${e.message}") // Print error so it doesn't fail silently
        emptyList()
    }
}