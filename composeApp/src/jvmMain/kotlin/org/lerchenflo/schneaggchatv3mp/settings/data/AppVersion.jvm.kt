package org.lerchenflo.schneaggchatv3mp.settings.data

import java.net.InetAddress
import java.net.NetworkInterface

actual class AppVersion {
    actual fun getVersionName(): String {
        return this::class.java.`package`.implementationVersion ?: "Desktop"
    }

    actual fun getVersionCode(): String {
        return "1"
    }

    actual fun isMobile(): Boolean {
        return false
    }

    actual fun isDesktop(): Boolean {
        return true
    }

    actual fun isAndroid(): Boolean {
        return false
    }

    actual fun isIOS(): Boolean {
        return false
    }

    actual fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.DESKTOP
    }

    actual fun getDeviceName(): String {
        val hostname = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Desktop"
        }

        return "$hostname - macAddress: ${getMacAddress()}"
    }

    private fun getMacAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filterNot { it.isLoopback || it.isVirtual }
                .mapNotNull { it.hardwareAddress }
                .firstOrNull { it.isNotEmpty() }
                ?.joinToString(":") { "%02X".format(it) }
                ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}