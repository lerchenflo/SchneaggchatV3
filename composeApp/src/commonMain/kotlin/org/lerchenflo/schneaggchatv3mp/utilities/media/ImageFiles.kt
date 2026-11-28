package org.lerchenflo.schneaggchatv3mp.utilities.media

import io.ktor.http.decodeURLPart

/**
 * Image file formats the app stores and saves, read from the file's first bytes, never from a
 * file name: stored message pictures have no extension, and the picker library names files `.jpg`
 * whatever they hold.
 */
enum class ImageFileFormat(val extension: String, val mimeType: String, val uploadable: Boolean) {
    JPEG("jpg", "image/jpeg", uploadable = true),
    PNG("png", "image/png", uploadable = true),
    GIF("gif", "image/gif", uploadable = true),
    HEIC("heic", "image/heic", uploadable = false);

    companion object {
        /** Number of leading bytes [detect] needs. */
        const val HEADER_SIZE = 12

        /** The format of a file starting with [header] (magic numbers), null for anything else. */
        fun detect(header: ByteArray): ImageFileFormat? = when {
            header.startsWithBytes(0xFF, 0xD8, 0xFF) -> JPEG
            header.startsWithBytes(0x89, 0x50, 0x4E, 0x47) -> PNG
            header.startsWithBytes(0x47, 0x49, 0x46, 0x38) -> GIF
            header.isHeic() -> HEIC
            else -> null
        }
    }
}

/**
 * Whether the server takes these image bytes as they are: JPEG, PNG or GIF. Anything else (HEIC,
 * WebP, ...) has to be re-encoded before an upload.
 */
fun isUploadableImage(imageBytes: ByteArray): Boolean = ImageFileFormat.detect(imageBytes)?.uploadable == true

private fun ByteArray.startsWithBytes(vararg bytes: Int): Boolean =
    size >= bytes.size && bytes.indices.all { this[it] == bytes[it].toByte() }

private val HEIC_BRANDS = setOf("heic", "heix", "hevc", "hevx", "heim", "heis", "mif1", "msf1")

/** HEIC / HEIF: an ISO media file whose box at byte 4 is "ftyp" with a HEIF brand behind it. */
private fun ByteArray.isHeic(): Boolean =
    size >= ImageFileFormat.HEADER_SIZE &&
        decodeToString(4, 8) == "ftyp" &&
        decodeToString(8, 12) in HEIC_BRANDS

/**
 * A local `file://` uri, as the image picker reports its photos, as a plain file path with percent
 * escapes decoded. Anything that isn't a `file://` uri comes back unchanged.
 */
fun fileUriToPath(uri: String): String =
    if (uri.startsWith(FILE_URI_SCHEME)) uri.removePrefix(FILE_URI_SCHEME).decodeURLPart() else uri

private const val FILE_URI_SCHEME = "file://"
