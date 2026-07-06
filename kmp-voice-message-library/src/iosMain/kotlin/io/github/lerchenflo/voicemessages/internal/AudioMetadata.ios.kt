package io.github.lerchenflo.voicemessages.internal

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
internal actual fun probeDurationMs(filePath: String): Long {
    val url = NSURL.fileURLWithPath(filePath)
    val asset = AVURLAsset(uRL = url, options = null)
    val seconds = CMTimeGetSeconds(asset.duration)
    return if (seconds.isFinite()) (seconds * 1_000).toLong() else 0L
}
