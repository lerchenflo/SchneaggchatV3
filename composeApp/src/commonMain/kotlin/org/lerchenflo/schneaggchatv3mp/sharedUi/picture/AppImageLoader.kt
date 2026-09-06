package org.lerchenflo.schneaggchatv3mp.sharedUi.picture

import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers

/**
 * Decoding one profile picture costs 20-30ms, and the chat selector shows a screenful of them at
 * once. Bounding the decoder keeps that burst from occupying every core while the list is still
 * being measured and drawn - Coil's default would otherwise spread it over the whole shared pool.
 */
private const val MAX_PARALLEL_DECODES = 3

/**
 * Installs the app wide Coil [ImageLoader], which keeps all fetching and decoding on a bounded
 * background dispatcher instead of the main thread. Safe to call more than once - only the first
 * call wins - but it has to run before the first image is requested.
 */
fun setupImageLoader() {
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .decoderCoroutineContext(
                Dispatchers.Default.limitedParallelism(MAX_PARALLEL_DECODES, "CoilDecoder")
            )
            .fetcherCoroutineContext(
                Dispatchers.Default.limitedParallelism(MAX_PARALLEL_DECODES, "CoilFetcher")
            )
            .build()
    }
}
