package org.lerchenflo.schneaggchatv3mp.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String
)

private val jsonParser = Json { ignoreUnknownKeys = true }

fun getTagName(jsonString: String): String? {
    return try {
        // You can safely use the default uppercase Json here now
        jsonParser.decodeFromString<GitHubRelease>(jsonString).tagName
    } catch (e: Exception) {
        println("Parsing error: ${e.message}") // Print error so it doesn't fail silently
        null
    }
}