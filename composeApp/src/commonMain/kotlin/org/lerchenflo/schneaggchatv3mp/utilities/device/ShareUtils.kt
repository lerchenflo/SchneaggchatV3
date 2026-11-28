package org.lerchenflo.schneaggchatv3mp.utilities.device

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import org.lerchenflo.schneaggchatv3mp.utilities.ui.SnackbarManager
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.copied_to_clipboard

interface ShareUtils {

    //Share a string (On android open popup and share in specific app, ios the same)
    fun shareString(string: String)
    
    //Open mail client with mailto link
    fun openMailClient(recipient: String, subject: String = "", body: String = "")

    //Copy text to clipboard
    fun copyToClipboard(text: String)

    //Open a lat/long location in the device's maps app
    fun openLocationInMaps(lat: Double, long: Double, label: String = "")

    //Open the phone dialer, pre-filled with the given number
    fun openPhoneDialer(phoneNumber: String)

    //Open the device's calendar app with a new event pre-filled, ready for the user to confirm
    fun addEventToCalendar(title: String, description: String = "", location: String = "", startDateMillis: Long, endDateMillis: Long?)

}

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
