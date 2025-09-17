package org.lerchenflo.schneaggchatv3mp.settings.data

expect class AppVersion {
    fun getVersionName(): String
    fun getVersionCode(): String
    fun isMobile(): Boolean
    fun isDesktop(): Boolean
    fun isAndroid(): Boolean
    fun isIOS(): Boolean
}