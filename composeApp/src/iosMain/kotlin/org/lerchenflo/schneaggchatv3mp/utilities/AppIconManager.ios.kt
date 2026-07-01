package org.lerchenflo.schneaggchatv3mp.utilities

import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication

actual class AppIconManager {
    actual fun setIcon(icon: AppIcon) {
        val name = when (icon) {
            AppIcon.DEFAULT -> null
            AppIcon.DARK -> "AppIconDark"
        }
        NSOperationQueue.mainQueue.addOperationWithBlock {
            UIApplication.sharedApplication.setAlternateIconName(name, null)
        }
    }

    actual fun getCurrentIcon(): AppIcon =
        if (UIApplication.sharedApplication.alternateIconName == "AppIconDark") AppIcon.DARK
        else AppIcon.DEFAULT

    actual fun isSupported(): Boolean = true
}
