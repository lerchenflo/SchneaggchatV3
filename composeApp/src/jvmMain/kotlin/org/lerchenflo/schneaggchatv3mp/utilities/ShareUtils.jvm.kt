package org.lerchenflo.schneaggchatv3mp.utilities

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder

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
    
    actual fun openMailClient(recipient: String, subject: String, body: String) {
        try {
            val mailtoUri = "mailto:$recipient".let { uri ->
                val params = mutableListOf<String>()
                if (subject.isNotEmpty()) params.add("subject=${URLEncoder.encode(subject, "UTF-8").replace("+", "%20")}")
                if (body.isNotEmpty()) params.add("body=${URLEncoder.encode(body, "UTF-8").replace("+", "%20")}")
                if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
            }
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
                Desktop.getDesktop().mail(URI(mailtoUri))
            } else {
                // Fallback: try to open with default browser
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI(mailtoUri))
                } else {
                    println("No mail client or browser available to handle mailto URL")
                    println("Mailto link: $mailtoUri")
                }
            }
        } catch (e: Exception) {
            println("Failed to open mail client: ${e.message}")
            // As a last resort, print the mailto link for manual use
            val mailtoUri = "mailto:$recipient".let { uri ->
                val params = mutableListOf<String>()
                if (subject.isNotEmpty()) params.add("subject=$subject")
                if (body.isNotEmpty()) params.add("body=$body")
                if (params.isNotEmpty()) "$uri?${params.joinToString("&")}" else uri
            }
            println("Please use this link manually: $mailtoUri")
        }
    }
    
    actual fun copyToClipboard(text: String, clipboard: Any) {
        val selection = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        selection.setContents(java.awt.datatransfer.StringSelection(text), null)
    }

    actual fun openLocationInMaps(lat: Double, long: Double, label: String) {
        try {
            val url = "https://www.google.com/maps/search/?api=1&query=$lat,$long"
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                println("No browser available to open location: $url")
            }
        } catch (e: Exception) {
            println("Failed to open location in maps: ${e.message}")
        }
    }
}