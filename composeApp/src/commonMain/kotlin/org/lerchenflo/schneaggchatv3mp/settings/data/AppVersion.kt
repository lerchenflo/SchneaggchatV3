package org.lerchenflo.schneaggchatv3mp.settings.data

import kotlinx.serialization.Serializable

expect class AppVersion {
    fun getVersionName(): String
    fun getVersionCode(): String
    fun isMobile(): Boolean
    fun isDesktop(): Boolean
    fun isAndroid(): Boolean
    fun isIOS(): Boolean

    fun getDeviceType(): DEVICETYPE

    //Human-readable name of the device this app is running on, e.g. "Flo's iPhone" or "Pixel 8"
    fun getDeviceName(): String
}

@Serializable
enum class DEVICETYPE {
    ANDROID,
    IOS,
    DESKTOP;
}