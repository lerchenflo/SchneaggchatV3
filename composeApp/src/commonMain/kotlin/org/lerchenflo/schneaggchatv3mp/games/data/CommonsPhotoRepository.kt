package org.lerchenflo.schneaggchatv3mp.games.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.lerchenflo.schneaggchatv3mp.games.domain.CommonsPhoto
import org.lerchenflo.schneaggchatv3mp.games.domain.GuessMap
import org.lerchenflo.schneaggchatv3mp.games.domain.contains
import org.lerchenflo.schneaggchatv3mp.games.domain.randomPointInside
import org.lerchenflo.schneaggchatv3mp.schneaggmap.domain.LatLong
import org.lerchenflo.schneaggchatv3mp.utilities.distanceMeters

private const val COMMONS_API_URL = "https://commons.wikimedia.org/w/api.php"

/** Wikimedia asks every API client to identify itself with a contact address. */
private const val USER_AGENT = "GhettoGuesser/1.0 (schneaggchat@gmail.com)"

/** Results come back nearest first, so each attempt searches a small circle around a random point. */
private const val SEARCH_RADIUS_METERS = 1000
private const val SEARCH_LIMIT = 50
private const val DETAIL_BATCH_SIZE = 10
private const val MAX_SEARCH_ATTEMPTS = 8

/** Commons answers quick bursts with "search too busy", so attempts are spaced out. */
private const val RETRY_DELAY_MILLIS = 700L

/** Rendered width of the photo; Commons serves standard thumbnail sizes only. */
private const val THUMB_WIDTH = 1280
private const val MIN_SHORT_SIDE = 640
private const val MIN_ASPECT_RATIO = 0.4
private const val MAX_ASPECT_RATIO = 2.5

/** Keeps the rounds of one game apart, photo series often share one spot. */
private const val MIN_DISTANCE_TO_PREVIOUS_METERS = 100.0

private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png")

sealed interface PhotoLookup {
    data class Found(val photo: CommonsPhoto, val imageBytes: ByteArray) : PhotoLookup
    data object NoPhotos : PhotoLookup
    data object NetworkError : PhotoLookup
}

@Serializable
private data class GeoSearchResponse(
    val query: GeoSearchQuery? = null,
    val error: ApiError? = null,
)

@Serializable
private data class ApiError(val code: String = "")

@Serializable
private data class GeoSearchQuery(val geosearch: List<GeoSearchHit> = emptyList())

@Serializable
private data class GeoSearchHit(val pageid: Long, val lat: Double, val lon: Double)

@Serializable
private data class DetailResponse(val query: DetailQuery? = null)

@Serializable
private data class DetailQuery(val pages: List<DetailPage> = emptyList())

@Serializable
private data class DetailPage(
    val pageid: Long = 0,
    val title: String = "",
    val coordinates: List<DetailCoordinate> = emptyList(),
    val imageinfo: List<DetailImageInfo> = emptyList(),
)

@Serializable
private data class DetailCoordinate(val lat: Double, val lon: Double)

@Serializable
private data class DetailImageInfo(
    val width: Int = 0,
    val height: Int = 0,
    val mime: String = "",
    val thumburl: String? = null,
    val descriptionurl: String = "",
    val extmetadata: Map<String, MetadataValue> = emptyMap(),
)

@Serializable
private data class MetadataValue(val value: JsonElement? = null)

/**
 * Random geotagged photos from Wikimedia Commons (no API key). Each found photo comes with the
 * author and license data its license requires us to show.
 */
