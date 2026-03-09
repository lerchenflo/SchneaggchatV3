package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

actual class ShareUtils {

    /**
     * Gets the root view controller using the modern connectedScenes API (iOS 13+),
     * with a fallback to the app delegate's window for older setups.
     */
    private fun getRootViewController(): UIViewController? {
        val windowScene = UIApplication.sharedApplication.connectedScenes
            .firstOrNull { it is UIWindowScene } as? UIWindowScene
        return windowScene?.windows
            ?.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
            ?.let { (it as UIWindow).rootViewController }
            ?: UIApplication.sharedApplication.delegate?.window?.rootViewController
    }

    private fun getTopViewController(root: UIViewController? = getRootViewController()): UIViewController? {
        var top = root
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }

    /**
     * RFC 3986 compliant percent-encoding for mailto URI parameter values.
     * Only unreserved characters (letters, digits, - . _ ~) are left unencoded.
     */
    private fun percentEncode(value: String): String {
        return buildString {
            for (char in value) {
                when {
                    char.isLetterOrDigit() || char in "-._~" -> append(char)
                    else -> {
                        val bytes = char.toString().encodeToByteArray()
                        for (byte in bytes) {
                            append('%')
                            append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
                        }
                    }
                }
            }
        }
    }

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    actual fun shareString(string: String) {
        val activityItems = listOf(NSString.create(string = string))
        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null
        )

        val rootViewController = getRootViewController()
        val topVC = getTopViewController(rootViewController)

        // Configure popover for iPad — anchor to center of the view
        // instead of full bounds to avoid awkward full-screen anchoring
        activityViewController.popoverPresentationController?.apply {
            sourceView = topVC?.view ?: rootViewController?.view
            val centerX = sourceView?.bounds?.useContents { size.width }?.div(2.0) ?: 0.0
            val centerY = sourceView?.bounds?.useContents { size.height }?.div(2.0) ?: 0.0
            sourceRect = CGRectMake(centerX, centerY, 1.0, 1.0)
        }

        (topVC ?: rootViewController)
            ?.presentViewController(activityViewController, animated = true, completion = null)
    }

    actual fun openMailClient(recipient: String, subject: String, body: String) {
        val mailtoUri = "mailto:$recipient".let { uri ->
            val params = mutableListOf<String>()
            if (subject.isNotEmpty()) params.add("subject=${percentEncode(subject)}")
            if (body.isNotEmpty()) params.add("body=${percentEncode(body)}")
            if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
        }

        val url = NSURL.URLWithString(URLString = mailtoUri)

        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(
                url,
                options = emptyMap<Any?, Any>(),
                completionHandler = null
            )
        } else {
            // Show user-facing alert instead of silent println
            val topVC = getTopViewController()
            val alert = UIAlertController.alertControllerWithTitle(
                title = "No Mail Client",
                message = "No mail client is available on this device. Please configure a mail account in Settings.",
                preferredStyle = UIAlertControllerStyleAlert
            )
            alert.addAction(
                UIAlertAction.actionWithTitle(
                    "OK",
                    style = UIAlertActionStyleDefault,
                    handler = null
                )
            )
            topVC?.presentViewController(alert, animated = true, completion = null)
        }
    }
    
    actual fun copyToClipboard(text: String, clipboard: Any) {
        val pasteboard = UIPasteboard.generalPasteboard
        pasteboard.string = text
    }
}
