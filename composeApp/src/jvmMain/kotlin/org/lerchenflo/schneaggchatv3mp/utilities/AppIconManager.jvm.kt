package org.lerchenflo.schneaggchatv3mp.utilities

actual class AppIconManager {
    actual fun setIcon(icon: AppIcon) {}
    actual fun getCurrentIcon(): AppIcon = AppIcon.DEFAULT
    actual fun isSupported(): Boolean = false
}
