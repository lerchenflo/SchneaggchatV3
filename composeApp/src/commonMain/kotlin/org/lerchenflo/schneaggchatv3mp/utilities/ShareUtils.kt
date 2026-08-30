package org.lerchenflo.schneaggchatv3mp.utilities

expect class ShareUtils {

    //Share a string (On android open popup and share in specific app, ios the same)
    fun shareString(string: String)
    
    //Open mail client with mailto link
    fun openMailClient(recipient: String, subject: String = "", body: String = "")

    //Copy text to clipboard
    fun copyToClipboard(text: String, clipboard: Any)

    //Open a lat/long location in the device's maps app
    fun openLocationInMaps(lat: Double, long: Double, label: String = "")

    //Open the phone dialer, pre-filled with the given number
    fun openPhoneDialer(phoneNumber: String)

    //Open the device's calendar app with a new event pre-filled, ready for the user to confirm
    fun addEventToCalendar(title: String, description: String = "", location: String = "", startDateMillis: Long, endDateMillis: Long?)

}