package org.lerchenflo.schneaggchatv3mp.utilities

import java.io.File
import java.net.InetAddress
import java.util.UUID

class DesktopAppVersion : AppVersion {
    override fun getVersionName(): String {
        return this::class.java.`package`.implementationVersion ?: "Desktop"
    }

    override fun getVersionCode(): String {
        return "1"
    }

    override fun isMobile(): Boolean {
        return false
    }

    override fun isDesktop(): Boolean {
        return true
    }

    override fun isAndroid(): Boolean {
        return false
    }

    override fun isIOS(): Boolean {
        return false
    }

    override fun getDeviceType(): DEVICETYPE {
        return DEVICETYPE.DESKTOP
    }

    override fun getDeviceName(): String {
        val hostname = try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "Desktop"
        }

        return "$hostname - installId: $installId"
    }

    private companion object {
        /**
         * Random id of this installation, created on first use and kept in the app data folder -
         * the desktop analogue of iOS identifierForVendor / Android ANDROID_ID. Replaces the MAC
         * address, which changed with the active network interface (Wi-Fi/Ethernet, VPN, Docker),
         * so the device got a new name and was no longer recognized as the same session.
         * A separate file, not DataStore: Preferencemanager.clearAll() on logout must not reset it.
         */
        val installId: String by lazy {
            try {
                val os = System.getProperty("os.name").lowercase()
                val userHome = System.getProperty("user.home")
                val appDataDir = when {
                    os.contains("win") -> File(System.getenv("APPDATA"), "Schneaggchat")
                    os.contains("mac") -> File(userHome, "Library/Application Support/Schneaggchat")
                    else -> File(userHome, ".local/share/Schneaggchat")
                }
                appDataDir.mkdirs()

                val idFile = File(appDataDir, "install_id")
                idFile.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString().also { idFile.writeText(it) }
            } catch (e: Exception) {
                // Unwritable data folder: still a usable name for this run
                "unknown"
            }
        }
    }
}
