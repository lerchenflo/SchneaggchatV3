package org.lerchenflo.schneaggchatv3mp.utilities

enum class AppIcon {
    DEFAULT, DARK
}

expect class AppIconManager {
    fun setIcon(icon: AppIcon)
    fun getCurrentIcon(): AppIcon
    fun isSupported(): Boolean
}
