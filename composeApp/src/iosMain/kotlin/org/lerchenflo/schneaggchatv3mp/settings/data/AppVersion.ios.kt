package org.lerchenflo.schneaggchatv3mp.settings.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname

actual class AppVersion {
    actual fun getVersionName(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "unknown"
    }

    actual fun getVersionCode(): String {
        return NSBundle.mainBundle
            .objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "unknown"
    }

    actual fun isMobile(): Boolean = true
    actual fun isDesktop(): Boolean = false
    actual fun isAndroid(): Boolean = false
    actual fun isIOS(): Boolean = true

    actual fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.IOS
    }

    /**
     * On iOS 16+, UIDevice.name no longer returns the user-assigned name
     * (e.g. "Tom's iPhone") unless the app holds Apple's restricted
     * com.apple.developer.device-information.user-assigned-device-name
     * entitlement. Without it, this returns a generic placeholder like "iPhone".
     * Everything else here (marketing model, system version, vendor id) is
     * freely available without any special entitlement.
     */
    actual fun getDeviceName(): String {
        val device = UIDevice.currentDevice
        val marketingName = getMarketingModelName()
        val systemVersion = device.systemVersion // e.g. "17.5"
        val vendorId = device.identifierForVendor?.UUIDString() ?: "unknown"

        // device.name is generic on iOS 16+, kept only as a last-resort label
        val displayName = device.name

        return "$marketingName (iOS $systemVersion) - $displayName - vendorId: $vendorId"
    }

    /**
     * Returns the raw hardware identifier, e.g. "iPhone17,3".
     * Available without any special permission.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun getHardwareIdentifier(): String {
        memScoped {
            val systemInfo = alloc<utsname>()
            uname(systemInfo.ptr)
            return systemInfo.machine.toKString()
        }
    }

    /**
     * Maps the raw hardware identifier to a human-readable marketing name.
     * Falls back to the raw identifier if the model isn't in the table yet
     * (e.g. a device released after this table was last updated).
     */
    private fun getMarketingModelName(): String {
        val identifier = getHardwareIdentifier()
        return marketingNameMap[identifier] ?: identifier
    }

    private val marketingNameMap: Map<String, String> = mapOf(
        // Simulator
        "i386" to "Simulator",
        "x86_64" to "Simulator",
        "arm64" to "Simulator",

        // iPhone SE
        "iPhone8,4" to "iPhone SE (1st generation)",
        "iPhone12,8" to "iPhone SE (2nd generation)",
        "iPhone14,6" to "iPhone SE (3rd generation)",

        // iPhone 8 / X
        "iPhone10,1" to "iPhone 8",
        "iPhone10,4" to "iPhone 8",
        "iPhone10,2" to "iPhone 8 Plus",
        "iPhone10,5" to "iPhone 8 Plus",
        "iPhone10,3" to "iPhone X",
        "iPhone10,6" to "iPhone X",

        // iPhone XR / XS
        "iPhone11,2" to "iPhone XS",
        "iPhone11,4" to "iPhone XS Max",
        "iPhone11,6" to "iPhone XS Max",
        "iPhone11,8" to "iPhone XR",

        // iPhone 11
        "iPhone12,1" to "iPhone 11",
        "iPhone12,3" to "iPhone 11 Pro",
        "iPhone12,5" to "iPhone 11 Pro Max",

        // iPhone 12
        "iPhone13,1" to "iPhone 12 mini",
        "iPhone13,2" to "iPhone 12",
        "iPhone13,3" to "iPhone 12 Pro",
        "iPhone13,4" to "iPhone 12 Pro Max",

        // iPhone 13
        "iPhone14,4" to "iPhone 13 mini",
        "iPhone14,5" to "iPhone 13",
        "iPhone14,2" to "iPhone 13 Pro",
        "iPhone14,3" to "iPhone 13 Pro Max",

        // iPhone 14
        "iPhone14,7" to "iPhone 14",
        "iPhone14,8" to "iPhone 14 Plus",
        "iPhone15,2" to "iPhone 14 Pro",
        "iPhone15,3" to "iPhone 14 Pro Max",

        // iPhone 15
        "iPhone15,4" to "iPhone 15",
        "iPhone15,5" to "iPhone 15 Plus",
        "iPhone16,1" to "iPhone 15 Pro",
        "iPhone16,2" to "iPhone 15 Pro Max",

        // iPhone 16
        "iPhone17,3" to "iPhone 16",
        "iPhone17,4" to "iPhone 16 Plus",
        "iPhone17,1" to "iPhone 16 Pro",
        "iPhone17,2" to "iPhone 16 Pro Max",
        "iPhone17,5" to "iPhone 16e",

        // iPad (add as needed)
        "iPad13,18" to "iPad (10th generation)",
        "iPad13,19" to "iPad (10th generation)",
        "iPad14,1" to "iPad mini (6th generation)",
        "iPad14,2" to "iPad mini (6th generation)",
    )
}