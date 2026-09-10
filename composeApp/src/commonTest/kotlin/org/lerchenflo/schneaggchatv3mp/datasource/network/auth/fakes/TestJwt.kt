package org.lerchenflo.schneaggchatv3mp.datasource.network.auth.fakes

/**
 * Mints unsigned three-part JWTs that `JWT.from` (jwt-kt) parses: a `{"alg":"HS256","typ":"JWT"}`
 * header, the claims given, and a fake signature. jwt-kt decodes base64url **without** padding,
 * so the encoder below never emits `=`.
 */
object TestJwt {
    const val DEFAULT_SUBJECT = "user-1"

    private var counter = 0

    fun token(
        sub: String = DEFAULT_SUBJECT,
        expEpochMillis: Long?,
        type: String = "access_token",
    ): String {
        counter++
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val claims = buildString {
            append("{\"sub\":\"").append(sub).append('"')
            if (expEpochMillis != null) append(",\"exp\":").append(expEpochMillis / 1000)
            append(",\"type\":\"").append(type).append('"')
            append(",\"jti\":\"t").append(counter).append("\"}")
        }
        return base64Url(header) + "." + base64Url(claims) + "." + base64Url("sig")
    }

    /** Something that is not a JWT at all (still three dot-separated parts, so the failure is in decoding, not splitting). */
    fun malformed(): String = "not.a.jwt"

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    private fun base64Url(input: String): String {
        val bytes = input.encodeToByteArray()
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else -1
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else -1
            sb.append(ALPHABET[b0 shr 2])
            sb.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            if (b1 >= 0) sb.append(ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)])
            if (b2 >= 0) sb.append(ALPHABET[b2 and 0x3F])
            i += 3
        }
        return sb.toString()
    }
}
