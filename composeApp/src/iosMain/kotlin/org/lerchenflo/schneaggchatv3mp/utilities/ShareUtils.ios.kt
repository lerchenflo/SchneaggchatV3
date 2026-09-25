package org.lerchenflo.schneaggchatv3mp.utilities

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKitUI.EKEventEditViewAction
import platform.EventKitUI.EKEventEditViewController
import platform.EventKitUI.EKEventEditViewDelegateProtocol
import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dateWithTimeIntervalSince1970
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
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// EKEventEditViewController only keeps a weak reference to its delegate, so this delegate
// instance must be kept alive from outside for as long as the editor is on screen.
private class EventEditDelegate(
    private val onFinished: () -> Unit
) : NSObject(), EKEventEditViewDelegateProtocol {
    override fun eventEditViewController(
        controller: EKEventEditViewController,
        didCompleteWithAction: EKEventEditViewAction
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        onFinished()
    }
}

actual class ShareUtils {

    private var activeEventEditDelegate: EventEditDelegate? = null

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
    /**
     * RFC 3986 compliant percent-encoding for mailto URI parameter values.
     * Only unreserved characters (letters, digits, - . _ ~) are left unencoded.
     * Encodes the whole string to UTF-8 at once rather than char-by-char, since encoding
     * individual Chars breaks for surrogate pairs (each half isn't valid UTF-8 alone,
     * producing garbled %EF%BF%BD replacement-character sequences).
     */
    private fun percentEncode(value: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < value.length) {
            val char = value[i]
            if (char.isLetterOrDigit() && char.code < 128 || char in "-._~") {
                builder.append(char)
                i += 1
            } else {
                val charLen = if (char.isHighSurrogate() && i + 1 < value.length && value[i + 1].isLowSurrogate()) 2 else 1
                val bytes = value.substring(i, i + charLen).encodeToByteArray()
                for (byte in bytes) {
                    builder.append('%')
                    builder.append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
                }
                i += charLen
            }
        }
        return builder.toString()
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
    
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    actual fun openLocationInMaps(lat: Double, long: Double, label: String) {
        val query = if (label.isNotEmpty()) "$lat,$long(${percentEncode(label)})" else "$lat,$long"
        val googleMapsUrl = NSURL.URLWithString(
            URLString = "comgooglemaps://?q=$query&center=$lat,$long"
        )

        if (googleMapsUrl != null && UIApplication.sharedApplication.canOpenURL(googleMapsUrl)) {
            UIApplication.sharedApplication.openURL(googleMapsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
        } else {
            // Fallback to Google Maps web URL
            val webUrl = NSURL.URLWithString(
                URLString = "https://www.google.com/maps/search/?api=1&query=$query"
            )
            if (webUrl != null) {
                UIApplication.sharedApplication.openURL(webUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
            }
        }
    }

    actual fun openPhoneDialer(phoneNumber: String) {
        val url = NSURL.URLWithString(URLString = "tel:${percentEncode(phoneNumber)}")

        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        } else {
            // canOpenURL for "tel:" is false on devices with no telephony (iPad, simulator)
            val topVC = getTopViewController()
            val alert = UIAlertController.alertControllerWithTitle(
                title = "Cannot Make Calls",
                message = "This device is not able to make phone calls.",
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

    /**
     * Opens Apple's event editor pre-filled with the event. Deliberately no calendar permission
     * request: since iOS 17 the editor runs out of process and saves without any calendar access,
     * while the old requestAccessToEntityType asks for *full* access - which needs a usage
     * description this app doesn't declare, so it was denied and the editor never opened.
     */
    actual fun addEventToCalendar(title: String, description: String, location: String, startDateMillis: Long, endDateMillis: Long?) {
        dispatch_async(dispatch_get_main_queue()) {
            val eventStore = EKEventStore()
            val event = EKEvent.eventWithEventStore(eventStore)
            event.title = title
            event.notes = description.ifEmpty { null }
            event.location = location.ifEmpty { null }
            event.startDate = NSDate.dateWithTimeIntervalSince1970(startDateMillis / 1000.0)
            event.endDate = NSDate.dateWithTimeIntervalSince1970((endDateMillis ?: (startDateMillis + 3_600_000L)) / 1000.0)
            //No calendar set: without access there's none to read, the editor uses the default one

            val editViewController = EKEventEditViewController()
            editViewController.eventStore = eventStore
            editViewController.event = event

            val delegate = EventEditDelegate { activeEventEditDelegate = null }
            activeEventEditDelegate = delegate
            editViewController.editViewDelegate = delegate

            getTopViewController()?.presentViewController(editViewController, animated = true, completion = null)
        }
    }
}
