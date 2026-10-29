package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.utilities.ShareUtils

/** [ShareUtils] that opens nothing and records what would have been shared or copied. */
class FakeShareUtils : ShareUtils {

    val sharedStrings = CallLog<String>()
    val copiedTexts = CallLog<String>()

    override fun shareString(string: String) = sharedStrings.record(string)
    override fun openMailClient(recipient: String, subject: String, body: String) = Unit
    override fun copyToClipboard(text: String) = copiedTexts.record(text)
    override fun openLocationInMaps(lat: Double, long: Double, label: String) = Unit
    override fun openPhoneDialer(phoneNumber: String) = Unit
    override fun addEventToCalendar(
        title: String,
        description: String,
        location: String,
        startDateMillis: Long,
        endDateMillis: Long?,
    ) = Unit
}
