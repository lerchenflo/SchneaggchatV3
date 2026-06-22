package io.github.lerchenflo.voicemessages.internal

import android.media.MediaMetadataRetriever

internal actual fun probeDurationMs(filePath: String): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(filePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (e: RuntimeException) {
        0L
    } finally {
        retriever.release()
    }
}
