package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class ShareUtils {
    @OptIn(BetaInteropApi::class)
    actual fun shareString(string: String) {
        val activityItems = listOf(
            NSString.create(string = string)
        )
        val activityViewController = UIActivityViewController(activityItems = activityItems, applicationActivities = null)

        // Get the top-most view controller to present the activity view controller
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)

    }
}