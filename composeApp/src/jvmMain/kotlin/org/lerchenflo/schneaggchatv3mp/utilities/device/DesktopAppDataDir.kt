package org.lerchenflo.schneaggchatv3mp.utilities.device

import java.io.File

/**
 * The desktop app's data folder (database, settings, pictures), created when missing:
 * `%APPDATA%\Schneaggchat` on Windows, `~/Library/Application Support/Schneaggchat` on macOS and
 * `~/.local/share/Schneaggchat` elsewhere.
 */
fun desktopAppDataDir(): File {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val dir = when {
        os.contains("win") -> File(System.getenv("APPDATA"), "Schneaggchat")
        os.contains("mac") -> File(userHome, "Library/Application Support/Schneaggchat")
        else -> File(userHome, ".local/share/Schneaggchat")
    }
    if (!dir.exists()) dir.mkdirs()
    return dir
}
