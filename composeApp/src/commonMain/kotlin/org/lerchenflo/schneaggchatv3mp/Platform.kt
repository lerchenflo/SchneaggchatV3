package org.lerchenflo.schneaggchatv3mp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform