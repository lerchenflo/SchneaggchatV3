package org.lerchenflo.schneaggchatv3mp.utilities

expect class ShareUtils {

    //Share a string (On android open popup and share in specific app, ios the same)
    fun shareString(string: String)

}