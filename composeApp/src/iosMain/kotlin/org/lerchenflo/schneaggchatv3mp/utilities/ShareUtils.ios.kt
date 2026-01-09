package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

actual class ShareUtils {
    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    actual fun shareString(string: String) {
        val activityItems = listOf(NSString.create(string = string))
        val activityViewController = UIActivityViewController(activityItems = activityItems, applicationActivities = null)
        
        val rootViewController =
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: UIApplication.sharedApplication.delegate?.window?.rootViewController

        val topVC = topViewController(rootViewController)

        activityViewController.popoverPresentationController?.apply {
            sourceView = topVC?.view ?: rootViewController?.view
            sourceRect = sourceView?.bounds ?: CGRectMake(0.0, 0.0, 1.0, 1.0)
        }

        (topVC ?: rootViewController)
            ?.presentViewController(activityViewController, animated = true, completion = null)

    }


    fun topViewController(root: UIViewController?): UIViewController? {
        var top = root
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }

}
