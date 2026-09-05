package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard

/**
 * Copies [text] to the system clipboard and shows a confirmation snackbar. Used from message
 * long-press "copy" (chat) as well as the map's location/user info cards.
 */
fun copyToClipboard(text: String) {
    val shareUtils = KoinPlatform.getKoin().get<ShareUtils>()
    shareUtils.copyToClipboard(text)

    runBlocking {
        val copied = getString(Res.string.copied_to_clipboard) + ": "
        SnackbarManager.showMessage(copied + text)
    }

}
