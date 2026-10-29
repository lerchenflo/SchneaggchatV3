package org.lerchenflo.schneaggchatv3mp.fakes

import org.lerchenflo.schneaggchatv3mp.utilities.AppVersion
import org.lerchenflo.schneaggchatv3mp.utilities.DEVICETYPE

/** [AppVersion] of the given [device] (default Android). */
class FakeAppVersion(
    var device: DEVICETYPE = DEVICETYPE.ANDROID,
    var version: String = "1.0.0",
    var build: String = "1",
    var deviceLabel: String = "Test device",
) : AppVersion {
    override fun getVersionName(): String = version
    override fun getVersionCode(): String = build
    override fun isMobile(): Boolean = device == DEVICETYPE.ANDROID || device == DEVICETYPE.IOS
    override fun isDesktop(): Boolean = device == DEVICETYPE.DESKTOP
    override fun isAndroid(): Boolean = device == DEVICETYPE.ANDROID
    override fun isIOS(): Boolean = device == DEVICETYPE.IOS
    override fun getDeviceType(): DEVICETYPE = device
    override fun getDeviceName(): String = deviceLabel
}
