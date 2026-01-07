package org.lerchenflo.schneaggchatv3mp.utilities

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Clipboard

actual class ShareUtils {
    actual fun shareString(string: String) {
        try {
            val stringSelection = StringSelection(string)
            val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
            println("Text copied to clipboard: $string")
        } catch (e: Exception) {
            println("Failed to copy to clipboard: ${e.message}")
        }
    }
}