class CommonsPhotoRepository(
    private val httpClient: HttpClient,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * A random usable photo inside [map], skipping [excludedPageIds] and anything closer than
     * 100 m to [previousLocations].
     */
    suspend fun findRandomPhoto(
        map: GuessMap,
        excludedPageIds: Set<Long>,
        previousLocations: List<LatLong>,
    ): PhotoLookup {
        var reachedServer = false
        repeat(MAX_SEARCH_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(RETRY_DELAY_MILLIS)
            val center = map.randomPointInside()
            val hits = searchAround(center) ?: return@repeat
            reachedServer = true

            val candidates = hits
                .filter { it.pageid !in excludedPageIds }
                .map { it to LatLong(it.lat, it.lon) }
                .filter { (_, location) -> map.contains(location) && location.isFarFrom(previousLocations) }
                .distinctBy { (_, location) -> location.roundedKey() }
                .shuffled()
                .take(DETAIL_BATCH_SIZE)
                .map { (hit, _) -> hit.pageid }
            if (candidates.isEmpty()) return@repeat

            val photo = loadDetails(candidates)
                .shuffled()
                .firstOrNull { map.contains(it.location) && it.location.isFarFrom(previousLocations) }
                ?: return@repeat
            val bytes = downloadImage(photo.imageUrl) ?: return PhotoLookup.NetworkError
            return PhotoLookup.Found(photo, bytes)
        }
        return if (reachedServer) PhotoLookup.NoPhotos else PhotoLookup.NetworkError
    }

    private suspend fun searchAround(center: LatLong): List<GeoSearchHit>? {
        val body = getOrNull {
            httpClient.get(COMMONS_API_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("action", "query")
                parameter("format", "json")
                parameter("formatversion", "2")
                parameter("list", "geosearch")
                parameter("gscoord", "${center.lat.roundTo4()}|${center.long.roundTo4()}")
                parameter("gsradius", SEARCH_RADIUS_METERS)
                parameter("gsnamespace", 6)
                parameter("gslimit", SEARCH_LIMIT)
            }
        } ?: return null
        val response = runCatching { json.decodeFromString<GeoSearchResponse>(body) }.getOrNull()
        if (response == null || response.error != null) return null
        return response.query?.geosearch ?: emptyList()
    }

    private suspend fun loadDetails(pageIds: List<Long>): List<CommonsPhoto> {
        val body = getOrNull {
            httpClient.get(COMMONS_API_URL) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                parameter("action", "query")
                parameter("format", "json")
                parameter("formatversion", "2")
                parameter("pageids", pageIds.joinToString("|"))
                parameter("prop", "imageinfo|coordinates")
                parameter("colimit", "max")
                parameter("iiprop", "url|size|mime|extmetadata")
                parameter("iiurlwidth", THUMB_WIDTH)
                parameter(
                    "iiextmetadatafilter",
                    "Artist|LicenseShortName|LicenseUrl|ObjectName|ImageDescription"
                )
            }
        } ?: return emptyList()
        val pages = runCatching { json.decodeFromString<DetailResponse>(body) }
            .getOrNull()
            ?.query?.pages
            ?: return emptyList()
        return pages.mapNotNull { it.toPhotoOrNull() }
    }

    private suspend fun downloadImage(url: String): ByteArray? = try {
        val response = httpClient.get(url) { header(HttpHeaders.UserAgent, USER_AGENT) }
        if (response.status.isSuccess()) response.readRawBytes() else null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun getOrNull(request: suspend () -> HttpResponse): String? = try {
        val response = request()
        if (response.status.isSuccess()) response.bodyAsText() else null
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

private fun DetailPage.toPhotoOrNull(): CommonsPhoto? {
    val info = imageinfo.firstOrNull() ?: return null
    val coordinate = coordinates.firstOrNull() ?: return null
    val imageUrl = info.thumburl ?: return null
    if (info.mime !in ALLOWED_MIME_TYPES) return null
    if (minOf(info.width, info.height) < MIN_SHORT_SIDE) return null
    val aspectRatio = info.width.toDouble() / info.height
    if (aspectRatio !in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO) return null

    val licenseName = info.metadata("LicenseShortName") ?: return null
    if (!licenseName.isAllowedLicense()) return null

    return CommonsPhoto(
        pageId = pageid,
        imageUrl = imageUrl,
        location = LatLong(coordinate.lat, coordinate.lon),
        author = info.metadata("Artist"),
        licenseName = licenseName,
        licenseUrl = info.metadata("LicenseUrl"),
        filePageUrl = info.descriptionurl,
        title = info.metadata("ObjectName") ?: title.removePrefix("File:").substringBeforeLast('.'),
        description = info.metadata("ImageDescription")?.takeIf { it.isNotBlank() },
    )
}

/** Metadata values can contain HTML (links around the author name, formatted descriptions). */
private fun DetailImageInfo.metadata(key: String): String? =
    (extmetadata[key]?.value as? JsonPrimitive)?.content?.stripHtml()?.takeIf { it.isNotEmpty() }

/**
 * Only licenses whose terms we meet by showing author, license and file page.
 * Anything unusual (GFDL only, fair use, "attribution only" variants) is skipped.
 */
private fun String.isAllowedLicense(): Boolean {
    val name = trim()
    return name.startsWith("CC BY", ignoreCase = true) ||
            name.startsWith("CC0", ignoreCase = true) ||
            name.startsWith("Public domain", ignoreCase = true)
}

private fun String.stripHtml(): String =
    replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun LatLong.isFarFrom(others: List<LatLong>): Boolean =
    others.none { distanceMeters(this, it) < MIN_DISTANCE_TO_PREVIOUS_METERS }

private fun LatLong.roundedKey(): Pair<Long, Long> =
    Pair((lat * 10_000).toLong(), (long * 10_000).toLong())

/** Four decimals are about 10 m; shorter coordinates also let Commons cache the search. */
private fun Double.roundTo4(): Double = kotlin.math.round(this * 10_000) / 10_000
