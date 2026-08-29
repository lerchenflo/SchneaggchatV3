package org.lerchenflo.schneaggchatv3mp.utilities.compass

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class CompassService {

    /** Desktop has no orientation sensor - emit null so the UI falls back to north-up. */
    actual fun getAzimuthFlow(): Flow<Float?> = flowOf(null)
}
