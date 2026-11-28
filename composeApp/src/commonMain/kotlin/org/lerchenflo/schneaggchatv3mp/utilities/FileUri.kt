package org.lerchenflo.schneaggchatv3mp.utilities

import io.ktor.http.decodeURLPart

/**
 * A local `file://` uri, as the image picker reports its photos, as a plain file path with percent
 * escapes decoded. Anything that isn't a `file://` uri comes back unchanged.
 */
fun fileUriToPath(uri: String): String =
    if (uri.startsWith(FILE_URI_SCHEME)) uri.removePrefix(FILE_URI_SCHEME).decodeURLPart() else uri

private const val FILE_URI_SCHEME = "file://"
